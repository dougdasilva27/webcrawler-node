package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 19/10/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilAmazonCrawler extends Crawler {

	private static final String HOME_PAGE = "https://www.amazon.com.br/";
	private static final String SELLER_NAME_LOWER = "amazon.com.br";

	public BrasilAmazonCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<>();

		if (isProductPage(doc)) {
			Logging.printLogDebug(logger, session,
					"Product page identified: " + this.session.getOriginalURL());

			String internalId = crawlInternalId(doc);
			String internalPid = internalId;
			String name = crawlName(doc);
			Float price = crawlPrice(doc);
			Prices prices = crawlPrices(price, doc);
			boolean available = crawlAvailability(doc);
			CategoryCollection categories = crawlCategories(doc);
			String primaryImage = crawlPrimaryImage(doc);
			String secondaryImages = crawlSecondaryImages(doc);
			String description = crawlDescription(doc);
			Integer stock = null;
			Document docMarketPlace = fetchDocumentMarketPlace(internalId);
			Marketplace marketplace = crawlMarketplace();

			// Creating the product
			Product product = ProductBuilder.create().setUrl(session.getOriginalURL())
					.setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price)
					.setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0))
					.setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2))
					.setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages)
					.setDescription(description).setStock(stock).setMarketplace(marketplace).build();

			products.add(product);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return products;

	}

	private boolean isProductPage(Document doc) {
		return doc.select("#dp").first() != null;
	}

	private String crawlInternalId(Document doc) {
		String internalId = null;

		Element internalIdElement = doc.select("input[name^=ASIN]").first();

		if (internalIdElement != null) {
			internalId = internalIdElement.val();
		}

		return internalId;
	}


	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("#centerCol h1#title").first();

		if (nameElement != null) {
			name = nameElement.text().trim();
		}


		return name;
	}

	private Float crawlPrice(Document document) {
		Float price = null;
		Element salePriceElement =
				document.select(".a-box .a-section.a-spacing-none.a-padding-none .a-color-price").first();

		if (salePriceElement != null) {
			price = MathCommonsMethods.parseFloat(salePriceElement.text().trim());
		}

		return price;
	}

	/**
	 * Fetch page when have marketplace info
	 * 
	 * @param id
	 * @return document
	 */
	private Document fetchDocumentMarketPlace(String id) {
		Document doc = new Document("");

		if (id != null) {
			String urlMarketPlace =
					"https://www.amazon.com.br/gp/offer-listing/" + id + "/ref=dp_olp_new";
			doc = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, urlMarketPlace, null, null);
		}

		return doc;
	}

	private Map<String, Prices> crawlMarketplaces(Document docMarketplaceInfo, Document doc) {
		Map<String, Prices> marketplace = new HashMap<>();

		String principalSeller = SELLER_NAME_LOWER;

		Elements lines = docMarketplaceInfo.select(".a-row.olpOffer");

		for (Element linePartner : lines) {
			String partnerName =
					linePartner.select(".a-color-price.olpSellerName").first().text().trim().toLowerCase();
			Float partnerPrice = Float.parseFloat(linePartner.select(".olpOfferPrice").first().text()
					.replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

			Element comprar = linePartner.select(".adicionarCarrinho > a.bt-comprar-disabled").first();

			Prices prices = new Prices();

			if (!principalSeller.equals(partnerName)) {
				prices.setBankTicketPrice(partnerPrice);

				Map<Integer, Float> installmentPriceMap = new HashMap<>();
				installmentPriceMap.put(1, partnerPrice);

				Elements installments = doc.select(".a-horizontal-stripes .a-text-lef");

				if (installments.size() > 1) {
					Integer installment = Integer.parseInt(installments.get(0).text().trim());
					Float value = Float.parseFloat(installments.get(1).text().replaceAll("[^0-9,]+", "")
							.replaceAll("\\.", "").replaceAll(",", "."));

					installmentPriceMap.put(installment, value);

				}

				prices.insertCardInstallment("visa", installmentPriceMap);
			} else {
				prices = crawlPrices(partnerPrice, doc);
			}


			if (comprar == null)
				marketplace.put(partnerName, prices);
		}

		return marketplace;
	}

	private Marketplace crawlMarketplace() {
		return new Marketplace();
	}

	private String crawlPrimaryImage(Document doc) {
		String primaryImage = null;
		Element elementPrimaryImage = doc.select(".zoom-img-index > img").first();

		if (elementPrimaryImage != null) {
			primaryImage = elementPrimaryImage.attr("data-zoom-image").trim();

			if (primaryImage.isEmpty()) {
				primaryImage = elementPrimaryImage.attr("src").trim();
			}

			if (!primaryImage.startsWith(HOME_PAGE)) {
				primaryImage = HOME_PAGE + primaryImage;
			}
		}

		return primaryImage;
	}

	/**
	 * Quando este crawler foi feito, nao tinha imagens secundarias
	 * 
	 * @param doc
	 * @return
	 */
	private String crawlSecondaryImages(Document doc) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements images = doc.select("#gallery li > a");

		for (int i = 1; i < images.size(); i++) {
			Element e = images.get(i);

			String image = e.attr("data-zoom-image").trim();

			if (image.isEmpty()) {
				image = e.attr("data-image").trim();
			}

			if (!image.startsWith(HOME_PAGE)) {
				image = HOME_PAGE + image;
			}

			secondaryImagesArray.put(image);
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	/**
	 * @param document
	 * @return
	 */
	private CategoryCollection crawlCategories(Document document) {
		CategoryCollection categories = new CategoryCollection();
		Elements elementCategories =
				document.select(".breadcrumbs li.show-for-large-up:not(.current) a");

		for (Element e : elementCategories) {
			String cat = e.ownText().trim();

			if (!cat.isEmpty()) {
				categories.add(cat);
			}
		}

		return categories;
	}

	private String crawlDescription(Document doc) {
		StringBuilder description = new StringBuilder();

		Element elementDescription = doc.select("#corpo-detalhe .prod-descr .descricao").first();

		if (elementDescription != null) {
			description.append(elementDescription.html());
		}

		return description.toString();
	}

	private boolean crawlAvailability(Document doc) {
		return doc.select(".button-comprar").first() != null;
	}

	/**
	 * 
	 * @param doc
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(Float price, Document doc) {
		Prices prices = new Prices();

		if (price != null) {
			Map<Integer, Float> installmentPriceMap = new TreeMap<>();
			installmentPriceMap.put(1, price);
			prices.setBankTicketPrice(price);

			Elements installmentsElement = doc.select("#side-prod .preco-parcela strong");

			if (installmentsElement.size() > 1) {
				String textInstallment = installmentsElement.get(0).ownText().replaceAll("[^0-9]", "");
				Float value = MathCommonsMethods.parseFloat(installmentsElement.get(1).ownText());

				if (!textInstallment.isEmpty() && value != null) {
					installmentPriceMap.put(Integer.parseInt(textInstallment), value);
				}
			}

			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
		}

		return prices;
	}

}
