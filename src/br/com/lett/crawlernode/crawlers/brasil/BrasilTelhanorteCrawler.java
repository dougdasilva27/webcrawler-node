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

import br.com.lett.crawlernode.kernel.fetcher.DataFetcher;
import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.Crawler;
import br.com.lett.crawlernode.kernel.task.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;

/************************************************************************************************************************************************************************************
 * Crawling notes (12/08/2016):
 * 
 * 1) For this crawler, we have one URL for single sku, but if exists variations, this crawler is prepared.
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
 * 7) The primary image is the first image on the secondary images.
 * 
 * 8) Variations of skus are not crawled if the variation is unavailable because it is not displayed for the user,
 * except if there is a html element voltage, because variations of voltage are displayed for the user even though unavailable.
 * 
 * 9) The url of the primary images are changed to bigger dimensions manually.
 * 
 * 10) The secondary images are get in api "http://www.telhanorte.com.br/produto/sku/" + internalId
 * 
 * Examples:
 * ex1 (available): http://www.telhanorte.com.br/gaveteiro-de-madeira-e-rattan-com-3-gavetas-branco-exclusivo-1200909/p
 * ex2 (unavailable): http://www.telhanorte.com.br/condensadora-de-ar-frio-mecanico-duo-7500-btus-127v-springer-1379291/p
 *
 *
 ************************************************************************************************************************************************************************************/

public class BrasilTelhanorteCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.telhanorte.com.br/";

	public BrasilTelhanorteCrawler(CrawlerSession session) {
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

		if ( isProductPage(session.getUrl()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());
			
			/* **************************************************************
			 * crawling data of multiple variations and the single products *
			 ****************************************************************/
		
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
				
				// Primary image
				String primaryImage = crawlPrimaryImage(jsonSku);
				
				// Name
				String name = crawlName(doc, jsonSku);
				
				// Secondary images
				String secondaryImages = crawlSecondaryImages(internalId);
				
				// Creating the product
				Product product = new Product();
				product.setSeedId(session.getSeedId());
				product.setUrl(session.getUrl());
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
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}
		
		return products;
	}

	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		if ( url.endsWith("/p") || url.contains("/p?attempt=")) return true;
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
			
			if(!name.contains(nameVariation)){
				name = name + " - " + nameVariation;
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

	private String crawlPrimaryImage(JSONObject json) {
		String primaryImage = null;

		if (json.has("image")) {
			String urlImage = json.getString("image");
			primaryImage = modifyImageURL(urlImage);
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(String internalId) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();
	
		String url = "http://www.telhanorte.com.br/produto/sku/" + internalId;
		String stringJsonImages = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, null); //GET request to get secondary images
		
		JSONObject jsonObjectImages;
		try {
			jsonObjectImages = new JSONArray(stringJsonImages).getJSONObject(0);
		} catch (JSONException e) {
			jsonObjectImages = new JSONObject();
		}
		
		if (jsonObjectImages.has("Images")) {
			JSONArray jsonArrayImages = jsonObjectImages.getJSONArray("Images");
			
			if(jsonArrayImages.length() > 1){
				for (int i = 1; i < jsonArrayImages.length(); i++) {				//starts with index 1, because the first image is the primary image
					JSONArray arrayImage = jsonArrayImages.getJSONArray(i);
					JSONObject jsonImage = arrayImage.getJSONObject(0);
					
					if(jsonImage.has("Path")){
						String urlImage = modifyImageURL(jsonImage.getString("Path"));
						secondaryImagesArray.put(urlImage);
					}
					
				}
			}
		}
		
		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}
		
		return secondaryImages;
	}
	
	
	private String modifyImageURL(String url) {
		String[] tokens = url.trim().split("/");
		String dimensionImage = tokens[tokens.length-2]; //to get dimension image and the image id
		
		String[] tokens2 = dimensionImage.split("-"); //to get the image-id
		String dimensionImageFinal = tokens2[0] + "-1000-1000";
		
		String urlReturn = url.replace(dimensionImage, dimensionImageFinal); //The image size is changed
		
		return urlReturn;	
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(".bread-crumb ul li a");

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
		Element descriptionElement = document.select(".especificacao").first();
		Element specElement = document.select(".dimensoes").first();

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
