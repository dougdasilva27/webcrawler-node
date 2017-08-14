package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 14/08/2017
 * @author gabriel
 *
 */
public class BrasilMiamistoreCrawler extends Crawler {

	private final String HOME_PAGE = "https://www.miami.com.br/";
	
	public BrasilMiamistoreCrawler(Session session) {
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
		List<Product> products = new ArrayList<>();

		if( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
			
			String mainPageName = crawlName(doc);
			String internalPid = this.crawlInternalPid(doc);
			CategoryCollection categories = crawlCategories(doc);
			String description = this.crawlDescription(doc);
			boolean hasVariations = hasVariationsSku(doc);
			JSONArray productsArray = crawlSkuJsonArray(doc); 
			
			// Number of colors in the sku
			Map<String, String>  colorsMap = this.identifyNumberOfColors(doc);
			
			// Get images from Colors
			JSONArray imageColorsArray = this.fetchImageColors(colorsMap, this.session.getOriginalURL());

			// if product has variations, the first product is a product default, so is not crawled
			// then if is not, the last product is not crawled, because is a invalid product
			int indexStart = 0;
			int indexFinished = productsArray.length();
			
			if(hasVariations){
				indexStart++;
			} else {
				indexFinished--;
			}
			
			for(int i = indexStart; i < indexFinished; i++) { 
				JSONObject jsonSku = productsArray.getJSONObject(i);
				String name = this.crawlName(jsonSku, mainPageName);
				String internalId = this.crawlInternalId(jsonSku);
				Integer stock = crawlStock(jsonSku);
				boolean available = this.crawlAvailability(stock);
				Float price = this.crawlPrice(jsonSku);
				String primaryImageVariation = this.crawlPrimaryImage(doc, name, imageColorsArray);
				String secondaryImagesVariation = this.crawlSecondaryImages(doc, primaryImageVariation, name, imageColorsArray);
				Prices prices = crawlPrices(price, jsonSku);
				
				// Creating the product
				Product product = ProductBuilder.create()
						.setUrl(session.getOriginalURL())
						.setInternalId(internalId)
						.setInternalPid(internalPid)
						.setName(name)
						.setPrice(price)
						.setPrices(prices)
						.setAvailable(available)
						.setCategory1(categories.getCategory(0))
						.setCategory2(categories.getCategory(1))
						.setCategory3(categories.getCategory(2))
						.setPrimaryImage(primaryImageVariation)
						.setSecondaryImages(secondaryImagesVariation)
						.setDescription(description)
						.setStock(stock)
						.setMarketplace(new Marketplace())
						.build();

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

	private boolean isProductPage(Document doc) {
		return doc.select(".name h3").first() != null;
	}

	

	/************************************
	 * Multiple products identification *
	 ************************************/

	private String crawlInternalPid(Document document) {
		String internalPid = null;
		Element elementInternalId = document.select("input[name=ProductID]").first();
		if (elementInternalId != null) {
			internalPid = elementInternalId.val();
		}

		return internalPid;
	}
	
	
	/*******************
	 * General methods *
	 *******************/

	private String crawlName(Document doc){
		String name = null;
		Element nameElement = doc.select(".name h3").first();
		
		if(nameElement != null){
			name = nameElement.text();
		}
		
		return name;
	}
	
	private Float crawlPrice(JSONObject jsonSku) {
		Float price = null;	
		
		if(jsonSku.has("price")){
			price = Float.parseFloat(jsonSku.getString("price").replaceAll("[^0-9,]+", "").replace(".", "").replace(",", ".").trim());
		}
		
		return price;
	}
	
	private String crawlInternalId(JSONObject jsonSku){
		String internalID = null;
		
		if(jsonSku.has("productID")){
			internalID = jsonSku.getString("productID");
		}
		
		return internalID;
	}
	
	private boolean crawlAvailability(Integer stock) {
		if(stock != null && stock > 0){
			return true;
		}
		
		return false;
	}
	
	private String crawlPrimaryImage(Document document, String name, JSONArray colorsImages) {
		String primaryImage = null;
		
		if(colorsImages.length() < 2){
			Element primaryImageElement = document.select("a.large-gallery").first();
			
			if(primaryImageElement != null) {
				primaryImage = primaryImageElement.attr("href");
			}
			
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
		List<String> secondaryImagesList = new ArrayList<>();
		JSONArray secondaryImagesArray = new JSONArray();
		
		if(colorsImages.length() < 2){
			Elements elementFotoSecundaria = document.select("a.large-gallery");
	
			if (elementFotoSecundaria.size()>1) {
				for(int i = 1; i < elementFotoSecundaria.size(); i++) { //starts with index 1 because de primary image is the first image
					Element e = elementFotoSecundaria.get(i);

					if(e != null){
						String secondaryImagesTemp = e.attr("href");
						
						if(!primaryImage.equals(secondaryImagesTemp) && !secondaryImagesList.contains(secondaryImagesTemp)) { // identify if the image is the primary image
							secondaryImagesList.add(secondaryImagesTemp); 
						}
					} 
					
				}
			}
			
			for(String image : secondaryImagesList){
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
	
	private String crawlName(JSONObject jsonSku, String mainPageName) {
		String name = mainPageName;
					
		if(jsonSku.has("options")){
			JSONArray jsonOptions = jsonSku.getJSONArray("options");
			
			for(int i = 0; i < jsonOptions.length(); i++){
				JSONObject option = jsonOptions.getJSONObject(i);
				
				if(option.has("title")){
					String nameVariation = option.getString("title").trim();
					
					if(!nameVariation.isEmpty() && !name.toLowerCase().contains(nameVariation.toLowerCase())){
						name +=  " " + nameVariation;
					}
				}
			}
		}
		
		
		return name;
	}

	private CategoryCollection crawlCategories(Document document) {
		CategoryCollection categories = new CategoryCollection();
		Elements elementCategories = document.select(".wd-browsing-breadcrumbs li a span");
		
		for(Element e : elementCategories) {
			String category = e.text().trim();
			
			if(!category.toLowerCase().contains("inicial")){
				categories.add(category);
			}
		}

		return categories;
	}

	private String crawlDescription(Document document) {
		String description = "";
		Element elementProductDetails = document.select(".descriptions").first();
		
		if(elementProductDetails != null) {
			description = description + elementProductDetails.html();
		}
		
		return description;
	}
	
	private Map<String,String> identifyNumberOfColors(Document doc){
		Map<String,String> colors = new HashMap<> ();
		Element colorElement = doc.select(".variation-group.type-color-pallete").first();
		
		if(colorElement != null){
			Elements colorsElementsTemp = colorElement.select(".options label span");
			
			for(Element e : colorsElementsTemp){
				Element input = e.select("input").first();
				
				if(input != null) {
					colors.put(input.val(), e.text().trim());
				}
			}
			
		}
		
		return colors;
	}
	
	
	private JSONArray fetchImageColors(Map<String,String> colors, String url){
		JSONArray colorsArray = new JSONArray();
		
		for(Entry<String, String> color : colors.entrySet()){ 
			JSONObject jsonColor = new JSONObject();				
			jsonColor.put("color", color.getValue());
			
			String urlColor = url.contains("?") ? url.split("\\?")[0] + "?pp=/" + color.getKey() + "/" : url + "?pp=/" + color.getKey() + "/";
			Document doc = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, urlColor, null, null);
			
			Elements colorsElements = doc.select("li.image");
			
			if(colorsElements.size() > 0) {
				String primaryImage = colorsElements.get(0).select("img").attr("data-image-large");
				JSONArray secondaryImages = new JSONArray();
				
				for(Element e : colorsElements){
					if(!e.hasAttr("style")){
						String image = e.select("img").attr("data-image-large");
						
						if(e.hasClass("selected")) {
							primaryImage = image;
						} else {
							secondaryImages.put(image);
						}
					}
				}
				
				jsonColor.put("primaryImage", primaryImage);
				jsonColor.put("secondaryImages", secondaryImages);
				
				colorsArray.put(jsonColor);
			}
		}
		
		return colorsArray;
	}
	
	private Integer crawlStock(JSONObject jsonSku){
		Integer stock = null;
		
		if(jsonSku.has("StockBalance")){
			String stockString = jsonSku.getString("StockBalance");
			
			if(stockString.contains(",")){
				stock = Integer.parseInt(stockString.split(",")[0].trim());
			} else {
				stock = Integer.parseInt(stockString);
			}
		}
		
		return stock;
	}
	
	private Prices crawlPrices(Float price, JSONObject jsonSku){
		Prices prices = new Prices();
		
		if(price != null){
			Map<Integer,Float> installmentPriceMap = new TreeMap<>();
			installmentPriceMap.put(1, price);
			
			if(jsonSku.has("priceDescription")){
				String html = jsonSku.getString("priceDescription").replaceAll("&lt;", "<")
						.replaceAll("&gt;", ">").replaceAll("&#39;", "\"")
						.replaceAll("&quot;", "");
				
				Document docJson = Jsoup.parse(html);
				Element boleto = docJson.select(".instant-price").first();
				
				if(boleto != null){
					Float inCashPrice = MathCommonsMethods.parseFloat(boleto.ownText());
					prices.setBankTicketPrice(inCashPrice);
				} else {
					prices.setBankTicketPrice(price);
				}
				
				Element parcels = docJson.select(".condition .parcels").first();
				Element parcelValue = docJson.select(".condition .parcel-value").first();
				
				if(parcels != null && parcelValue != null) {
					Integer installment = parcels.ownText().replaceAll("[^0-9]", "").trim().isEmpty() 
							? null : Integer.parseInt(parcels.ownText().replaceAll("[^0-9]", "").trim());
					
					Float value = MathCommonsMethods.parseFloat(parcelValue.ownText());
					
					if(installment != null && value != null) {
						installmentPriceMap.put(installment, value);
					}
				}
			} else {
				prices.setBankTicketPrice(price);
			}
			
			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
		}
		
		return prices;
	}

	
	private boolean hasVariationsSku(Document doc){
		Elements skus = doc.select(".sku-option");
		
		if(skus.size() > 2){
			return true;
		}
		
		return false;
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
