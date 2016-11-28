package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;

/**
 * 
 * 1) Only one sku per page.
 * 
 * Price crawling notes:
 * 1) In time crawler was made, there no product unnavailable.
 * 2) There is no bank slip (boleto bancario) payment option.
 * 3) There is no installments for card payment. So we only have 
 * 1x payment, and to this value we use the cash price crawled from
 * the sku page. (nao existe divisao no cartao de credito).
 * 
 * @author Gabriel Dornelas
 *
 */
public class MexicoWalmartsuperCrawler extends Crawler {

	private final String HOME_PAGE = "https://super.walmart.com.mx";

	public MexicoWalmartsuperCrawler(Session session) {
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


			String internalId = crawlInternalId(doc);
			String internalPid = crawlInternalPid(doc);
			String name = crawlName(doc);
			Float price = crawlPrice(doc);
			Prices prices = crawlPrices(price);
			boolean available = crawlAvailability(doc);
			CategoryCollection categories = crawlCategories(doc);
			String primaryImage = crawlPrimaryImage(doc);
			String secondaryImages = crawlSecondaryImages(doc);
			String description = crawlDescription(doc);
			Integer stock = null;
			JSONArray marketplace = crawlMarketplace(doc);

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
		if (doc.select(".product-details").first() != null) return true;
		return false;
	}

	private String crawlInternalId(Document document) {
		String internalId = null;

		Element internalIdElement = document.select("span[itemprop=productID]").first();
		if (internalIdElement != null) {
			internalId = internalIdElement.text();
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
		Element nameElement = document.select(".p-name h1 span").first();

		if (nameElement != null) {
			name = nameElement.text().trim();
		}

		return name;
	}

	private Float crawlPrice(Document document) {
		Float price = null;

		String priceText = null;
		Element salePriceElement = document.select(".p-price.price").first();		

		if (salePriceElement != null) {
			priceText = salePriceElement.text();
		}

		if (priceText != null && !priceText.isEmpty()) {
			price = Float.parseFloat(priceText.replaceAll("\\$", "").replaceAll(",", "").trim());
		}

		return price;
	}

	private boolean crawlAvailability(Document document) {
		boolean available = false;

		Element outOfStockElement = document.select(".slider-button input.enable").first();
		if (outOfStockElement != null) {
			available = true;
		}

		return available;
	}

	private JSONArray crawlMarketplace(Document document) {
		return new JSONArray();
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select(".slider-img-display img").first();

		if (primaryImageElement != null) {			
			primaryImage = primaryImageElement.attr("data-extralarge").trim();
			
			if(primaryImage.isEmpty()){
				primaryImage = primaryImageElement.attr("url").trim();
			}
			
			if(primaryImage.isEmpty()){
				primaryImage = primaryImageElement.attr("src").trim();
			}
		}
		
		if(!primaryImage.contains("walmart") && !primaryImage.isEmpty()){
			primaryImage = HOME_PAGE + primaryImage;
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select(".slider-img-display img");

		for (int i = 1; i < imagesElement.size(); i++) { // first is the primary image
			String image = HOME_PAGE + imagesElement.get(i).attr("data-extralarge").trim();
			
			if(image.isEmpty()){
				image = imagesElement.get(i).attr("url").trim();
			}
			
			if(image.isEmpty()){
				image = imagesElement.get(i).attr("src").trim();
			}
			
			secondaryImagesArray.put( image );	
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private CategoryCollection crawlCategories(Document document) {
		CategoryCollection categories = new CategoryCollection();

		Elements elementCategories = document.select(".product-nav > li a");
		for (int i = 1; i < elementCategories.size(); i++) { // starting from index 1, because the first is the market name
			categories.add( elementCategories.get(i).text().trim() );
		}

		return categories;
	}

	private String crawlDescription(Document document) {
		StringBuilder description = new StringBuilder();
		Element descriptionElement = document.select("content.e-description.description").first();
		
		if(descriptionElement != null) description.append(descriptionElement.html());
		

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
	private Prices crawlPrices(Float price) {
		Prices prices = new Prices();

		if(price != null){
			Map<Integer,Float> installmentPriceMap = new TreeMap<Integer, Float>();
			installmentPriceMap.put(1, price);
	
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
		}

		return prices;
	}

}
