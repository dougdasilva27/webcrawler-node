package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;

/************************************************************************************************************************************************************************************
 * Crawling notes (19/07/2016):
 * 
 * 1) For this crawler, we have one URL for one sku, but if has more then on sku in url, this crawler was prepared.
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking for an specific html element.
 * 
 * 5) If the sku is unavailable, it's price is not displayed.
 * 
 * 6) The price of sku, found in json script, is wrong when the same is unavailable, then it is not crawled.
 * 
 * 7) There is internalPid for skus in this ecommerce. The internalPid is a number that is the same for all
 * the variations of a given sku.
 * 
 * 8) The primary image is the first image on the secondary images.
 * 
 * 9) Images are get in api "http://www.supermuffato.com.br/produto/sku/" + internalId
 * 
 * 10) In json with images has attributte isMain(boolean), if this is true, the image is the primary image.
 * 
 * 
 * Examples:
 * ex1 (available): http://www.supermuffato.com.br/smartphone-motorola-moto-g4-4g-android-6-0-octa-core-16gb-camera-13mp-tela-5-5-pol-preto-1017828/p
 * ex2 (unavailable): http://www.havan.com.br/panela-de-pressao-clock-inox-6-0-litros/p
 *
 *
 ************************************************************************************************************************************************************************************/

public class BrasilSupermuffatoCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.supermuffato.com.br/";

	public BrasilSupermuffatoCrawler(CrawlerSession session) {
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
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
			
			// Pid
			String internalPid = crawlInternalPid(doc);
			
			// Categories
			ArrayList<String> categories = crawlCategories(doc);
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);

			// Description
			String description = crawlDescription(doc);

			// Stock
			Integer stock = null;

			// Marketplace map
			Map<String, Float> marketplaceMap = crawlMarketplace(doc);

			// Marketplace
			JSONArray marketplace = assembleMarketplaceFromMap(marketplaceMap);
			
			// sku data in json
			JSONArray arraySkus = crawlSkuJsonArray(doc);			
			
			for(int i = 0; i < arraySkus.length(); i++){
				JSONObject jsonSku = arraySkus.getJSONObject(i);
				
				// Availability
				boolean available = crawlAvailability(jsonSku);

				// InternalId 
				String internalId = crawlInternalId(jsonSku);
				
				// Price
				Float price = crawlMainPagePrice(jsonSku, available);
				
				// Get sku information json from the endpoint
				JSONArray jsonImages = crawlJsonImagesFromAPI(doc, internalId);

				// Primary image
				String primaryImage = crawlPrimaryImage(jsonImages);

				// Secondary images
				String secondaryImages = crawlSecondaryImages(jsonImages);
				
				// Name
				String name = crawlName(doc, jsonSku);
				
				// Creating the product
				Product product = new Product();
				
				product.setUrl(session.getOriginalURL());
				product.setInternalId(internalId);
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
				
		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}
		
		return products;

	}

	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(Document document) {
		if ( document.select(".prd-info-container").first() != null ) return true;
		return false;
	}
	
	
	/*******************
	 * General methods *
	 *******************/

	private String crawlInternalId(JSONObject json) {
		String internalId = null;

		if (json.has("sku")) {
			internalId = Integer.toString((json.getInt("sku"))).trim();			
		}

		return internalId;
	}	


	private String crawlInternalPid(Document document) {
		String internalPid = null;
		Element internalPidElement = document.select("#___rc-p-id").first();

		if (internalPidElement != null) {
			internalPid = internalPidElement.attr("value").toString().trim();
		}

		return internalPid;
	}

	private String crawlName(Document document, JSONObject jsonSku) {
		String name = null;
		Element nameElement = document.select(".productName").first();

		String nameVariation = jsonSku.getString("skuname");

		if (nameElement != null) {
			name = nameElement.text().toString().trim();
			
			if(name.length() > nameVariation.length()){
				name += " " + nameVariation;
			} else {
				name = nameVariation;
			}
		}

		return name;
	}

	private Float crawlMainPagePrice(JSONObject json, boolean available) {
		Float price = null;

		if (json.has("bestPriceFormated") && available) {
			price = Float.parseFloat( json.getString("bestPriceFormated").replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		}

		return price;
	}

	private boolean crawlAvailability(JSONObject json) {

		if(json.has("available")) return json.getBoolean("available");

		return false;
	}

	private Map<String, Float> crawlMarketplace(Document document) {
		return new HashMap<String, Float>();
	}

	private JSONArray assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		return new JSONArray();
	}

	private String crawlPrimaryImage(JSONArray skuJson) {
		String primaryImage = null;

		JSONObject sku = skuJson.getJSONObject(0);
		if (sku.has("Images")) {
			JSONArray images = sku.getJSONArray("Images");
			for (int i = 0; i < images.length(); i++) {
				JSONArray innerImagesArray = images.getJSONArray(i);
				if ( this.isArrayOfMainImages(innerImagesArray) ) {
					primaryImage = this.getLargestImage(innerImagesArray);
				}
			}
		}

		return primaryImage;
	}
	
	private String crawlSecondaryImages(JSONArray skuJson) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();
		
		JSONObject sku = skuJson.getJSONObject(0);
		if (sku.has("Images")) {
			JSONArray images = sku.getJSONArray("Images");
			for (int i = 0; i < images.length(); i++) {
				JSONArray innerImagesArray = images.getJSONArray(i);
				if ( !this.isArrayOfMainImages(innerImagesArray) ) {
					secondaryImagesArray.put( this.getLargestImage(innerImagesArray) );
				}
			}
		}
		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private boolean isArrayOfMainImages(JSONArray innerImagesArray) { // Identify the inner array of images that are the primary image
		JSONObject innerImage = (JSONObject) innerImagesArray.get(0);
		if (innerImage.has("IsMain")) {
			if (innerImage.getBoolean("IsMain")) return true;
		}
		return false;
	}
	
	private String getLargestImage(JSONArray innerImagesArray) {
		for (int i = 0; i < innerImagesArray.length(); i++) {
			JSONObject innerImage = (JSONObject) innerImagesArray.get(i);
			if (innerImage.has("Path")) {
				String imagePath = innerImage.getString("Path");
				if (imagePath.contains("-1000-1000/")) return imagePath;
			}
		}
		
		return null;
	}

	/**
	 * 
	 * @param document
	 * @param internalId
	 * @return array with one position
	 */
	private JSONArray crawlJsonImagesFromAPI(Document document, String internalId) {
		JSONArray skuData = new JSONArray();
		if (internalId != null) {
			String apiURL = HOME_PAGE + "produto/sku/" + internalId;
			String json = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, apiURL, null, null);
						
			try {
				skuData = new JSONArray(json);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		return skuData;
	}


	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(".bread-crumb > ul li a");

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
		Element descriptionElement = document.select("#prd-description").first();
		Element specElement = document.select("#prd-specifications").first();

		if (descriptionElement != null) description = description + descriptionElement.html();
		if (specElement != null) description = description + specElement.html();

		return description;
	}

	/**
	 * Get the script having a json with the availability information
	 * @return
	 */
	private JSONArray crawlSkuJsonArray(Document document) {
		Elements scriptTags = document.getElementsByTag("script");
		JSONObject skuJson = null;
		JSONArray skuJsonArray = null;

		for (Element tag : scriptTags){                
			for (DataNode node : tag.dataNodes()) {
				if(tag.html().trim().startsWith("var skuJson_0 = ")) {

					skuJson = new JSONObject
							(
									node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1] +
									node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1].split(Pattern.quote("}]};"))[0]
									);

				}
			}        
		}

		try {
			skuJsonArray = skuJson.getJSONArray("skus");
		} catch(Exception e) {
			e.printStackTrace();
		}

		return skuJsonArray;
	}
}
