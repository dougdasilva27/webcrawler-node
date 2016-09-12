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

import br.com.lett.crawlernode.kernel.fetcher.DataFetcher;
import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.Crawler;
import br.com.lett.crawlernode.kernel.task.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;

/************************************************************************************************************************************************************************************
 * Crawling notes (04/08/2016):
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
 * 6) The price of sku, found in json script, is wrong when the same is unavailable, then it is not crawled.
 * 
 * 7) There is internalPid for skus in this ecommerce. The internalPid is a number that is the same for all
 * the variations of a given sku.
 * 
 * 7) The primary image is the first image on the secondary images.
 * 
 * 8) To get price, availability and Name sku Variations, for all products is accessed a page with json.
 * 
 * 9) In json has variations of product.
 * 
 * Examples:
 * ex1 (available): http://www.mobly.com.br/ar-condicionado-portatil-quente-e-frio-10500-btus-branco-160252.html
 * ex2 (unavailable): http://www.mobly.com.br/armario-multiuso-madeira-178cm-decoracao-marrom-venus-158006.html
 *
 *
 ************************************************************************************************************************************************************************************/

public class BrasilMoblyCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.mobly.com.br/";

	public BrasilMoblyCrawler(CrawlerSession session) {
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
			Elements skus = doc.select("li[data-sku]");
			
			if(skus.size() > 0){
				
				/* ***********************************
				 * crawling data of mutiple products *
				 *************************************/
				// IntenalIDS para requisição
				String internalIDS = crawlInternalIDS(skus);
				
				// Json page
				JSONObject jsonPage = this.fetchSkuInformation(internalIDS);
				
				for(Element sku : skus){
					// InternalId
					String internalID = crawlInternalIdForMutipleVariations(sku);
					
					// Sku Json
					JSONObject jsonSku = this.assembleJsonProduct(internalID, internalPid, jsonPage);
					
					// Name
					String name = crawlNameForMutipleVariations(jsonSku, nameMainPage);
	
					// Price
					Float price = crawlPrice(jsonSku);
					
					// Availability
					boolean available = crawlAvailability(jsonSku);
					
					// Creating the product
					Product product = new Product();
					product.setSeedId(this.session.getSeedId());
					product.setUrl(this.session.getUrl());
					product.setInternalId(internalID);
					product.setInternalPid(internalPid);
					product.setName(name);
					product.setPrice(price);
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
				
				// Json page
				JSONObject jsonPage = this.fetchSkuInformation(internalID);
				
				// Sku Json
				JSONObject jsonSku = this.assembleJsonProduct(internalID, internalPid, jsonPage);

				// Price
				Float price = crawlPrice(jsonSku);
				
				// Availability
				boolean available = crawlAvailability(jsonSku);

				// Creating the product
				Product product = new Product();
				product.setSeedId(this.session.getSeedId());
				product.setUrl(this.session.getUrl());
				product.setInternalId(internalID);
				product.setInternalPid(internalPid);
				product.setName(nameMainPage);
				product.setPrice(price);
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
		if ( document.select(".conteiner.product-detail").first() != null ) return true;
		return false;
	}
	
	/*********************
	 * Variation methods *
	 *********************/
	
	private String crawlInternalIdForMutipleVariations(Element sku) {
		String internalId = null;

		internalId = sku.attr("data-sku").trim();
		
		return internalId;
	}
	
	private String crawlInternalIDS(Elements skus) {
		String internalId = "";

		for(Element e : skus){
			internalId =  internalId + " " + e.attr("data-sku");
		}
		
		internalId = internalId.trim().replaceAll(" ", ",");
		
		return internalId;
	}
	
	private String crawlNameForMutipleVariations(JSONObject jsonSku, String name) {
		String nameVariation = name;	
		
		if(jsonSku.has("NameVariation")){
			nameVariation = nameVariation + "  " + jsonSku.getString("NameVariation");
		}	

		return nameVariation;
	}

	/**********************
	 * Single Sku methods *
	 **********************/
	
	private String crawlInternalIdSingleProduct(Document document) {
		String internalId = null;
		Element internalIdElement = document.select(".sel-product-move-to-wishlist").first();

		if (internalIdElement != null) {
			String[] tokens = internalIdElement.attr("href").split("/");
			internalId = tokens[tokens.length-1].trim();
		}

		return internalId;
	}
	
	private Float crawlPrice(JSONObject jsonSku) {
		Float price = null;	
		
		if(jsonSku.has("Price")){
			price = Float.parseFloat( jsonSku.getString("Price").replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		}	

		return price;
	}
	
	private boolean crawlAvailability(JSONObject jsonSku) {
		
		if (jsonSku.has("Available")) {
			return jsonSku.getBoolean("Available");
		}
		
		return true;
	}

	/*******************
	 * General methods *
	 *******************/
	
	private JSONObject fetchSkuInformation(String internalIDS){
		String url = "http://www.mobly.com.br/api/catalog/price/hash/38b5c35d624459682e3bd23521cb4248f631d39d/?skus="+ internalIDS;
		
		return DataFetcher.fetchJSONObject(DataFetcher.GET_REQUEST, session, url, null, null);
	}
	
	private JSONObject assembleJsonProduct(String internalID, String internalPid, JSONObject jsonPage){
		JSONObject jsonSku = new JSONObject();
		JSONObject jsonInformation = new JSONObject();
		
		if(jsonPage.has("priceStore")){
			JSONObject jsonTemp = jsonPage.getJSONObject("priceStore");
			if(jsonTemp.has(internalPid)){
				JSONObject pidJson = jsonTemp.getJSONObject(internalPid);
				
				if(pidJson.has("prices")){
					JSONObject jsonPrices = pidJson.getJSONObject("prices");
					
					if(jsonPrices.has(internalID)){
						jsonInformation = jsonPrices.getJSONObject(internalID);
					}
				}
			}
		}
		
		if(jsonInformation.has("finalPrice")) 		jsonSku.put("Price", jsonInformation.getString("finalPrice"));
		if(jsonInformation.has("stock_available")) 	jsonSku.put("Available", jsonInformation.getBoolean("stock_available"));
		if(jsonInformation.has("option")) 			jsonSku.put("NameVariation", jsonInformation.getString("option"));
		
		return jsonSku;
	}
	
	private String crawlInternalPid(Document document) {
		String internalPid = null;

		Element internalPidElement = document.select("#configSku").first();

		if (internalPidElement != null) {
			internalPid = internalPidElement.attr("value").toString().trim();			
		}
		
		return internalPid;
	}
	
	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("h1.prd-title").first();

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
		Element primaryImageElement = document.select("#prdImage").first();

		if (primaryImageElement != null) {
			primaryImage = primaryImageElement.attr("src").trim();
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select("#productMoreImagesList li");

		for (int i = 1; i < imagesElement.size(); i++) { // start with indez 1 because the first image is the primary image
			Element e = imagesElement.get(i);
			
			if(e.hasAttr("data-image-big") && !e.attr("data-image-big").isEmpty())	{
				secondaryImagesArray.put( e.attr("data-image-big").trim() );
			} else if(e.hasAttr("data-image-product") && !e.attr("data-image-product").isEmpty()){
				secondaryImagesArray.put( e.attr("data-image-product").trim() );
			} else {
				Element img = e.select("img").first();
				
				if(img != null){
					secondaryImagesArray.put( e.attr("src").trim() );
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
		Elements elementCategories = document.select(".breadcrumb ul li a");

		for (int i = 0; i < elementCategories.size(); i++) { 
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
		Element descriptionElement = document.select(".product-description").first();
		Element specElement = document.select("#product-attributes").first();

		if (descriptionElement != null) description = description + descriptionElement.html();
		if (specElement != null) description = description + specElement.html();

		return description;
	}

}
