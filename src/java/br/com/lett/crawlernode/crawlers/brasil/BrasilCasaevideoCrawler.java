package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
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
 * Crawling notes (02/08/2016):
 * 
 * 1) For this crawler, we have one url per mutiple skus.
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking for an specific html element.
 * 
 * 5) Even if a product is unavailable, its price is not displayed.
 * 
 * 6) There is internalPid for skus in this ecommerce. The internalPid is a number that is the same for all
 * the variations of a given sku.
 * 
 * 7) The primary image is in the secondary images in random index.
 * 
 * 8) To crawled price of variations is accessed a api.
 * 
 * 9) Name of products is make from options in the html page.
 * 
 * Examples:
 * ex1 (available): http://www.casaevideo.com.br/webapp/wcs/stores/servlet/ProductDisplay?productId=772514
 * ex2 (unavailable): http://www.casaevideo.com.br/webapp/wcs/stores/servlet/ProductDisplay?productId=142536
 * ex3 (Available 1 variation) : http://www.casaevideo.com.br/webapp/wcs/stores/servlet/pt/auroraesite/cafeteira-expresso-15bar-arno-dolce-gusto-piccolo-127v
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilCasaevideoCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.casaevideo.com.br/";

	public BrasilCasaevideoCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			// Variations
			boolean hasVariations = this.hasVariationsFunction(doc);

			// Pid
			String internalPid = crawlInternalPid(doc);

			// Name
			String name = crawlName(doc, hasVariations);

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

			if(hasVariations){

				/*************************************
				 * crawling data of mutiple products *
				 *************************************/


				JSONArray arraySkus = this.crawlMutipleVariations(doc);

				for(int i = 0; i < arraySkus.length(); i++){
					JSONObject jsonSku = arraySkus.getJSONObject(i);

					// InternalId
					String internalID = crawlInternalIdVariation(jsonSku);

					// Name
					String nameVariation = crawlNameVariation(jsonSku, name);

					if(nameVariation == null) continue; // se o nome for nulo é porque essa variação não aparece para o usuário

					// Price
					Float priceVariation = this.crawlPriceVariation(internalPid, internalID);

					// Availability
					boolean available = crawlAvailabilityVariation(priceVariation);

					// Creating the product
					Product product = new Product();
					product.setSeedId(session.getSeedId());
					product.setUrl(session.getUrl());
					product.setInternalId(internalID);
					product.setInternalPid(internalPid);
					product.setName(nameVariation);
					product.setPrice(priceVariation);
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

				/*************************************
				 * crawling data of only one product *
				 *************************************/

			} else {

				// InternalId
				String internalID = crawlInternalIdSingleProduct(doc);

				// Available
				boolean available = this.crawlAvailabilitySingleProduct(doc);

				// Price
				Float price = this.crawlPriceSingleProduct(doc, available);

				// Creating the product
				Product product = new Product();
				product.setSeedId(session.getSeedId());
				product.setUrl(session.getUrl());
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

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}
		
		return products;
	}



	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(Document document) {
		if ( document.select(".product_options").first() != null ) return true;
		return false;
	}

	/********************
	 * Multiple Product *
	 ********************/

	private JSONArray crawlMutipleVariations(Document doc){
		JSONArray skusArray = new JSONArray();
		Element variations = doc.select("#catEntryParams").first();

		if(variations != null){
			JSONObject jsonSkus = new JSONObject();

			String htmlJson = variations.outerHtml();

			if(htmlJson.contains("skus:")){
				int x = htmlJson.indexOf("skus:");

				String jsonFinal = "{" + htmlJson.substring(x);

				try {
					jsonSkus = new JSONObject(jsonFinal);
				} catch (JSONException e1) {
					
				}
			}

			if(jsonSkus.has("skus")){
				skusArray = jsonSkus.getJSONArray("skus");
			}
		}

		return skusArray;
	}

	private boolean hasVariationsFunction(Document doc){
		Element hasVariatonsElement = doc.select(".options_dropdown").first();

		if(hasVariatonsElement != null) return true;

		return false;
	}

	private String crawlInternalIdVariation(JSONObject jsonSku) {
		String internalId = null;

		if(jsonSku.has("id")) internalId = jsonSku.getString("id");

		return internalId;
	}

	private String crawlNameVariation(JSONObject jsonSku, String name) {
		String nameVariation = null;

		if(jsonSku.has("attributes")){
			JSONObject objectAtributes = jsonSku.getJSONObject("attributes");

			if(objectAtributes.has("Opções do Produto")) nameVariation = name + " " + objectAtributes.getString("Opções do Produto");
		}

		return nameVariation;
	}

	private Float crawlPriceVariation(String internalPid, String internalID) {
		Float price = null;
		JSONObject jsonSku = this.crawlPriceFromApi(internalPid, internalID);

		if(jsonSku.has("offerPrice")){
			String stringPrice = jsonSku.getString("offerPrice");
			if(stringPrice.replaceAll("[^0-9,]+", "").length() > 1){
				price = Float.parseFloat( stringPrice.replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
			}
		}

		return price;
	}

	// to get price is accessed this api
	private JSONObject crawlPriceFromApi(String internalPid, String internalID){
		JSONObject jsonSku = new JSONObject();
		String params = "storeId=10152&langId=-6&catalogId=10001&catalogEntryId="+ internalID +"&productId="+ internalPid;
		String urlPost = "http://www.casaevideo.com.br/webapp/wcs/stores/servlet/GetCatalogEntryDetailsByIDView";

		Map<String,String> headers = new HashMap<>();
		headers.put("Content-Type", "application/x-www-form-urlencoded");

		String response = DataFetcher.fetchPagePOSTWithHeaders(urlPost, session, params, null, 1, headers);

		if(response != null){
			int x = response.indexOf("/*");
			int y = response.indexOf("*/", x+2);

			jsonSku = new JSONObject(response.substring(x+2, y).trim()).getJSONObject("catalogEntry");
		}

		return jsonSku;
	}

	private boolean crawlAvailabilityVariation(Float price) {

		if (price == null) {
			return false;
		}

		return true;
	}

	/*******************
	 * General methods *
	 *******************/

	private String crawlInternalIdSingleProduct(Document document) {
		String internalId = null;
		Element internalIdElement = document.select("#catEntryParams").first();

		if (internalIdElement != null) {
			String idHtml = internalIdElement.outerHtml();

			if(idHtml.contains("id:")){
				int x = idHtml.indexOf("id:");
				int y = idHtml.indexOf(",", x+3);
				
				internalId = idHtml.substring(x+3, y).replaceAll("[^0-9]", "").trim();
			}
		}

		return internalId;
	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;
		Element pidElement = document.select("span.sku").first();

		if(pidElement != null){
			String[] tokens = pidElement.attr("id").split("_");
			internalPid = tokens[tokens.length-1];
		}

		return internalPid;
	}

	private String crawlName(Document document, boolean hasVariations) {
		String name = null;
		Element nameElement = document.select(".top .main_header").first();

		if (nameElement != null) {
			name = nameElement.text().toString().trim();
		}

		if(!hasVariations){
			Element nameSpecial = document.select(".product_options option").first();

			if(nameSpecial != null){
				name = name + " " + nameSpecial.text();
			}
		}

		return name;
	}

	private Float crawlPriceSingleProduct(Document document, boolean available) {
		Float price = null;

		if(available){
			Element specialPrice = document.select("span.price").first();		

			if (specialPrice != null) {
				price = Float.parseFloat( specialPrice.text().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
			}
		}
		return price;
	}

	private boolean crawlAvailabilitySingleProduct(Document document) {
		Element notifyMeElement = document.select(".sublist span.text[id]").first();

		if (notifyMeElement != null) {
			String status = notifyMeElement.text().toLowerCase();

			if(status.contains("indisponível")){
				return false;
			}
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
		Element primaryImageElement = document.select("#productMainImage").first();

		if (primaryImageElement != null) {
			primaryImage = HOME_PAGE + primaryImageElement.attr("src").trim();
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document, String primaryImage) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select(".photo-thumb img");

		for (int i = 0; i < imagesElement.size(); i++) { 
			String secondaryImage = HOME_PAGE + imagesElement.get(i).attr("src").trim().replaceAll("JPG105", "JPG1000"); // montando url para pegar a maior imagem

			if(!secondaryImage.equals(primaryImage)){
				secondaryImagesArray.put(secondaryImage); 
			}
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select("#widget_breadcrumb li a");

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
		Element descriptionElement = document.select("#tabContainer").first();

		if (descriptionElement != null) description = description + descriptionElement.html();

		return description;
	}

}
