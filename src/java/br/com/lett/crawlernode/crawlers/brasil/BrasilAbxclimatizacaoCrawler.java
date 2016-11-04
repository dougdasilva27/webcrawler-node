package br.com.lett.crawlernode.crawlers.brasil;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

/************************************************************************************************************************************************************************************
 * Crawling notes (25/08/2016):
 * 
 * 1) For this crawler, we have one url per each sku. There is no page is more than one sku in it.
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking for an specific html element.
 * 
 * 5) Even if a product is unavailable, its price is displayed.
 * 
 * 6) There is no internalPid for skus in this ecommerce. 
 * 
 * 7) The primary image is the first image in the secondary images selector.
 * 
 * Examples:
 * ex1 (available): https://www.abxclimatizacao.com.br/split-springer-window-9-000-btus-110v.html
 * ex2 (unavailable): https://www.abxclimatizacao.com.br/split-hi-wall-midea-vita-inverter-12-000-btus-220v-quente-frio.html
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/
public class BrasilAbxclimatizacaoCrawler extends Crawler {

	private final String HOME_PAGE = "https://www.abxclimatizacao.com.br/";

	public BrasilAbxclimatizacaoCrawler(CrawlerSession session) {
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

			/* ***********************************
			 * crawling data of only one product *
			 *************************************/

			// InternalId
			String internalId = crawlInternalId(doc);

			// Pid
			String internalPid = crawlInternalPid(doc);

			// Name
			String name = crawlName(doc);

			// Price
			Float price = crawlMainPagePrice(doc);

			// Prices
			Prices prices = crawlPrices(doc, price);

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
			String secondaryImages = crawlSecondaryImages(doc, primaryImage);

			// Description
			String description = crawlDescription(doc);

			// Stock
			Integer stock = null;

			// Marketplace map
			Map<String, Float> marketplaceMap = crawlMarketplace(doc);

			// Marketplace
			JSONArray marketplace = assembleMarketplaceFromMap(marketplaceMap);

			// Creating the product
			Product product = new Product();
			product.setUrl(session.getOriginalURL());
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

	private boolean isProductPage(Document doc) {
		if (doc.select(".product-name") != null ) return true;
		return false;
	}


	/*******************
	 * General methods *
	 *******************/

	private String crawlInternalId(Document document) {
		String internalId = null;
		Elements internalIdElements = document.select(".data-table tr");

		for(Element e : internalIdElements){
			String ref = e.text().trim();

			if(ref.toLowerCase().startsWith("ref")){
				int x = ref.indexOf(":") + 1;

				internalId = ref.substring(x).trim();
			}
		}

		return internalId;
	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;

		return internalPid;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select(".product-name h1").first();

		if (nameElement != null) {
			name = nameElement.text().toString().trim();
		}

		return name;
	}

	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element specialPrice = document.select(".precos .new").first();		

		if (specialPrice != null) {
			price = Float.parseFloat( specialPrice.text().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		} 

		return price;
	}

	private boolean crawlAvailability(Document document) {
		Element notifyMeElement = document.select(".availability.out-of-stock").first();

		if (notifyMeElement != null) {
			return false;
		}

		return true;
	}

	private Map<String, Float> crawlMarketplace(Document document) {
		return new HashMap<String, Float>();
	}

	private JSONArray assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		return new JSONArray();
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select(".cloud-zoom").first();

		if (primaryImageElement != null) {			
			primaryImage = primaryImageElement.attr("href").trim();
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document, String primaryImage) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select(".item .cloud-zoom-gallery");

		for (int i = 0; i < imagesElement.size(); i++) { 
			String image = imagesElement.get(i).attr("href").trim();

			if(!image.replace("thumbnail", "image").equals(primaryImage)) {
				secondaryImagesArray.put( image );	
			}

		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();

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
		Element descriptionElement = document.select(".short-description").first();
		Element princElement = document.select(".product-collateral").first();

		if (descriptionElement != null) description = description + descriptionElement.html();
		if (princElement != null) description = description + princElement.html();

		return description;
	}

	private Prices crawlPrices(Document doc, Float price){
		Prices prices = new Prices();

		if(price != null){
			Map<Integer,Float> installmentPriceMap = new TreeMap<Integer, Float>();

			// bank ticket
			Element boleto = doc.select(".boleto_bancario_view").first();
			if(boleto != null){
				Float bankTicket = CommonMethods.parseFloat(boleto.ownText());
				prices.insertBankTicket(bankTicket);
			}

			// card payment conditions
			Elements installments = doc.select(".tabela1 td");
			for(Element e : installments){
				String text = e.text();
				int x = text.indexOf("x");

				Integer installment = Integer.parseInt(text.substring(0, x).trim());
				Float value = CommonMethods.parseFloat(text.substring(x+1));

				installmentPriceMap.put(installment, value);
			}

			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.BNDES.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
		}

		return prices;
	}

}
