package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.Crawler;
import br.com.lett.crawlernode.kernel.task.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;


/************************************************************************************************************************************************************************************
 * Crawling notes (22/08/2016):
 * 
 * 1) For this crawler, we have one URL for multiple skus.
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking for an specific html element.
 * 
 * 5) If the sku is unavailable, it's price is displayed.
 * 
 * 6) In json script in html has variations of product.
 * 
 * 7) There is internalPid for skus in this ecommerce. The internalPid is a number that is the same for all
 * the variations of a given sku.
 * 
 * 7) The primary image is the first image on the secondary images.
 * 
 * 8) To get internalID and Name of sku Variations is crawl in json script in html.
 * 
 * 
 * Examples:
 * ex1 (available): http://www.rrmaquinas.com.br/bomba-d-agua-pressurizadora-220v-1600l-hora-3-4-bpf15-9-120-ferrari.html
 * ex2 (unavailable): http://www.rrmaquinas.com.br/ar-condicionado-portatil-12000-btus-piu-quente-frio-olimpia-splendid.html
 *
 *
 ************************************************************************************************************************************************************************************/

public class BrasilRrmaquinasCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.rrmaquinas.com.br/";

	public BrasilRrmaquinasCrawler(CrawlerSession session) {
		super(session);
	}
	
	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);

		List<Product> products = new ArrayList<Product>();
		
		if ( isProductPage(doc) ) {

			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			/* ***********************************
			 * crawling data of only one product *
			 *************************************/

			// Pid
			String internalPid = crawlInternalPid(doc);

			// Name
			String nameMainPage = crawlName(doc);
			
			// Price
			Float priceMainPage = crawlPrice(doc);
			
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
			JSONArray marketplace = assembleMarketplaceFromMap(marketplaceMap);

			// Sku variations
			Element skus = doc.select(".product-options").first();
			
			if(skus != null){
				
				/* ***********************************
				 * crawling data of mutiple products *
				 *************************************/

				// Sku Json
				JSONArray jsonSku = this.crawlJsonVariations(doc);
				
				for(int i = 0; i < jsonSku.length(); i++){
					
					JSONObject sku = jsonSku.getJSONObject(i);
					
					// InternalId
					String internalID = crawlInternalIdForMutipleVariations(sku);
					
					// Name
					String name = crawlNameForMutipleVariations(sku, nameMainPage);
					
					// Creating the product
					Product product = new Product();
					
					product.setUrl(this.session.getUrl());
					product.setInternalId(internalID);
					product.setInternalPid(internalPid);
					product.setName(name);
					product.setPrice(priceMainPage);
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
				}
				
				/* *********************************
				 * crawling data of single product *
				 ***********************************/
				
			} else {
				
				// InternalId
				String internalID = crawlInternalIdSingleProduct(doc);

				// Creating the product
				Product product = new Product();
				
				product.setUrl(this.session.getUrl());
				product.setInternalId(internalID);
				product.setInternalPid(internalPid);
				product.setName(nameMainPage);
				product.setPrice(priceMainPage);
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
				
			}


		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}
		
		return products;

	}

	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(Document document) {
		if ( document.select(".product-view").first() != null ) return true;
		return false;
	}
	
	/*********************
	 * Variation methods *
	 *********************/
	
	private String crawlInternalIdForMutipleVariations(JSONObject sku) {
		String internalId = null;

		if(sku.has("products")){
			internalId = sku.getJSONArray("products").getString(0).trim();
		}
		
		return internalId;
	}
	
	private String crawlNameForMutipleVariations(JSONObject jsonSku, String name) {
		String nameVariation = name;	
		
		if(jsonSku.has("label")){
			nameVariation = nameVariation + " - " + jsonSku.getString("label");
		}	

		return nameVariation;
	}

	/**********************
	 * Single Sku methods *
	 **********************/
	
	private String crawlInternalIdSingleProduct(Document document) {
		String internalId = null;
		Element internalIdElement = document.select(".no-display input[name=product]").first();

		if (internalIdElement != null) {
			internalId = internalIdElement.attr("value").trim();
		}

		return internalId;
	}
	
	/*******************
	 * General methods *
	 *******************/
	
	private Float crawlPrice(Document doc) {
		Float price = null;	
		Element priceElement = doc.select("#formas-pagamento-box ul li span").first();
		
		if(priceElement != null){
			price = Float.parseFloat(priceElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
		}	

		return price;
	}
	
	private boolean crawlAvailability(Document doc) {
		Element e = doc.select(".alert-stock").first();
		
		if (e != null) {
			return false;
		}
		
		return true;
	}
	
	private JSONArray crawlJsonVariations(Document doc){
		JSONArray jsonSku = new JSONArray();
		Elements scripts = doc.select(".product-options script");
		
		String id = doc.select("select.super-attribute-select").first().attr("id").replaceAll("[^0-9]", "").trim();
		
		String term = "Product.Config(";
		
		for(Element e : scripts){
			String script = e.outerHtml().trim();
			
			if(script.contains(term)){
				int x = script.indexOf(term);
				int y = script.indexOf(");", x + term.length());
				
				JSONObject json = new JSONObject(script.substring(x + term.length(), y).trim());
				
				if(json.has("attributes")){
					json = json.getJSONObject("attributes");
					
					if(json.has(id)){
						json = json.getJSONObject(id);
						
						if(json.has("options")){
							jsonSku = json.getJSONArray("options");
						}
					}
				}
				
				break;
			}
		}
		
		return jsonSku;
	}
	
	private String crawlInternalPid(Document document) {
		String internalPid = null;

		Element internalPidElement = document.select(".product-essential p.right").first();

		if (internalPidElement != null) {
			String pid = internalPidElement.text().toString().trim();	
			
			int x = pid.indexOf(":");
			internalPid = pid.substring(x+1).trim();
		}
		
		return internalPid;
	}
	
	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select(".product-name").first();

		if (nameElement != null) {
			name = nameElement.text().toString().trim();
		}

		return name;
	}

	private Map<String, Float> crawlMarketplace(Document document) {
		return new HashMap<String, Float>();
	}
	
	private JSONArray assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		return new JSONArray();
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select(".product-image a").first();

		if (primaryImageElement != null) {
			primaryImage = primaryImageElement.attr("href").trim();
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select(".more-views ul li a");

		for (int i = 1; i < imagesElement.size(); i++) { // start with indez 1 because the first image is the primary image
			Element e = imagesElement.get(i);
			
			secondaryImagesArray.put( e.attr("href").trim() );
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(".breadcrumbs li a");

		for (int i = 1; i < elementCategories.size(); i++) { // start with index 1 because the first item is the home page
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
		Element descriptionElement = document.select("#tab1").first();

		if (descriptionElement != null) description = description + descriptionElement.html();

		return description;
	}

}
