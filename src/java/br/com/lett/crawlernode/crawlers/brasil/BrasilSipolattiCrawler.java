package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.core.task.Crawler;
import br.com.lett.crawlernode.util.Logging;


/************************************************************************************************************************************************************************************
 * Crawling notes (27/07/2016):
 * 
 * 1) For this crawler, we have one url for mutiples skus.
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace information in this ecommerce
 *  
 * 4) To verify that this product has variation, we use an api. We also use this API to get the name and the variation code.
 * 
 * 5) If the sku has variations, we must use another API to get price, availability and all images.
 * 
 * 6) All the information gathered from the API is inserted on a JSONObject to extract informations.
 * 
 * 7) If the sku is unavailable, the price is not displayed.
 * 
 * 8) In the API object response, the keys ImagemAmpliadaFoto and ProdutoImagem are primaryImages, the first one is the biggest.
 * 
 * 9) In the API object response, the keys [name=liImgDetalhe1], [name=liImgDetalhe2] , [name=liImgDetalhe3] ... are secondaryImages.
 * 
 * 10) If the first image contains ZoomImage, the secondaryImages also contains the zoom image version, then we must modify the image URL, changing "Detalhes" for "Ampliada".
 * 
 * 11) InternalID from variations is the internalIDMainPage + "-" + idColor, because we do not have a unique internalId for each variation.
 * 
 * 12) We used a POST request from DataFetcher, passing the headers as parameters to. The corresponding method on DataFetcher,
 * was first created to be used in this crawler. But can be used in any case.
 * 
 * Examples:
 * ex1 (available): http://www.sipolatti.com.br/suporte-para-tvs-elg-fixo-genius-tvs-de-14-a-84-824.aspx/p
 * ex2 (unavailable): http://www.sipolatti.com.br/suporte-para-tvs-elg-articulado-a02v2-s-15-a-suporta-ate-20-kg-826.aspx/p
 * ex3 (With colors): http://www.sipolatti.com.br/mesa-escrivaninha-para-computador-c18-dalla-costa-909.aspx/p
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilSipolattiCrawler extends Crawler {

	private final String HOME_PAGE 				= "http://www.sipolatti.com.br/";
	private final String CONTENT_TYPE 			= "text/plain";
	private final String URL_API 				= "http://www.sipolatti.com.br/ajaxpro/IKCLojaMaster.detalhes,Sipolatti.ashx";
	private final String VARIATIONS_AJAX_METHOD = "CarregaSKU";
	private final String SKU_AJAX_METHOD 		= "DisponibilidadeSKU";
	private final String VARIATION_NAME_PAYLOAD = "ColorCode";
	
	public BrasilSipolattiCrawler(CrawlerSession session) {
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

		if( isProductPage(this.session.getUrl(), doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());


			/* *********************************************************
			 * crawling data common to both the cases of product page  *
			 ***********************************************************/

			// Pid
			String internalPid = this.crawlInternalPid(doc);

			// Name
			String name = this.crawlName(doc);

			// InternalId
			String internalIDMainPage = this.crawlInternalId(doc);

			// Estoque
			Integer stock = null;

			// Marketplace map
			Map<String, Float> marketplaceMap = this.crawlMarketplaces(doc);

			// Assemble marketplace from marketplace map
			JSONArray marketplaces =  this.assembleMarketplaceFromMap(marketplaceMap);

			// Categories
			ArrayList<String> categories = this.crawlCategories(doc);
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);

			// Description
			String description = this.crawlDescription(doc);

			// Variations
			Elements productVariationElements = this.crawlSkuOptions(internalIDMainPage, this.session.getUrl());

			if(productVariationElements.size() > 1){
				
				Logging.printLogDebug(logger, session, "Crawling information of more than one product...");
				
				/*
				 * Multiple variations
				 */
				for(int i = 0; i < productVariationElements.size(); i++) {
					
					Element sku = productVariationElements.get(i);
					
					// idVariation
					String[] tokens = sku.attr("href").split(",");
					String idVariation = tokens[tokens.length-3];
					
					// InternalId and images
					JSONObject skuInformation = this.crawlSkuInformations(idVariation, internalIDMainPage, this.session.getUrl());
					
					// Varitation name
					String variationName = sku.select("img").attr("alt").trim().toLowerCase();
					
					// Name
					String nameVariation = this.crawlNameVariation(variationName, name);
					
					// InternalId
					String internalIdVariation = this.crawlInternalIdVariation(internalIDMainPage, idVariation);
					
					// PrimaryImage
					String primaryImageVariation = this.crawlPrimaryImageVariation(skuInformation);
					
					// SecondaryImage
					String secondaryImagesVariation = this.crawlSecondaryImagesVariation(skuInformation);
					
					// Available
					boolean availableVariation = crawlAvailabilityVariation(skuInformation);
					
					// Price
					Float priceVariation = crawlPriceVariation(skuInformation, availableVariation);
					
					Product product = new Product();
					
					product.setUrl(this.session.getUrl());
					product.setInternalId(internalIdVariation);
					product.setInternalPid(internalPid);
					product.setName(nameVariation);
					product.setPrice(priceVariation);
					product.setCategory1(category1);
					product.setCategory2(category2);
					product.setCategory3(category3);
					product.setPrimaryImage(primaryImageVariation);
					product.setSecondaryImages(secondaryImagesVariation);
					product.setDescription(description);
					product.setStock(stock);
					product.setMarketplace(marketplaces);
					product.setAvailable(availableVariation);
	
					products.add(product);
	
				}			
				
			} 
			
			/*
			 * Single product
			 */
			else {
				
				// PrimaryImage
				String primaryImage= this.crawlPrimaryImage(doc);

				// Secondary Images
				String secondaryImages= this.crawlSecondaryImages(doc, primaryImage);
				
				// Price
				Float price = this.crawlPrice(doc);
				
				// Available
				boolean available = this.crawlAvailability(doc);
				
				Product product = new Product();
				
				product.setUrl(this.session.getUrl());
				product.setInternalId(internalIDMainPage);
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

	private boolean isProductPage(String url, Document doc) {
		Element producElement = doc.select("#info-product").first();

		if (producElement != null && (url.endsWith("/p") || (url.contains("/p?attempt=")))) return true;
		return false;
	}


	/*********************************
	 * Multiple product page methods *
	 *********************************/

	private Float crawlPriceVariation(JSONObject jsonSku, boolean availbale){
		Float price = null;
		
		if(jsonSku.has("price") && availbale) price = Float.parseFloat(jsonSku.getString("price"));
		
		return price;
	}
	
	private boolean crawlAvailabilityVariation(JSONObject skuInformation) {		
		if(skuInformation.has("available")) return skuInformation.getBoolean("available");

		return false;
	}
	
	
	private String crawlInternalIdVariation(String internalIDMainPage, String idVariation){
		String internalID = null;

		internalID = internalIDMainPage + "-" + idVariation.replaceAll(" ", "");

		return internalID;
	}
	
	private String crawlPrimaryImageVariation(JSONObject json) {
		String primaryImage = null;
		
		if(json.has("primaryImage")) primaryImage = json.getString("primaryImage");
		
		return primaryImage;
	}
	
	private String crawlNameVariation(String variationName, String name) {
		String nameVariation = null;
		
		nameVariation = name + " " + variationName.trim();
		
		return nameVariation;
	}

	private String crawlSecondaryImagesVariation(JSONObject json) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		if(json.has("secondaryImages")){
			secondaryImagesArray = json.getJSONArray("secondaryImages");
		}

		if(secondaryImagesArray.length() > 0){
			secondaryImages = secondaryImagesArray.toString();
		}
		
		return secondaryImages;
	}	

	
	/**
	 * fetch json from api
	 * @param idColor
	 * @param internalIdMainPage
	 * @param urlProduct
	 * @return
	 */
	private JSONObject fetchJSONFromApi(String urlProduct, String payload, String method){
		
		Map<String,String> headers = new HashMap<>();
		
		headers.put("Content-Type", CONTENT_TYPE);
		headers.put("Referer", urlProduct);
		headers.put("X-AjaxPro-Method", method);
		
		String response = DataFetcher.fetchPagePOSTWithHeaders(URL_API, session, payload, null, 1, headers);
		
		return new JSONObject(response);
	}
	
	/**
	 * Get all variations of sku from json
	 * @param internalIdMainPage
	 * @param urlProduct
	 * @return
	 */
	private Elements crawlSkuOptions(String internalIdMainPage, String urlProduct) {
		Elements skuOptions = null;
		String payload = "{\"ProdutoCodigo\": \""+ internalIdMainPage +"\", \""+ VARIATION_NAME_PAYLOAD +"\": \"0\"}";
		
		JSONObject colorsJson = fetchJSONFromApi(urlProduct, payload, VARIATIONS_AJAX_METHOD);
		
		if(colorsJson.has("value")){
			String htmlColors = (colorsJson.getJSONArray("value").getString(0)).replaceAll("\t", "");
			
			Document doc = Jsoup.parse(htmlColors);
			
			skuOptions = doc.select("li > a");
		}
		
		return skuOptions;
	}
	
	/**
	 * Get informations of sku from json
	 * @param idVariation
	 * @param internalIdMainPage
	 * @param urlProduct
	 * @return
	 */
	private JSONObject crawlSkuInformations(String idVariation, String internalIdMainPage, String urlProduct) {
		JSONObject returnJson = new JSONObject();
		
		String payload = "{\"ProdutoCodigo\": \""+ internalIdMainPage +"\", \"CarValorCodigo1\": \""+ idVariation +"\", "
				+ "\"CarValorCodigo2\": \"0\", \"CarValorCodigo3\": \"0\", "
				+ "\"CarValorCodigo4\": \"0\", \"CarValorCodigo5\": \"0\"}";
		
		JSONObject jsonSku = fetchJSONFromApi(urlProduct, payload, SKU_AJAX_METHOD);
				
		if(jsonSku.has("value")){
			JSONArray valueArray = jsonSku.getJSONArray("value");
			int numberProduct = valueArray.getInt(valueArray.length()-1);
			String price = getPriceFromJSON(valueArray);
			boolean available = false;
			
			if(numberProduct != 0){
				available = true;
			}
			
			Map<String, String> imagesMap = new HashMap<String,String>();
			JSONArray imagesArray = valueArray.getJSONArray(1);
			
			for(int i = 0; i < imagesArray.length(); i++){
				String temp = imagesArray.getString(i);
				
				if(temp.startsWith("http://www.sipolatti.com.br/Imagens/produtos/")){
					if(i < imagesArray.length()-1){
						imagesMap.put(imagesArray.getString(i+1), temp);
					}
				}
			}
			
			String primaryImage = null;
			
			if(imagesMap.containsKey("ImagemAmpliadaFoto")){
				primaryImage = imagesMap.get("ImagemAmpliadaFoto");
			} else if(imagesMap.containsKey("ProdutoImagem")) {
				primaryImage = imagesMap.get("ProdutoImagem");
			}
			
			JSONArray secondaryImagesArray = new JSONArray();
			
			for(String key : imagesMap.keySet()){
				if(key.startsWith("[name=liImgDetalhe")){
					if(primaryImage.contains("Ampliada")){
						secondaryImagesArray.put(imagesMap.get(key).replaceAll("Detalhes", "Ampliada"));
					} else {
						secondaryImagesArray.put(imagesMap.get(key));
					}
				}
			}
			
			returnJson.put("available", available);
			if(price != null)						returnJson.put("price", price);
			if(primaryImage != null)				returnJson.put("primaryImage", primaryImage);
			if(secondaryImagesArray.length() > 0) 	returnJson.put("secondaryImages", secondaryImagesArray);
			
		}
		
		return returnJson;
	}
	
	/**
	 * get price from json picked in api
	 * @param skuArray
	 * @return
	 */
	private String getPriceFromJSON(JSONArray skuArray){
		String price = null;
		
		JSONArray priceArray = skuArray.getJSONArray(0);
		
		if(!priceArray.getString(0).contains("Indisponível")){
			
			for(int i = 0; i < priceArray.length(); i++){
				String temp = priceArray.getString(i);
				
				if(temp.startsWith("<em>por")){
					price = temp.replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim();
				}
			}
		}
		
		return price;
	}
	
	
	/*******************
	 * General methods *
	 *******************/

	private String crawlInternalPid(Document document) {
		String internalPid = null;
		Element elementInternalId = document.select("#ProdutoCodigoInterno").first();
		if (elementInternalId != null) {
			internalPid = elementInternalId.attr("value").trim();
		}

		return internalPid;
	}
	
	private Float crawlPrice(Document doc) {
		Float price = null;	
		Element priceElement = doc.select("#lblPrecos #lblPrecoPor strong").first();

		if( priceElement != null ){
			price = Float.parseFloat(priceElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
		}

		return price;
	}

	private String crawlInternalId(Document doc){
		String internalID = null;
		Element internalIdElement = doc.select("#ProdutoCodigo").first();

		if(internalIdElement != null){
			internalID = internalIdElement.attr("value").trim();
		}

		return internalID;
	}

	private boolean crawlAvailability(Document doc) {
		Element availableElement = doc.select("#lblPrecos #lblPrecoPor strong").first();
		
		if(availableElement != null) return true;

		return false;
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElements = document.select("#big_photo_container a").first();

		if(primaryImageElements != null){
			primaryImage = primaryImageElements.attr("href");
		} else {
			primaryImageElements = document.select("#big_photo_container img").first();
			if(primaryImageElements != null){
				primaryImage = primaryImageElements.attr("src");
			}
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document, String primaryImage) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();
		
		Elements elementFotoSecundaria = document.select("ul.thumbs li a");

		if (elementFotoSecundaria.size()>1) {
			for(int i = 0; i < elementFotoSecundaria.size(); i++) { 
				Element e = elementFotoSecundaria.get(i);
				String secondaryImagesTemp = e.attr("href");

				if(!primaryImage.equals(secondaryImagesTemp) && !secondaryImagesTemp.equals("#")) { // identify if the image is the primary image
					secondaryImagesArray.put(secondaryImagesTemp); 
				} else {
					Element x = e.select("img").first();
					
					if(!x.attr("src").isEmpty()){
						secondaryImagesArray.put(x.attr("src").replaceAll("Detalhes", "Ampliada"));
					}
				}

			}
		}

		
		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}	

	private String crawlName(Document doc) {
		String name = null;
		Element elementName = doc.select(".head-product .name").first();
		if(elementName != null) {
			name = elementName.text().replace("'","").replace("’","").trim();
		}
		return name;
	}

	private ArrayList<String> crawlCategories(Document document) {
		Elements elementCategories = document.select("#breadcrumbs span a span");
		ArrayList<String> categories = new ArrayList<String>();

		for(int i = 1; i < elementCategories.size(); i++) { // starts with index 1 because the first category is home page
			Element e = elementCategories.get(i);
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
		Element elementProductDetails = document.select("#panCaracteristica").first();

		if(elementProductDetails != null) 	description = description + elementProductDetails.html();

		return description;
	}

}
