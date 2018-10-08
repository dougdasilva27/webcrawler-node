package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;


/************************************************************************************************************************************************************************************
 * Crawling notes (11/08/2016):
 * 
 * 1) For this crawler, we have URLs with one single sku.
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking the URL format.
 * 
 * 5) Even if a product is unavailable, its price is displayed.
 * 
 * 6) There is internalPid for skus in this ecommerce. The internalPid must be a number that is the same for all
 * the variations of a given sku.
 * 
 * 7) For this crawler, the page of sku is maked with (url + "?skuId=" + internalID), like this: https://www.etna.com.br/etna/p/ar-condicionado-portatil-consul-12000-btus-127v/045303?skuId=0400295
 * 
 * 8) In page of sku, has links to variations of these.
 * 
 * 9) In voltage variations there is no internalPid, becuase the variations not appear in page of sku.
 * 
 * 
 * Examples:
 * ex1 (available): https://www.etna.com.br/etna/p/cabide-adulto-ker-acrilico/005225
 * ex2 (unavailable): https://www.etna.com.br/etna/p/ar-condicionado-portatil-consul-12000-btus-127v/045303?skuId=0400295
 * 
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilEtnamoveisCrawler extends Crawler {

	public BrasilEtnamoveisCrawler(Session session) {
		super(session);
	}

	private final String HOME_PAGE = "https://www.etna.com.br/";

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE)); 
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<>();

		if ( isProductPage(this.session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
			
			// Url
			String url = session.getOriginalURL();
			
			// InternalId
			String internalId = crawlInternalId(doc);
			
			// Pid
			String internalPid = crawlInternalPid(doc);
			
			// Stock
			Integer stock = null;

			// Marketplace map
			Map<String, Float> marketplaceMap = crawlMarketplace(doc);

			// Marketplace
			Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap);
			
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
			
			// url Product
			String urlSku = this.makeUrlProduct(url, internalId);
			
			// Prices
			Prices prices = crawlPrices(price, doc);
			
			// Creating the product
			Product product = new Product();
			
			product.setUrl(urlSku);
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
		if ( url.startsWith(HOME_PAGE + "etna/p/")) return true;
		return false;
	}
	
	
	/*******************
	 * General methods *
	 *******************/
	
	private String makeUrlProduct(String url, String internalId){
		String urlSku = url;
		
		if(!urlSku.contains("?skuId")){
			urlSku = urlSku + "?skuId=" + internalId;
		}
		
		return urlSku;
	}
	
	private String crawlInternalId(Document document) {
		String internalId = null;
		Element internalIdElement = document.select("span#sku-id span").first();

		if (internalIdElement != null) {
			internalId = internalIdElement.text().trim();			
		}

		return internalId;
	}

	private String crawlInternalPid(Document doc) {
		String internalPid = null;
		Element pidElement = doc.select("#giftlistProductId").first();
		
		if(pidElement != null){
			internalPid = pidElement.attr("value");
		}
		
		return internalPid;
	}
	
	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("#sku-name").first();

		if (nameElement != null) {
			name = nameElement.ownText().toString().trim();
		}

		return name;
	}

	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element mainPagePriceElement = document.select(".finalValueDescription").first();

		if (mainPagePriceElement != null) {
			price = Float.parseFloat( mainPagePriceElement.text().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim() );
		}

		return price;
	}
	
	private boolean crawlAvailability(Document document) {
		Element notifyMeElement = document.select("#stock-buy h2").first();
		
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
		Element primaryImageElement = document.select("a.produtozoom").first();

		if (primaryImageElement != null && primaryImageElement.attr("href").endsWith(".jpg")) {
			
			String image = primaryImageElement.attr("href");
			if(!image.startsWith("http:")){
				primaryImage = "http:" + image.toLowerCase();
			} else {
				primaryImage = image.toLowerCase();
			}
			
		} else if(primaryImageElement != null) {
			Element e = primaryImageElement.select(".zoomPad img[alt]").first();
			
			if(e != null){
				String image = e.attr("src");
				
				if(!image.startsWith("http:")){
					primaryImage = "http:" + image.toLowerCase();
				} else {
					primaryImage = image.toLowerCase();
				}
			}
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select("a.produtozoom");

		for (int i = 1; i < imagesElement.size(); i++) { // starting from index 1, because the first is the primary image
			Element secondaryImagesElement = imagesElement.get(i);
			if(secondaryImagesElement.attr("href").endsWith(".jpg")){
				
				String image = secondaryImagesElement.attr("href");
				if(!image.startsWith("http:")){
					secondaryImagesArray.put("http:" + image);
				} else {
					secondaryImagesArray.put(image);
				}
				
			} else {
				Element e = secondaryImagesElement.select(".zoomPad img[alt]").first();
				
				String image = e.attr("src");
				if(!image.startsWith("http:")){
					secondaryImagesArray.put("http:" + image);
				} else {
					secondaryImagesArray.put(image);
				}
				
			}
		}
		
		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select("ol.breadcrumb li a");

		for (int i = 1; i < elementCategories.size(); i++) { // starting from index 1, because the first is the market name
			categories.add( elementCategories.get(i).text().trim() );
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
		Element descriptionElement = document.select(".bloc1.prod-info").first();
		Element specificElement = document.select(".bloc2.prod-info").first();

		if (descriptionElement != null) description = description + descriptionElement.html();
		if (specificElement != null) description = description + specificElement.html();

		return description;
	}
	
	private Prices crawlPrices(Float price, Document doc){
		Prices prices = new Prices();
		
		if(price != null){
			Map<Integer,Float> installmentPriceMap = new HashMap<>();
			Element installmentSpecial = doc.select(".parcelasValueProductDescription strong").first();
			
			if(installmentSpecial != null){
				String text = installmentSpecial.text().toLowerCase();
				
				if(text.contains("x")){
					Integer installment = Integer.parseInt(text.split("x")[0].trim());
					Float value = MathUtils.parseFloatWithComma(text.split("x")[1].trim());
					
					installmentPriceMap.put(installment, value);
					
					prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
					prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
					prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
					prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
					prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
					prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
				}
			}
		}
		
		return prices;
	}

}
