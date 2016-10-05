package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.core.task.Crawler;
import br.com.lett.crawlernode.util.Logging;

//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import org.json.JSONArray;
//import org.jsoup.nodes.Document;
//import org.jsoup.nodes.Element;
//import org.jsoup.select.Elements;
//
//import br.com.lett.crawlernode.core.models.Product;
//import br.com.lett.crawlernode.core.session.CrawlerSession;
//import br.com.lett.crawlernode.core.task.Crawler;
//import br.com.lett.crawlernode.util.Logging;
//
///************************************************************************************************************************************************************************************
// * Crawling notes (11/07/2016):
// * 
// * 1) For this crawler, we have one url per each sku. There is no page is more than one sku in it.
// *  
// * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
// * 
// * 3) There is no marketplace in this ecommerce by the time this crawler was made.
// * 
// * 4) The sku page identification is done by looking for a specific html element of a sku.
// * 
// * 5) If the sku is unavailable, it's price is not displayed.
// * 
// * 6) There is no internalPid for skus in this ecommerce. The internalPid must be a number that is the same for all
// * the variations of a given sku.
// * 
// * 7) On crawling the descriptions, the pattern observed is that we have 3 tabs. The first one is the description, the second is
// * tech specs and third is for sku insurance. We consider only the first and second.
// * 
// * 8) We have one method for each type of information for a sku (please carry on with this pattern).
// * 
// * Examples:
// * ex1 (available): http://www.climario.com.br/split-inverter/22000-a-30000-btus/ar-condicionado-split-hi-wall-hitachi-inverter-22000-btuh-220v-frio-hitachi.html
// * ex2 (unavailable): http://www.climario.com.br/ar-de-janela/11000-a-19000-btus/ar-condicionado-de-janela-springer-minimaxi-12000-btus-220v-mecanico-frio-springer.html 
// *
// * Optimizations notes:
// * No optimizations.
// *
// ************************************************************************************************************************************************************************************/
//
//public class BrasilClimarioCrawler extends Crawler {
//
//	private final String HOME_PAGE = "http://www.climario.com.br/";
//	private final String MAIN_DOMAIN = "http://www.climario.com.br";
//	
//	public BrasilClimarioCrawler(CrawlerSession session) {
//		super(session);
//	}
//
//	@Override
//	public boolean shouldVisit() {
//		String href = this.session.getUrl().toLowerCase();
//		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
//	}
//
//
//	@Override
//	public List<Product> extractInformation(Document doc) throws Exception {
//		super.extractInformation(doc);
//		List<Product> products = new ArrayList<Product>();
//
//		if ( isProductPage(session.getUrl()) ) {
//
//			Logging.printLogDebug(logger, session, "Product page identified: " + session.getUrl());
//
//			/* ***********************************
//			 * crawling data of only one product *
//			 *************************************/
//
//			// InternalId
//			String internalId = crawlInternalId(doc);
//
//			// Pid
//			String internalPid = crawlInternalPid(doc);
//
//			// Name
//			String name = crawlName(doc);
//
//			// Price
//			Float price = crawlMainPagePrice(doc);
//			
//			// Availability
//			boolean available = crawlAvailability(doc);
//
//			// Categories
//			ArrayList<String> categories = crawlCategories(doc);
//			String category1 = getCategory(categories, 0);
//			String category2 = getCategory(categories, 1);
//			String category3 = getCategory(categories, 2);
//
//			// Primary image
//			String primaryImage = crawlPrimaryImage(doc);
//
//			// Secondary images
//			String secondaryImages = crawlSecondaryImages(doc);
//
//			// Description
//			String description = crawlDescription(doc);
//
//			// Stock
//			Integer stock = null;
//
//			// Marketplace map
//			Map<String, Float> marketplaceMap = crawlMarketplace(doc);
//
//			// Marketplace
//			JSONArray marketplace = assembleMarketplaceFromMap(marketplaceMap);
//
//			// Creating the product
//			Product product = new Product();
//			product.setUrl(this.session.getUrl());
//			product.setInternalId(internalId);
//			product.setInternalPid(internalPid);
//			product.setName(name);
//			product.setPrice(price);
//			product.setAvailable(available);
//			product.setCategory1(category1);
//			product.setCategory2(category2);
//			product.setCategory3(category3);
//			product.setPrimaryImage(primaryImage);
//			product.setSecondaryImages(secondaryImages);
//			product.setDescription(description);
//			product.setStock(stock);
//			product.setMarketplace(marketplace);
//
//			products.add(product);
//
//		} else {
//			Logging.printLogTrace(logger, "Not a product page" + this.session.getUrl());
//		}
//		
//		return products;
//	}
//
//
//
//	/*******************************
//	 * Product page identification *
//	 *******************************/
//
//	private boolean isProductPage(String url) {
//		return url.endsWith("/p");
//	}
//	
//	
//	/*******************
//	 * General methods *
//	 *******************/
//	
//	private String crawlInternalId(Document document) {
//		String internalId = null;
//		Element internalIdElement = document.select("#prod_id").first();
//
//		if (internalIdElement != null) {
//			internalId = internalIdElement.attr("value").toString().trim();			
//		}
//
//		return internalId;
//	}
//
//	private String crawlInternalPid(Document document) {
//		String internalPid = null;
//
//		return internalPid;
//	}
//	
//	private String crawlName(Document document) {
//		String name = null;
//		Element nameElement = document.select(".tituloProdDetalhe span[itemprop=name]").first();
//
//		if (nameElement != null) {
//			name = sanitizeName(nameElement.text());
//		}
//
//		return name;
//	}
//
//	private Float crawlMainPagePrice(Document document) {
//		Float price = null;
//		Element mainPagePriceElement = document.select(".precoPor .val").first();
//
//		if (mainPagePriceElement != null) {
//			price = Float.parseFloat( mainPagePriceElement.text().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
//		}
//
//		return price;
//	}
//	
//	private boolean crawlAvailability(Document document) {
//		Element notifyMeElement = document.select(".produtoIndisponivel").first();
//		if (notifyMeElement != null) return false;
//		return true;
//	}
//
//	private Map<String, Float> crawlMarketplace(Document document) {
//		return new HashMap<String, Float>();
//	}
//	
//	private JSONArray assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
//		return new JSONArray();
//	}
//
//	private String crawlPrimaryImage(Document document) {
//		String primaryImage = null;
//		Element primaryImageElement = document.select(".imgDetalhe a img").first();
//
//		if (primaryImageElement != null) {
//			primaryImage = primaryImageElement.attr("content").trim();
//		}
//
//		return primaryImage;
//	}
//
//	private String crawlSecondaryImages(Document document) {
//		String secondaryImages = null;
//		JSONArray secondaryImagesArray = new JSONArray();
//
//		Elements imagesElement = document.select("#mycarousel li a img");
//
//		for (int i = 1; i < imagesElement.size(); i++) { // starting from index 1, because the first is the primary image
//			String image = MAIN_DOMAIN + imagesElement.get(i).attr("src").trim();
//			secondaryImagesArray.put( modifyImageURL(image) );
//		}
//
//		if (secondaryImagesArray.length() > 0) {
//			secondaryImages = secondaryImagesArray.toString();
//		}
//
//		return secondaryImages;
//	}
//
//	private ArrayList<String> crawlCategories(Document document) {
//		ArrayList<String> categories = new ArrayList<String>();
//		Elements elementCategories = document.select("#breadCrumbDetalhe");
//
//		String[] tokens = elementCategories.text().split(">");		
//		for (int i = 1; i < tokens.length; i++) {
//			categories.add( sanitizeName(tokens[i]) );
//		}		
//
//		return categories;
//	}
//
//	private String getCategory(ArrayList<String> categories, int n) {
//		if (n < categories.size()) {
//			return categories.get(n);
//		}
//
//		return "";
//	}
//
//	private String crawlDescription(Document document) {
//		String description = "";
//		Element descriptionElement = document.select("#abas1").first();
//		Element specElement = document.select("#abas2").first();
//
//		if (descriptionElement != null) description = description + descriptionElement.html();
//		if (specElement != null) description = description + specElement.html();
//
//		return description;
//	}
//	
//	/**************************
//	 * Specific manipulations *
//	 **************************/
//	
//	private String sanitizeName(String name) {
//		return name.replace("'","").replace("’","").trim();
//	}
//	
//	
//	/**
//	 * Modify the imageURL to get a bigger version of the image.
//	 * The logic used in this ecommerce is to modify the first two letters in the number code
//	 * for the image, just before the .jpg. The observed pattern is that 'PP' is for the smallest version,
//	 * and OP is for the biggest.
//	 * 
//	 * @param imageURL the image URL with the main domain already appended
//	 * @return the modified imageURL for the biggest version
//	 */
//	private String modifyImageURL(String imageURL) {
//		return imageURL.replace("__PP", "__OP");
//	}
//
//}

/************************************************************************************************************************************************************************************
 * Crawling notes (04/10/2016):
 * 
 * 1) For this crawler, we have one URL for multiple skus.
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
 * 8) To get the internal_id is necessary to get a json , where internal_id is an attribute " sku ".
 * 
 * Examples:
 * ex1 (available): http://www.climario.com.br/ar-condicionado-de-janela-elgin-21000btuh-220-monofasico-frio-mecanico/p
 * ex2 (unavailable): http://www.climario.com.br/ar-condicionado-split-elgin-high-wall-18k-220v-frior410a/p
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilClimarioCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.climario.com.br/";

	public BrasilClimarioCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}


	@Override
	public List<Product>  extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(doc) ) {
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
				String primaryImage = crawlPrimaryImage(doc);

				// Name
				String name = crawlName(doc, jsonSku);

				// Secondary images
				String secondaryImages = crawlSecondaryImages(doc);

				// Creating the product
				Product product = new Product();

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

	private boolean isProductPage(Document document) {
		if ( document.select(".row-product-info").first() != null ) return true;
		return false;
	}

	/*******************
	 * General methods *
	 *******************/

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

		String mainPageName = null;
		Element nameElement = document.select(".productName").first();
		if (nameElement != null) {
			mainPageName = nameElement.text().trim();
		}

		String skuVariationName = null;
		if (jsonSku.has("skuname")) {
			skuVariationName = jsonSku.getString("skuname");
		}

		if (skuVariationName == null) return mainPageName;

		if (mainPageName != null) {
			name = mainPageName;

			if (name.length() > skuVariationName.length()) {
				name += skuVariationName;
			} else {
				name = skuVariationName;
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

	private String crawlPrimaryImage(Document doc) {
		String primaryImage = null;

		Element image = doc.select(".image-zoom").first();

		if (image != null) {
			primaryImage = image.attr("href");
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document doc) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements images = doc.select("#botaoZoom");

		for (int i = 1; i < images.size(); i++) {				//starts with index 1, because the first image is the primary image
			Element e = images.get(i);

			if(e.hasAttr("zoom")){
				String urlImage = e.attr("zoom");

				if(!urlImage.startsWith("http")){
					urlImage = e.attr("rel");
				}

				secondaryImagesArray.put(urlImage);
			}
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
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
		Element specElement = document.select("#caracteristicas").first();

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