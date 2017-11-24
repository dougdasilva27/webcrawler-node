package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

/************************************************************************************************************************************************************************************
 * Crawling notes (20/09/2016):
 * 
 * 1) One URL per sku. Despite the fact that the user can select a variation of a sku in a page,
 * like this case:
 * https://www.carrefour.com.br/Cafeteira-Eletrica-Mondial-Preta-Portateis-C-17-110V/p/9455310 The
 * crawler must only get the product that is default on that url. The other variation will be
 * discovered by the ranking crawler.
 * 
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply by looking the substring "/p/" in the URL.
 * 
 * 5) Even if a product is unavailable, its price is not displayed.
 * 
 * 6) Internal pid is always null.
 * 
 * Examples: ex2 (unavailable):
 * https://www.carrefour.com.br/Notebook-Samsung-Intel-Core-i7-8GB-1TB-Placa-de-Video-2GB-Windows-10-Tela-15-6-Expert-X40-Branco/p/9831541
 *
 * Optimizations notes: no optimization
 * 
 * @author Samir Leao
 *
 ***********************************************************************************************************************************************************************************/
public class BrasilCarrefourCrawler extends Crawler {

	private final String HOME_PAGE = "https://www.carrefour.com.br/";

	public BrasilCarrefourCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<>();

		if (isProductPage(this.session.getOriginalURL())) {
			Logging.printLogDebug(logger, session,
					"Product page identified: " + this.session.getOriginalURL());

			/*
			 * *********************************** crawling data of only one product *
			 *************************************/

			// InternalId
			String internalId = crawlInternalId(doc);

			// Pid
			String internalPid = crawlInternalPid(session.getOriginalURL());

			// Name
			String name = crawlName(doc);

			// Price
			Float price = crawlMainPagePrice(doc);

			// Availability
			boolean available = crawlAvailability(doc);

			// Categories
			ArrayList<String> categories = crawlCategories(doc);
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);

			// Primary image
			String primaryImage = crawlPrimaryImage(doc);

			// Secondary images
			String secondaryImages = crawlSecondaryImages(doc);

			// Description
			String description = crawlDescription(doc);

			// Stock
			Integer stock = null;

			// Marketplace map
			Map<String, Float> marketplaceMap = crawlMarketplace(doc);

			// Marketplace
			Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap);

			// Prices
			Prices prices = crawlPrices(price, internalId);

			// Creating the product
			Product product = new Product();
			product.setUrl(this.session.getOriginalURL());
			product.setInternalId(internalId);
			product.setInternalPid(internalPid);
			product.setName(name);
			product.setPrice(price);
			product.setPrices(prices);
			product.setAvailable(available);
			product.setCategory1(category1);
			product.setCategory2(category2);
			product.setCategory3(category3);
			product.setPrimaryImage(primaryImage);
			product.setSecondaryImages(secondaryImages);
			product.setDescription(description);
			product.setStock(stock);
			product.setMarketplace(marketplace);

			products.add(product);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return products;
	}


	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		if ((url.contains("/p/")))
			return true;
		return false;
	}



	/*******************
	 * General methods *
	 *******************/

	private String crawlInternalId(Document document) {
		String internalId = null;
		Element internalIdElement = document.select("#productCod").first();

		if (internalIdElement != null) {
			internalId = internalIdElement.attr("value").trim();
		}

		return internalId;
	}

	private String crawlInternalPid(String url) {
		String internalPid = null;

		if (url.contains("/p/")) {
			String[] tokens = url.split("/");
			internalPid = tokens[tokens.length - 1].replaceAll("[^0-9]", "");
		}

		return internalPid;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("h1[itemprop=name]").first();

		if (nameElement != null) {
			name = nameElement.text().toString().trim();
		}

		return name;
	}

	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element specialPrice = document.select(".prince-product-default").first();

		if (specialPrice != null) {
			price = Float.parseFloat(specialPrice.text().toString().replaceAll("[^0-9,]+", "")
					.replaceAll("\\.", "").replaceAll(",", "."));
		}

		return price;
	}

	private boolean crawlAvailability(Document document) {
		Element notifyMeElement = document.select(".text-not-product-avisme").first();

		if (notifyMeElement != null) {
			return false;
		}

		return true;
	}

	private Map<String, Float> crawlMarketplace(Document document) {
		return new HashMap<String, Float>();
	}

	private Marketplace assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		return new Marketplace();
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select(".sust-gallery div.item .thumb img").first();

		if (primaryImageElement != null) {
			String image = primaryImageElement.attr("data-zoom-image");
			if (image != null) {
				primaryImage = image.trim();
			}

		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select(".sust-gallery div.item .thumb img");

		for (int i = 1; i < imagesElement.size(); i++) { // start with index 1 because the first image
																											// is the primary image
			Element e = imagesElement.get(i);

			if (e.attr("data-zoom-image") != null && !e.attr("data-zoom-image").isEmpty()) {
				secondaryImagesArray.put(e.attr("data-zoom-image").trim());
			}
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(".breadcrumb li span");

		for (int i = 1; i < elementCategories.size() - 1; i++) { // starting from index 1, because the
																															// first is the market name
			if (!categories.contains(elementCategories.get(i).text().trim())) {
				categories.add(elementCategories.get(i).text().trim());
			}
		}

		return categories;
	}

	private String getCategory(ArrayList<String> categories, int n) {
		if (n < categories.size()) {
			return categories.get(n);
		}

		return "";
	}

	private String crawlDescription(Document document) {
		String description = "";
		Elements descriptionElements = document.select("#accordionFichaTecnica");

		if (descriptionElements != null) {
			description = description + descriptionElements.html();
		}

		Element desc = document.select(".productDetailsPageDescription").first();

		if (desc != null) {
			description += desc.outerHtml();
		}

		return description;
	}

	private Prices crawlPrices(Float price, String internalId) {
		Prices prices = new Prices();

		if (price != null) {
			prices.setBankTicketPrice(price);

			String url = "https://www.carrefour.com.br/installment/creditCard?productPrice=" + price
					+ "&productCode=" + internalId;
			String json = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, cookies);

			JSONObject jsonPrices = new JSONObject();

			try {
				jsonPrices = new JSONObject(json);
			} catch (Exception e) {
				Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
			}

			if (jsonPrices.has("maestroInstallments")) {
				Map<Integer, Float> installmentPriceMap =
						crawlInstallment(jsonPrices, "maestroInstallments");
				prices.insertCardInstallment(Card.MAESTRO.toString(), installmentPriceMap);
			}

			if (jsonPrices.has("carrefourInstallments")) {
				Map<Integer, Float> installmentPriceMap =
						crawlInstallment(jsonPrices, "carrefourInstallments");

				prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);
			}

			if (jsonPrices.has("mastercardInstallments")) {
				Map<Integer, Float> installmentPriceMap =
						crawlInstallment(jsonPrices, "mastercardInstallments");
				prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			}

			if (jsonPrices.has("dinersInstallments")) {
				Map<Integer, Float> installmentPriceMap =
						crawlInstallment(jsonPrices, "dinersInstallments");
				prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
			}

			if (jsonPrices.has("visaInstallments")) {
				Map<Integer, Float> installmentPriceMap = crawlInstallment(jsonPrices, "visaInstallments");
				prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			}

			if (jsonPrices.has("hipercardInstallments")) {
				Map<Integer, Float> installmentPriceMap =
						crawlInstallment(jsonPrices, "hipercardInstallments");
				prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
			}

			if (jsonPrices.has("amexInstallments")) {
				Map<Integer, Float> installmentPriceMap = crawlInstallment(jsonPrices, "amexInstallments");
				prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			}

			if (jsonPrices.has("eloInstallments")) {
				Map<Integer, Float> installmentPriceMap = crawlInstallment(jsonPrices, "eloInstallments");
				prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
			}
		}

		return prices;
	}

	private Map<Integer, Float> crawlInstallment(JSONObject jsonPrices, String keyCard) {
		Map<Integer, Float> installmentPriceMap = new HashMap<>();
		JSONArray installments = jsonPrices.getJSONArray(keyCard);

		for (int i = 0; i < installments.length(); i++) {
			JSONObject jsonInstallment = installments.getJSONObject(i);

			if (jsonInstallment.has("index")) {
				Integer installment = jsonInstallment.getInt("index");

				if (jsonInstallment.has("value")) {
					Float value = Float.parseFloat(jsonInstallment.getString("value")
							.replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());

					installmentPriceMap.put(installment, value);
				}
			}
		}

		return installmentPriceMap;
	}
}
