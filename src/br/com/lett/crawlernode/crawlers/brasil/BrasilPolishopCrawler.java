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

import br.com.lett.crawlernode.kernel.CrawlerSession;
import br.com.lett.crawlernode.kernel.fetcher.DataFetcher;
import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.Crawler;
import br.com.lett.crawlernode.util.Logging;

/************************************************************************************************************************************************************************************
 * Crawling notes (19/07/2016):
 * 
 * 1) For this crawler, we have one URL for multiple skus.
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking for an specific url element.
 * 
 * 5) If the sku is unavailable, it's price is not displayed.
 * 
 * 6) The price of sku, found in json script, is wrong when the same is unavailable, then it is not crawled.
 * 
 * 7) There is internalPid for skus in this ecommerce. The internalPid is a number that is the same for all
 * the variations of a given sku.
 * 
 * 7) The primary image is the first image on the secondary images when are colors variations.
 * 
 * 8) Variations of skus are not crawled if the variation is unavailable because it is not displayed for the user,
 * except if there is a html element voltage, because variations of voltage are displayed for the user even though unavailable.
 * 
 * 9) The url of the images for colors are changed to bigger dimensions manually.
 * 
 * 10) The secondary images for colors are get in api "http://www.polishop.com.br/produto/sku/" + internalId
 * 
 * 11) To get normal images, is get images that has element "-main-", primary images ends with "01.jpg".
 * 
 * 12) has "tm"² in names.
 * 
 * 13) Some page of products are html selector diferents for get images.
 * 
 * Examples:
 * ex1 (available): http://www.polishop.com.br/fritadeira-airfryer-avance-xl-philips-walita/p
 * ex2 (unavailable): http://www.polishop.com.br/ar-condicionado-pinguino-maxxi-cooling-exclusive-delonghi/p
 * ex3 (with colors): http://www.polishop.com.br/batom-hidratante-fps-20-be-emotion/p
 * ex4 (with colors and size): http://www.polishop.com.br/calca-capri-craveworthy-legging/p
 * ex5 (sepcial product): http://www.polishop.com.br/nutri-ninja-auto-iq/p
 *
 *
 ************************************************************************************************************************************************************************************/

public class BrasilPolishopCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.polishop.com.br/";

	public BrasilPolishopCrawler(CrawlerSession session) {
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

		if ( isProductPage(this.session.getUrl()) ) {

			
			/* **************************************************************
			 * crawling data of multiple variations and the single products *
			 ****************************************************************/
		
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());
			
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
			
			// Primary image
			String primaryImage = this.crawlPrimaryImageMainPage(doc);
			
			// Secondary images
			String secondaryImages = this.crawlSecondaryImagesMainPage(doc, primaryImage);
			
			// sku data in Json
			JSONObject jsonSkus = crawlSkuJsonArray(doc);
			
			// Name
			String name = this.crawlNameMainPage(jsonSkus);
			
			// HasColorVariation
			boolean hasColors = this.hasColor(jsonSkus);
			
			// sku data in jsonArray
			JSONArray arraySkus = jsonSkus.getJSONArray("skus");			
			
			for(int i = 0; i < arraySkus.length(); i++){
				JSONObject jsonSku = arraySkus.getJSONObject(i);
				
				// Availability
				boolean available = crawlAvailability(jsonSku);

				// Name
				String namevariation = crawlName(name, jsonSku, arraySkus);
				
				// InternalId 
				String internalId = crawlInternalId(jsonSku);
				
				// Price
				Float price = crawlMainPagePrice(jsonSku, available);
				
				// Json Images
				JSONObject jsonimages = this.fetchImagesFromApi(internalId, hasColors);
				
				// Primary image
				String primaryImageVariation = crawlPrimaryImageForColors(jsonimages, hasColors, primaryImage);
				
				// Secondary images
				String secondaryImagesVariation = crawlSecondaryImagesForColors(jsonimages, hasColors, secondaryImages);
				
				// Creating the product
				Product product = new Product();
				product.setSeedId(this.session.getSeedId());
				product.setUrl(this.session.getUrl());
				product.setInternalId(internalId);
				product.setInternalPid(internalPid);
				product.setName(namevariation);
				product.setPrice(price);
				product.setAvailable(available);
				product.setCategory1(category1);
				product.setCategory2(category2);
				product.setCategory3(category3);
				product.setPrimaryImage(primaryImageVariation);
				product.setSecondaryImages(secondaryImagesVariation);
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
		Element internalPidElement = document.select("meta[itemprop=productID]").first();
		
		if (internalPidElement != null) {
			internalPid = internalPidElement.attr("content").toString().trim();
		}
		
		return internalPid;
	}
	
	private String crawlNameMainPage(JSONObject jsonSkus) {
		String name = null;

		if(jsonSkus.has("name")){
			name = jsonSkus.getString("name").trim();
		}
		
		return name;
	}
	
	private String crawlName(String nameMainPage, JSONObject jsonSku, JSONArray arraySkus) {
		String name = nameMainPage;
			
		if(jsonSku.has("skuname") && arraySkus.length() > 1){
			name = name + " " + jsonSku.getString("skuname").replace("'","").replace("’","").trim();
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

	private String crawlPrimaryImageMainPage(Document doc) {
		String primaryImage = null;
		Elements images = doc.select(".thumbs li a");
		
		if(images.size() > 0){
			
			for(Element e: images){
				if(e.hasAttr("rel") && !e.attr("rel").isEmpty()){
					String urlImage = e.attr("rel").trim();
					
					if(urlImage.endsWith("01.jpg") && urlImage.contains("-main-") && !urlImage.contains("selo")){
						primaryImage = urlImage;
						break;
					}
				} else {
					Element img = e.select("img").first();
					String urlImage = img.attr("src").trim();
					
					if(urlImage.endsWith("01.jpg") && urlImage.contains("-main-") && !urlImage.contains("selo")){
						primaryImage = urlImage;
						break;
					}
				}
			}
		} else {
			Element productVerySpecial = doc.select(".hotsite-prospero-splash-prod").first();
			
			if(productVerySpecial != null){
				primaryImage = "http:" + productVerySpecial.attr("src");
				
			} else {
				Element productSpecial = doc.select(".hotsite-image-offset img").first();
				
				if(productSpecial != null){
					primaryImage = "http:" + productSpecial.attr("src");
				}
			}
		}
		
		return primaryImage;
	}
	
	private String crawlSecondaryImagesMainPage(Document doc, String primaryImage) {
		String secondaryImages = null;	
		JSONArray secondaryImagesArray = new JSONArray();
		Elements images = doc.select(".thumbs li a");
		
		if(primaryImage != null){
			if(images.size() > 0){
				for(Element e: images){
					if(e.hasAttr("rel") && !e.attr("rel").isEmpty()){
						String urlImage = e.attr("rel").trim();
						
						if(urlImage.contains("-main-") && !primaryImage.equals(urlImage)){
							secondaryImagesArray.put(urlImage);
						}
					} else {
						Element img = e.select("img").first();
						String urlImage = img.attr("src").trim();
						
						if(urlImage.contains("-main-") && !primaryImage.equals(urlImage)){
							secondaryImagesArray.put(urlImage);
						}
					}
				}
				
			} else {
				Elements productSpecial = doc.select(".hotsite-image-offset img");
				
				for(int i = 0; i < productSpecial.size(); i++) { // start with index 1 because the first image is the primary image
					Element e = productSpecial.get(i);
					String urlImage = "http:" + e.attr("src");
					
					if(!primaryImage.equals(urlImage)) secondaryImagesArray.put(urlImage);
				}
			}
			
			if(secondaryImagesArray.length() > 0){
				secondaryImages = secondaryImagesArray.toString();
			}
		}
		
		return secondaryImages;
	}
	
	private boolean hasColor(JSONObject jsonSkus){
		
		if(jsonSkus.has("dimensionsMap")){
			JSONObject types = jsonSkus.getJSONObject("dimensionsMap");
			
			if(types.has("COR")){
				return true;
			}
		}
		
		return false;
		
	}
	
	private String modifyImageURL(String url) {
		String[] tokens = url.trim().split("/");
		String dimensionImage = tokens[tokens.length-2];
		
		String dimensionImageFinal = getIdColorFromImage(url) + "-1000-563";
		
		String urlReturn = url.replace(dimensionImage, dimensionImageFinal); //The image size is changed
		
		return urlReturn;	
	}

	private String getIdColorFromImage(String urlImage) {
		String[] tokens = urlImage.trim().split("/");
		String dimensionImage = tokens[tokens.length-2]; //to get dimension image and the image id
		
		String[] tokens2 = dimensionImage.split("-"); //to get the image-id
		String idImage = tokens2[0];
		
		return idImage;
	}
	
	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(".bread-crumb li a");

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
		Element descriptionElement = document.select(".productDescription").first();
		Element specElement = document.select(".product-specs").first();

		if (descriptionElement != null) description = description + descriptionElement.html();
		if (specElement != null) description = description + specElement.html();

		if(description.equals("")){			
			Elements productSpecial = document.select(".centered-content[layout]");
			
			for(Element e : productSpecial){
				if(e != null) description = description + e.html();
			}
			
			Element specSpecial = document.select("tech-specs").first();
			if (specSpecial != null) description = description + specSpecial.html();
		}
		
		return description;
	}
	
	/**
	 * Get the script having a json with the availability information
	 * @return
	 */
	private JSONObject crawlSkuJsonArray(Document document) {
		Elements scriptTags = document.getElementsByTag("script");
		JSONObject skuJson = null;
		
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
		
		
		return skuJson;
	}
	
	/**************** *
	 * Colors Methods *
	 * ************** */
	
	private String crawlPrimaryImageForColors(JSONObject json, boolean hasColor, String primaryImageMainPage) {
		String primaryImage = null;
		
		if(hasColor){
			if (json.has("primaryImage")) {
				primaryImage = json.getString("primaryImage");
			}
		} else {
			primaryImage = primaryImageMainPage;
		}

		return primaryImage;
	}

	private String crawlSecondaryImagesForColors(JSONObject jsonImages, boolean hasColor, String secondaryImagesMainPage) {
		String secondaryImages = null;	
		
		if(hasColor){
			if(jsonImages.has("secondaryImages")){
				secondaryImages = jsonImages.getJSONArray("secondaryImages").toString();
			}
		} else {
			secondaryImages = secondaryImagesMainPage;
		}
		
		return secondaryImages;
	}
	
	private JSONObject fetchImagesFromApi(String internalId, boolean hasColor){
		JSONObject jsonImages = new JSONObject();
		JSONArray secondaryImagesArray = new JSONArray();
		String primaryImage = null;
		
		if(hasColor){
			String url = "http://www.polishop.com.br/produto/sku/" + internalId;
			String stringJsonImages = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, null); //GET request to get secondary images
			
			JSONObject jsonObjectImages;
			try {
				jsonObjectImages = new JSONArray(stringJsonImages).getJSONObject(0);
			} catch (JSONException e) {
				jsonObjectImages = new JSONObject();
				e.printStackTrace();
			}
			
			JSONArray rightImages = new JSONArray();
			
			if (jsonObjectImages.has("Images")) {
				JSONArray jsonArrayImages = jsonObjectImages.getJSONArray("Images");
				
				for (int i = 0; i < jsonArrayImages.length(); i++) { 
					JSONArray arrayImage = jsonArrayImages.getJSONArray(i);
					JSONObject jsonImage = arrayImage.getJSONObject(0);
					
					if(jsonImage.has("Name")){
						if(jsonImage.getString("Name").trim().length() < 1){
							if(jsonImage.has("Path")){
								String urlImage = modifyImageURL(jsonImage.getString("Path")).trim();
								rightImages.put(urlImage);
							}
						}
					}
				}
			}
			
			if(rightImages.length() > 0){
				primaryImage = rightImages.getString(0);
				
				for(int i = 1; i < rightImages.length(); i++){ // start with index 1 because the first image is the primary image
					secondaryImagesArray.put(rightImages.getString(i));
				}
			}
			
			if(primaryImage != null) jsonImages.put("primaryImage", primaryImage);
			if(secondaryImagesArray.length() > 0) jsonImages.put("secondaryImages", secondaryImagesArray);
		}
		
		return jsonImages;
		
	}
	
	
}
