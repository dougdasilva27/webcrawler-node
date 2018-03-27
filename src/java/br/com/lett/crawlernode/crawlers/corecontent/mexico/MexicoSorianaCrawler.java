package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import java.util.ArrayList;
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
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/**
 * 
 * 1) Only one sku per page.
 * 
 * Price crawling notes:
 * 1) Even with the product unavailable, it's price is displayed.
 * 2) There is no bank slip (boleto bancario) payment option.
 * 3) There is no installments for card payment. So we only have 
 * 1x payment, and to this value we use the cash price crawled from
 * the sku page. (nao existe divisao no cartao de credito).
 * 
 * @author Samir Leao
 *
 */
public class MexicoSorianaCrawler extends Crawler {

	private final String HOME_PAGE = "https://www.soriana.com/";

	public MexicoSorianaCrawler(Session session) {
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
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			Document imagesPageDocument = fetchImagesPage();

			String internalId = crawlInternalId(doc);
			String internalPid = crawlInternalPid(doc);
			String name = crawlName(doc);
			Float price = crawlPrice(doc);
			Prices prices = crawlPrices(doc);
			boolean available = crawlAvailability(doc);
			CategoryCollection categories = crawlCategories(doc);
			String primaryImage = crawlPrimaryImage(imagesPageDocument);
			String secondaryImages = crawlSecondaryImages(imagesPageDocument);
			String description = crawlDescription(doc);
			Integer stock = null;
			Marketplace marketplace = crawlMarketplace(doc);

			// Creating the product
			Product product = ProductBuilder.create()
					.setUrl(session.getOriginalURL())
					.setInternalId(internalId)
					.setInternalPid(internalPid)
					.setName(name)
					.setPrice(price)
					.setPrices(prices)
					.setAvailable(available)
					.setCategory1(categories.getCategory(0))
					.setCategory2(categories.getCategory(1))
					.setCategory3(categories.getCategory(2))
					.setPrimaryImage(primaryImage)
					.setSecondaryImages(secondaryImages)
					.setDescription(description)
					.setStock(stock)
					.setMarketplace(marketplace)
					.build();

			products.add(product);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return products;

	}

	private boolean isProductPage(Document doc) {
		if (doc.select("div.productDetailsPanel").first() != null) return true;
		return false;
	}

	private String crawlInternalId(Document document) {
		String internalId = null;

		Element internalIdElement = document.select("input[name=productCodePost]").first();
		if (internalIdElement != null) {
			internalId = internalIdElement.attr("value");
		}

		return internalId;
	}

	/**
	 * There is no internalPid.
	 * 
	 * @param document
	 * @return
	 */
	private String crawlInternalPid(Document document) {
		String internalPid = null;

		return internalPid;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("div.productDescription h1").first();

		if (nameElement != null) {
			name = nameElement.text().trim();
		}

		return name;
	}

	private Float crawlPrice(Document document) {
		Float price = null;

		String priceText = null;
		Element salePriceElement = document.select("div.big-price .sale-price").first();		

		if (salePriceElement != null) {
			priceText = salePriceElement.ownText();
		} else {
			Element bigPriceElement = document.select("div.big-price").first();
			if (bigPriceElement != null) {
				priceText = bigPriceElement.text();
			}
		}

		if (priceText != null && !priceText.isEmpty()) {
			price = Float.parseFloat(priceText.replaceAll(MathUtils.PRICE_REGEX, ""));
		}

		return price;
	}

	private boolean crawlAvailability(Document document) {
		boolean available = true;

		Element outOfStockElement = document.select("button.outOfStock").first();
		if (outOfStockElement != null) {
			available = false;
		}

		return available;
	}

	private Marketplace crawlMarketplace(Document document) {
		return new Marketplace();
	}

	/**
	 * Fetch a page containing all the images URLs
	 * of their largest version.
	 * 
	 * @return
	 */
	private Document fetchImagesPage() {
		String url = session.getOriginalURL() + "/zoomImages";
		return DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, null);
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select("div.productImagePrimary img").first();

		if (primaryImageElement != null) {			
			primaryImage = "https://www.soriana.com" + primaryImageElement.attr("src").trim();
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select("div.productImageGallery ul li img");

		for (int i = 1; i < imagesElement.size(); i++) { 
			String image = "https://www.soriana.com" + imagesElement.get(i).attr("data-zoomurl").trim();
			secondaryImagesArray.put( image );	
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private CategoryCollection crawlCategories(Document document) {
		CategoryCollection categories = new CategoryCollection();

		Elements elementCategories = document.select("#breadcrumb li a");
		for (int i = 1; i < elementCategories.size()-1; i++) { // starting from index 1, because the first is the market name
			categories.add( elementCategories.get(i).text().trim() );
		}

		return categories;
	}

	private String crawlDescription(Document document) {
		StringBuilder description = new StringBuilder();

		Elements descriptionElements = document.select("#productTabs div.tabBody");
		for (Element details : descriptionElements) {
			description.append(details.html());
		}

		return description.toString();
	}

	/**
	 * There is no bankSlip price.
	 * 
	 * There is no card payment options, other than cash price.
	 * So for installments, we will have only one installment for each
	 * card brand, and it will be equals to the price crawled on the sku
	 * main page.
	 * 
	 * @param doc
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(Document document) {
		Prices prices = new Prices();

		Float cashPrice = crawlPrice(document);

		Map<Integer,Float> installmentPriceMap = new TreeMap<Integer, Float>();

		installmentPriceMap.put(1, cashPrice);

		prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
		prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
		prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);

		return prices;
	}

}
