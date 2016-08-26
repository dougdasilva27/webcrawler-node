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

import br.com.lett.crawlernode.kernel.fetcher.DataFetcher;
import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.Crawler;
import br.com.lett.crawlernode.kernel.task.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;

/************************************************************************************************************************************************************************************
 * Crawling notes (22/07/2016):
 * 
 * 1) For this crawler, we have one url for mutiples skus.
 *  
 * 2) There is stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace information in this ecommerce
 *  
 * 4) To get the secondaryImages in for products with colors, is acessed a page from this color for crawled
 * 
 * 5) The sku page identification is done simply looking the URL format and the html element.
 * 
 * 6) Even if a product is unavailable, its price is displayed.
 * 
 * 7) There is internalPid for skus in this ecommerce. The internalPid is a number that is the same for all
 * the variations of a given sku.
 * 
 * 8) The first image in secondary images is the primary image, sometimes the second image too, then it made ​​a check.
 * 
 * 9) The price of variation is crawler from the script code var_variants in html
 * 
 * Examples:
 * ex1 (available): http://www.estrela10.com.br/placa-de-video-vga-geforce-gtx-750ti-oc-2gb-gddr5-128bits-pny-84830-p11270167
 * ex2 (unavailable): http://www.estrela10.com.br/placa-de-video-geforce-gt-640-2gb-ddr3-128-bits-evga-25650-p4356170
 * ex3 (With colors): http://www.estrela10.com.br/jaqueta-fleece-com-bolso-frontal-flexxxa-mormaii-64858-p10269522
 * ex4 (With Volts): http://www.estrela10.com.br/furadeira-profissional-sem-impacto-fsv400-vonder-14743-p4399019
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilEstrela10Crawler extends Crawler {

	private final String HOME_PAGE = "http://www.estrela10.com.br/";
	
	public BrasilEstrela10Crawler(CrawlerSession session) {
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

			// Number of colors in the sku
			Map<String, String>  colorsMap = this.identifyNumberOfColors(doc);
			
			// Get images from Colors
			JSONArray imageColorsArray = this.fetchImageColors(colorsMap, this.session.getUrl());
			
			Logging.printLogDebug(logger, session, "Crawling information of more than one product...");

			Elements productVariationElements = this.crawlSkuOptions(doc);
			
			// HasVariations
			boolean hasVariations = this.hasProductVariations(productVariationElements);
			
			for(int i = 0; i < productVariationElements.size(); i++) {

				
				Element sku = productVariationElements.get(i);
				
				// Name
				String name = this.crawlName(sku);
				
				// InternalId
				String internalID = this.crawlInternalId(sku);

				// Estoque
				Integer stock = Integer.parseInt(sku.select("span.item-product-stock-balance").text().trim());

				// Available
				boolean available = this.crawlAvailability(stock);
				
				// Price
				Float price = this.crawlPrice(doc, internalID, hasVariations);
				
				// PrimaryImage
				String primaryImageVariation = this.crawlPrimaryImage(doc, name, imageColorsArray);
				
				// Secondary Images
				String secondaryImagesVariation = this.crawlSecondaryImages(doc, primaryImageVariation, name, imageColorsArray);

				//Price
				Float priceVariation = this.crawlPriceVariation(available, price);
				
				Product product = new Product();
				product.setSeedId(this.session.getSeedId());
				product.setUrl(this.session.getUrl());
				product.setInternalId(internalID);
				product.setInternalPid(internalPid);
				product.setName(name);
				product.setPrice(priceVariation);
				product.setCategory1(category1);
				product.setCategory2(category2);
				product.setCategory3(category3);
				product.setPrimaryImage(primaryImageVariation);
				product.setSecondaryImages(secondaryImagesVariation);
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
		Element producElement = doc.select(".title-product").first();
		
		if (producElement != null && !url.contains("?")) return true;
		return false;
	}

	

	/************************************
	 * Multiple products identification *
	 ************************************/

	private boolean hasProductVariations(Elements skus) {
		
		if (skus.size() > 1) {
			return true;
		} else {
			return false;
		}
	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;
		Element elementInternalId = document.select("#wd26").first();
		if (elementInternalId != null) {
			internalPid = elementInternalId.attr("data-widget-pid").trim();
		}

		return internalPid;
	}
	
	

	/*********************************
	 * Multiple product page methods *
	 *********************************/


	private Elements crawlSkuOptions(Document document) {
		Elements skuOptions = document.select(".bundle-item-cell.bundle-item-description");

		return skuOptions;
	}

	
	private Float crawlPriceVariation(boolean available, Float price){
		Float priceVariation = null;
		
		if(available) priceVariation = price;
		
		return priceVariation;
	}
	
	/*******************
	 * General methods *
	 *******************/

	private Float crawlPrice(Document doc, String intenalId, boolean hasVariations) {
		Float price = null;	
		
		if(hasVariations){
			JSONArray jsonSkusArray = crawlSkuJsonArray(doc);
			
			for(int i = 1; i< jsonSkusArray.length(); i++) { //starts with index 1 because the first index is the product default
				JSONObject jsonSku = jsonSkusArray.getJSONObject(i);
				String internalIdTemp = "";
				
				if(jsonSku.has("productID")) internalIdTemp = jsonSku.getString("productID").trim();
				
				if(intenalId.equals(internalIdTemp)){
					if(jsonSku.has("price")){
						price = Float.parseFloat(jsonSku.getString("price").replaceAll("\\.", "").replaceAll(",", ".").trim());
					}
					break;
				}
			}
		} else {
			Element priceElement = doc.select(".sale-price span[itemprop=price]").first();
			
			if(priceElement != null){
				price = Float.parseFloat( priceElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
			}
		}
		
				
		return price;
	}
	
	private String crawlInternalId(Element variation){
		String internalID = null;
		Element internalIdElement = variation.select("span.item-product-identification").first();
		
		if(variation != null){
			internalID = internalIdElement.text().replaceAll("[^0-9]", "").trim();
		}
		
		return internalID;
	}
	
	private boolean crawlAvailability(Integer stock) {
		boolean available = false;
		
		if(stock != null){
			if(stock > 0) return true;
		}
		
		return available;
	}
	
	private String crawlPrimaryImage(Document document, String name, JSONArray colorsImages) {
		String primaryImage = null;
		
		if(colorsImages.length() < 1){
			Elements primaryImageElements = document.select("a.large-gallery");
			
			if(primaryImageElements != null) primaryImage = primaryImageElements.get(0).attr("href");
			
		} else {

			for(int i = 0; i < colorsImages.length(); i++){
				JSONObject colorsJson = colorsImages.getJSONObject(i);
				String color = colorsJson.getString("color").toLowerCase();
				
				if(name.toLowerCase().contains(color) || name.toLowerCase().contains(color.substring(0, color.length()-1))){
					primaryImage = colorsJson.getString("primaryImage");
					break;
				}
			}

		}
		
		return primaryImage;
		
	}
	
	private String crawlSecondaryImages(Document document, String primaryImage, String name, JSONArray colorsImages) {
		String secondaryImages = null;
		Map<Integer,String> secondaryImagesMap = new HashMap<Integer,String>();
		JSONArray secondaryImagesArray = new JSONArray();
		
		if(colorsImages.length() < 1){
			Elements elementFotoSecundaria = document.select("a.large-gallery");
	
			if (elementFotoSecundaria.size()>1) {
				for(int i = 1; i < elementFotoSecundaria.size(); i++) { //starts with index 1 because de primary image is the first image
					Element e = elementFotoSecundaria.get(i);
					String secondaryImagesTemp = null;
					
					if(e != null){
						secondaryImagesTemp = e.attr("href");
						
						if(!primaryImage.equals(secondaryImagesTemp)) { // identify if the image is the primary image
							secondaryImagesMap.put(i,secondaryImagesTemp); 
						}
					} 
					
				}
			}
			
			for(String image : secondaryImagesMap.values()){
				secondaryImagesArray.put(image);
			}
			
		} else {
			
			for(int i = 0; i < colorsImages.length(); i++){
				JSONObject colorsJson = colorsImages.getJSONObject(i);
				
				String color = colorsJson.getString("color").toLowerCase();
				
				if(name.toLowerCase().contains(color) || name.toLowerCase().contains(color.substring(0, color.length()-1))){
					secondaryImagesArray = colorsJson.getJSONArray("secondaryImages");
					break;
				}
			}
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}
		
		return secondaryImages;
	}	
	
	private String crawlName(Element sku) {
		String name = null;
		Element elementName = sku.select("span.title").first();
		if(elementName != null) {
			name = elementName.text().replace("'","").replace("’","").trim();
		}
		return name;
	}

	private ArrayList<String> crawlCategories(Document document) {
		Elements elementCategories = document.select("li[itemprop=child] > a");
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
	
	private Map<String,String> identifyNumberOfColors(Document doc){
		Map<String,String> colors = new HashMap<String,String> ();
		Elements colorsElements = doc.select(".variation-group");
		Element colorElement = null;
		
		
		for(Element e : colorsElements){
			if(e.select("b").text().equals("Cor")){
				colorElement = e;
				break;
			}
		}
		
		if(colorElement != null){
			Elements colorsElementsTemp = colorElement.select("option[id]");
			
			for(int i = 0; i < colorsElementsTemp.size(); i++){
				Element e = colorsElementsTemp.get(i);
				colors.put(e.attr("value"), e.text().trim());
			}
			
		}
		
		return colors;
	}
	
	
	private JSONArray fetchImageColors(Map<String,String> colors, String url){
		JSONArray colorsArray = new JSONArray();
		
		if(colors.size() > 0){
			for(String idColor : colors.keySet()){ 
				String urlColor = url + "?pp=/" + idColor + "/";
				JSONObject jsonColor = new JSONObject();				
				jsonColor.put("color", colors.get(idColor));
				
				Document doc = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, urlColor, null, null);
				Elements colorsElements = doc.select("li.image");
				
				String primaryImage = colorsElements.get(0).select("img").attr("data-image-large");
				JSONArray secondaryImages = new JSONArray();
				
				for(Element e : colorsElements){
					if(!e.hasAttr("style")){
						String image = e.select("img").attr("data-image-large");
						
						if(e.hasClass("selected")) 	primaryImage = image;
						else						secondaryImages.put(image);
					}
				}
				
				jsonColor.put("primaryImage", primaryImage);
				jsonColor.put("secondaryImages", secondaryImages);
				
				colorsArray.put(jsonColor);
			}
		}
		
		return colorsArray;
	}
	
	/**
	 * Get the script having a json with the availability information
	 * @return
	 */
	private JSONArray crawlSkuJsonArray(Document document) {
		Elements scriptTags = document.getElementsByTag("script");
		JSONArray skuJson = null;
		
		for (Element tag : scriptTags){                
			for (DataNode node : tag.dataNodes()) {
				if(tag.html().trim().startsWith("var variants = ")) {

					skuJson = new JSONArray
							(
							node.getWholeData().split(Pattern.quote("var variants = "))[1] +
							node.getWholeData().split(Pattern.quote("var variants = "))[1].split(Pattern.quote("];"))[0]
							);

				}
			}        
		}
		
		return skuJson;
	}
}
