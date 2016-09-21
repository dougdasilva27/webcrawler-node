package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.task.Crawler;
import br.com.lett.crawlernode.core.task.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;

/************************************************************************************************************************************************************************************
 * Crawling notes (19/07/2016):
 * 
 * 1) For this crawler, we have one url for mutiples skus.
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace information in this ecommerce
 *  
 * 4) To get the secondaryImages in category Moda, is acessed a api for crawled
 * 
 * 5) The sku page identification is done simply looking the URL format.
 * 
 * 6) Even if a product is unavailable, its price is not displayed, then price is null.
 * 
 * 7) There is internalPid for skus in this ecommerce. The internalPid is a number that is the same for all
 * the variations of a given sku.
 * 
 * 8) The first image in secondary images is the primary image, sometimes the second image too, then it made ​​a check.
 * 
 * 9) When the sku has variations, the variation name it is added to the name found in the main page. 
 * 
 * 11) The Id from the main Page is the internalPid.
 * 
 * 12) The name of sku is wrong when category is Special
 * 
 * Examples:
 * ex1 (available): http://www.soubarato.com.br/produto/6603120/ar-condicionado-de-janela-springer-duo-7.500-btus-frio-mecanico
 * ex2 (unavailable): http://www.soubarato.com.br/produto/125911511/smartphone-samsung-galaxy-s7-edge-desbloqueado-android-6.0-tela-5-5-32gb-4g-12mp-prata
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilSoubaratoCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.soubarato.com.br/";
	private final String CATEGORY_SPECIAL = "Moda";
	
	public BrasilSoubaratoCrawler(CrawlerSession session) {
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
		
		if( isProductPage(this.session.getUrl()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			/* *********************************************************
			 * crawling data common to both the cases of product page  *
			 ***********************************************************/
			
			// Pid
			String internalPid = this.crawlInternalPid(doc);

			// Name
			String name = this.crawlMainPageName(doc);
			
			// Available
			boolean available = this.crawlAvailability(doc);
			
			// Price
			Float price = this.crawlPrice(doc, available);
			
			// Marketplace map
			Map<String, Float> marketplaceMap = this.crawlMarketplaces(doc);

			// Assemble marketplace from marketplace map
			JSONArray marketplaces =  this.assembleMarketplaceFromMap(marketplaceMap);
			
			// Categories
			ArrayList<String> categories = this.crawlCategories(doc);
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);
			
			// Category Special
			boolean categorySpecial = false;
			
			// Primary image
			String primaryImage = this.crawlPrimaryImage(doc, categorySpecial, null);
			
			// Secondary images
			String secondaryImages = this.crawlSecondaryImages(doc, categorySpecial, internalPid, name, primaryImage);

			// Description
			String description = this.crawlDescription(doc);

			// Estoque
			Integer stock = null;

			
			/* **************************************
			 * crawling data of multiple variations *
			 ****************************************/
			if( hasProductVariations(doc) ) {
				Logging.printLogDebug(logger, session, "Crawling information of more than one product...");

				Elements productVariationElements = this.crawlSkuOptions(doc);
				for(int i = 0; i < productVariationElements.size(); i++) {

					Element sku = productVariationElements.get(i);
					
					// Category Special
					categorySpecial = isCategorySpecial(category1);
					
					// InternalId
					String variationInternalID = sku.attr("value");
					
					// Getting name variation
					String variationName = this.crawlNameForMutipleVariations(name, sku, categorySpecial);

					// Available
					boolean availableVariations = this.crawlAvailabilityForMutipleVariations(doc, available, variationInternalID);
					
					// PrimaryImage
					String primaryImageVariation = this.crawlPrimaryImage( doc, categorySpecial, variationInternalID );
					
					// Secondary Images
					String secondaryImagesVariation = this.crawlSecondaryImages(doc, categorySpecial, internalPid, variationName, primaryImageVariation);

					//Price
					Float priceVariation = this.crawlPriceVariation(availableVariations, price);
					
					Product product = new Product();
					
					product.setUrl(this.session.getUrl());
					product.setInternalId(variationInternalID);
					product.setInternalPid(internalPid);
					product.setName(variationName);
					product.setPrice(priceVariation);
					product.setCategory1(category1);
					product.setCategory2(category2);
					product.setCategory3(category3);
					product.setPrimaryImage(primaryImageVariation);
					product.setSecondaryImages(secondaryImagesVariation);
					product.setDescription(description);
					product.setStock(stock);
					product.setMarketplace(marketplaces);
					product.setAvailable(availableVariations);

					products.add(product);
					
				}
			}


			/* *******************************************
			 * crawling data of only one product in page *
			 *********************************************/
			else {
				
				// InternalId
				String internalID = this.crawlInternalIDSingleProduct(doc);
				
				Product product = new Product();
				
				product.setUrl(this.session.getUrl());
				product.setInternalId(internalID);
				product.setInternalPid(internalPid);
				product.setName(name);
				product.setPrice(price);
				product.setCategory1(category1);
				product.setCategory2(category2);
				product.setCategory3(category3);
				product.setPrimaryImage(primaryImage);
				product.setSecondaryImages(secondaryImages);
				product.setDescription(description);
				product.setStock(stock);
				product.setMarketplace(marketplaces);
				product.setAvailable(available);

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

		if (url.startsWith(HOME_PAGE + "produto/") && !url.contains("?")) return true;
		return false;
	}


	/***********************************
	 * Category special identification *
	 ***********************************/
	private boolean isCategorySpecial(String category){
		
		if(category.equals(CATEGORY_SPECIAL))  return true;
		
		return false;
	}
	

	/************************************
	 * Multiple products identification *
	 ************************************/

	private boolean hasProductVariations(Document document) {
		Elements skuChooser = document.select(".pure-select select option[data-stock]");
		
		if (skuChooser.size() > 0) {
			return true;
		} else {
			return false;
		}
	}

	private String crawlInternalPid(Document document) {
		String internalId = null;
		Element elementInternalId = document.select("input[name=codProdFusion]").first();
		if (elementInternalId != null) {
			internalId = elementInternalId.attr("value").trim();
		}

		return internalId;
	}

	
	/*******************************
	 * Single product page methods *
	 *******************************/
	
	private String crawlInternalIDSingleProduct(Document document) {
		String internalIDMainPage = null;
		Element elementDataSku = document.select(".p-code").first();
		
		if(elementDataSku != null) {
			internalIDMainPage = elementDataSku.text().replaceAll("[^0-9]", "").trim();
		}
		

		return internalIDMainPage;
	}
	

	/*********************************
	 * Multiple product page methods *
	 *********************************/


	private Elements crawlSkuOptions(Document document) {
		Elements skuOptions = document.select(".pure-select select option[data-stock]");

		return skuOptions;
	}

	
	private Float crawlPriceVariation(boolean available, Float price){
		Float priceVariation = null;
		
		if(available) priceVariation = price;
		
		return priceVariation;
	}
	
	private JSONObject getInformationsFromApi(String internalPid){
		String url = "http://www.soubarato.com.br/produtosModa/" + internalPid;
		
		JSONObject jsonReturn = DataFetcher.fetchJSONObject(DataFetcher.GET_REQUEST, session, url, null, null);
		
		return jsonReturn;
	}
	
	private boolean crawlAvailabilityForMutipleVariations(Document doc, boolean availableFirst, String internalId) {
		if(availableFirst){
			Element availabilityElement = doc.select(".pure-select select option[value=\"" + internalId + "\"]").first();
			
			if(availabilityElement.attr("data-stock").trim().equals("true")){
				return true;
			}
		
			return false;
		} else {
			return false;
		}
	}	
		
	
	private String crawlNameForMutipleVariations(String nameMainPage, Element variation, boolean isCategorySpecial) {
		String name = null;
		
		if(!isCategorySpecial){
			name = nameMainPage + " - " + variation.text().trim();
		} else {
			String nameVariation = variation.text();
			String number = nameVariation.replaceAll("[^0-9- ]", "").trim();
			
			String[] tokens = nameVariation.replaceAll(number, "").trim().split(" ");
			
			Map<Integer, String> colors = new HashMap<Integer, String>();
			
			for(int i = 0; i < tokens.length; i++){
				
				if(!colors.containsValue(tokens[i])){
					colors.put(i, tokens[i]);
				}
			}
			
			String colorsName = "";
			
			Map<Integer, String> map = new TreeMap<Integer, String>(colors);
			
			for(String color: map.values()){
				colorsName = colorsName + color + " ";
			}
			
			name = nameMainPage + " - " + number + " - " + colorsName.trim();
		} 
		return name;
	}
	
	/*******************
	 * General methods *
	 *******************/

	private Float crawlPrice(Document doc, boolean available) {
		Float price = null;	
		Element priceElement = doc.select(".p-price .value").first();
		
		if(priceElement != null && available) price = Float.parseFloat(priceElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));;
				
		return price;
	}
	
	private boolean crawlAvailability(Document doc) {
		boolean available = false;
		Elements avaibilityElements = doc.select("span.p-label.static");
		
		for(Element e : avaibilityElements){
			String symbol = e.attr("data-tag-symbol");
			
			if(symbol.equals("N")) return true; //Symbol N means new product
		}
		
		return available;
	}
	
	private String crawlPrimaryImage(Document document, boolean isCategorySpecial, String internalId) {
		
		if(!isCategorySpecial){
			String primaryImage = null;
			
			Element primaryImageElements = document.select(".p-link .p-image").first();
			
			if(primaryImageElements.hasAttr("data-image-sz")){
				primaryImage = primaryImageElements.attr("data-image-sz");
				if(primaryImage.equals("#")){
					primaryImage = primaryImageElements.attr("data-process-image");
				}
			} else {
				primaryImage = primaryImageElements.attr("data-process-image");
			}
			
			return primaryImage;
		} else {
			String primaryImage = null;
			Element primaryElement = document.select(".pure-select select option[value=\"" + internalId + "\"]").first();
			
			if(primaryElement != null){
				String urlForImage = primaryElement.attr("data-sku-image");
				
				String token = urlForImage.replaceAll("http://img.soubarato.com.br/", "http://images.soubarato.io/");
				primaryImage = token.replaceAll("P", "SZ");
			}
			

			return primaryImage;
		}
	}
	
	private String crawlSecondaryImages(Document document, boolean isCategorySpecial, String internalPid, String name, String primaryImage) {
		String secondaryImages = null;
		
		if(!isCategorySpecial){
			JSONArray secondaryImagesArray = new JSONArray();
			Elements elementFotoSecundaria = document.select(".p-link .p-image");
	
			if (elementFotoSecundaria.size()>1) {
				for(int i = 1; i < elementFotoSecundaria.size(); i++) { //starts with index 1 because de primary image is the first image
					Element e = elementFotoSecundaria.get(i);
					String secondaryImagesTemp;
					
					if(e.hasAttr("data-image-sz")){
						secondaryImagesTemp = e.attr("data-image-sz");
						if(secondaryImagesTemp.equals("#")){
							secondaryImagesTemp = e.attr("data-process-image");
						}
					} else {
						secondaryImagesTemp = e.attr("data-process-image");
					}
					
					if(!primaryImage.equals(secondaryImagesTemp)) secondaryImagesArray.put(secondaryImagesTemp); // identify if the image is the primary image
				}
			}
				
			if (secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}
		} else {
			secondaryImages = crawlSecondaryImagesForCategorySpecial(internalPid, name);
		}

		return secondaryImages;
	}	

	private String crawlSecondaryImagesForCategorySpecial(String internalPid, String name){
		String secondaryImages = null;
		JSONArray secondaryImagesArrayReturn = new JSONArray();
		
		JSONObject secondaryJson = getInformationsFromApi(internalPid);
		JSONObject imagesJson = new JSONObject();
		
		if(secondaryJson.has("pictures")){
			JSONArray picturesArray = secondaryJson.getJSONArray("pictures");
			
			for(int i = 0; i < picturesArray.length(); i++){
				JSONObject imageTemp = picturesArray.getJSONObject(i);
				
				if(imageTemp.has("model")){
					if(name.contains(imageTemp.getString("model"))){
						imagesJson = imageTemp.getJSONObject("images");
						
					}
				}
			}
			
			JSONArray secondaryImagesArray = new JSONArray();
			
			if(imagesJson.has("SZ")){
				JSONArray szArray = imagesJson.getJSONArray("SZ");
				if(imagesJson.has("GG")){
					JSONArray ggArray = imagesJson.getJSONArray("GG");
					
					if(ggArray.length() > szArray.length()){
						secondaryImagesArray = ggArray;
					} else {
						secondaryImagesArray = szArray;
					}
					
				}else {
					secondaryImagesArray = szArray;
				}
				
			} else if(imagesJson.has("GG")){
				secondaryImagesArray = imagesJson.getJSONArray("GG");
			}
			
			for(int i = 1; i < secondaryImagesArray.length(); i++){ //starts with index 1 because the fisrt image is the primary image
				secondaryImagesArrayReturn.put(secondaryImagesArray.getString(i));	
			}
		}
		
		if (secondaryImagesArrayReturn.length() > 0) {
			secondaryImages = secondaryImagesArrayReturn.toString();
		}
		
		return secondaryImages;
	}
	
	private String crawlMainPageName(Document document) {
		String name = null;
		Element elementName = document.select(".p-name").first();
		if(elementName != null) {
			name = elementName.ownText().replace("'","").replace("’","").trim();
		}
		return name;
	}

	private ArrayList<String> crawlCategories(Document document) {
		Elements elementCategories = document.select(".breadcrumb a");
		ArrayList<String> categories = new ArrayList<String>();

		for(Element e : elementCategories) {
			categories.add(e.text().trim());
		}

		return categories;
	}

	private String getCategory(ArrayList<String> categories, int n) {
		if (n < categories.size()) {
			return categories.get(n);
		}

		return "";
	}


	private JSONArray assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		JSONArray marketplace = new JSONArray();
		
		return marketplace;
	}	
	
	private Map<String, Float> crawlMarketplaces(Document doc) {
		Map<String, Float>  marketplace = new HashMap<String, Float> ();

		return marketplace;
	}

	private String crawlDescription(Document document) {
		String description = "";
		Element elementProductDetails = document.select(".products-info-container#basicinfotoggle").first();
		Element elementProductTec = document.select(".products-info-container#informacoes-tecnicas-0").first();
		
		if(elementProductDetails != null) 	description = description + elementProductDetails.html();
		if(elementProductTec != null) 		description = description + elementProductTec.html();
		
		return description;
	}
}
