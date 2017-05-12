package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.text.Normalizer;
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

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import models.Prices;

/************************************************************************************************************************************************************************************
 * Crawling notes (19/07/2016):
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
 * 8) Variations of skus are not crawled if the variation is unavailable because it is not displayed for the user,
 * except if there is a html element voltage, because variations of voltage are displayed for the user even though unavailable.
 * 
 * 9) The url of the primary images are changed to bigger dimensions manually.
 * 
 * 10) The secondary images are get in api "http://www.havan.com.br/produto/sku/" + internalId
 * 
 * Examples:
 * ex1 (available): http://www.havan.com.br/cafeteira-philco-ph31-red/p
 * ex2 (unavailable): http://www.havan.com.br/panela-de-pressao-clock-inox-6-0-litros/p
 *
 *
 ************************************************************************************************************************************************************************************/

public class BrasilHavanCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.havan.com.br/";

	public BrasilHavanCrawler(Session session) {
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
			
			// sku data in json
			JSONArray arraySkus = crawlSkuJsonArray(doc);			
			
			for(int i = 0; i < arraySkus.length(); i++){
				JSONObject jsonSku = arraySkus.getJSONObject(i);
				
				// InternalId 
				String internalId = crawlInternalId(jsonSku);
				
				// Primary image
				String primaryImage = crawlPrimaryImage(jsonSku);
				
				// Name
				String name = crawlName(doc, jsonSku);
				
				// Secondary images
				String secondaryImages = crawlSecondaryImages(internalId);
				
				// Marketplace map
				Map<String, Float> marketplaceMap = crawlMarketplace(jsonSku);

				// Marketplace
				JSONArray marketplace = assembleMarketplaceFromMap(marketplaceMap, internalId);
				
				// Availability
				boolean available = crawlAvailability(marketplaceMap);
				
				// Price
				Float price = crawlMainPagePrice(marketplaceMap);
				
				// Prices
				Prices prices = crawlPrices(internalId, price);
				
				// Creating the product
				Product product = new Product();
				product.setUrl(this.session.getOriginalURL());
				product.setInternalId(internalId);
				product.setInternalPid(internalPid);
				product.setName(name);
				product.setPrice(price);
				product.setPrices(prices);
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
		if ( document.select(".product-qd-v1-name").first() != null ) return true;
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

	private Float crawlMainPagePrice(Map<String, Float> marketplace) {
		Float price = null;
		
		if (marketplace.containsKey("havan")) {
			price = marketplace.get("havan");
		}

		return price;
	}
	
	private boolean crawlAvailability(Map<String, Float> marketplace) {

		if(marketplace.containsKey("havan")) return true;
		
		return false;
	}
	
	private boolean crawlAvailabilityMarketPlace(JSONObject json) {

		if(json.has("available")) return json.getBoolean("available");
		
		return false;
	}

	private Map<String, Float> crawlMarketplace(JSONObject json) {
		Map<String, Float> marketplace = new HashMap<String, Float>();
		
		if(json.has("seller")){
			String nameSeller = json.getString("seller").toLowerCase().trim();
			
			if (json.has("bestPriceFormated") && crawlAvailabilityMarketPlace(json)) {
				Float price = Float.parseFloat( json.getString("bestPriceFormated").replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
				marketplace.put(nameSeller, price);
			}
		}
		
		return marketplace;
	}
	
	private JSONArray assembleMarketplaceFromMap(Map<String, Float> marketplaceMap, String internalId) {
		JSONArray marketplace = new JSONArray();

		for (String seller : marketplaceMap.keySet()) {
			if ( !seller.equals("havan") ) { 
				Float price = marketplaceMap.get(seller);

				JSONObject partner = new JSONObject();
				partner.put("name", seller);
				partner.put("price", price);
				partner.put("prices", crawlPrices(internalId, price).toJSON());

				marketplace.put(partner);
			}
		}

		return marketplace;
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
	
		String url = "http://www.havan.com.br/produto/sku/" + internalId;
		String stringJsonImages = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, null); //GET request to get secondary images
		
		JSONObject jsonObjectImages;
		try {
			jsonObjectImages = new JSONArray(stringJsonImages).getJSONObject(0);
		} catch (JSONException e) {
			jsonObjectImages = new JSONObject();
			e.printStackTrace();
		}
		
		if (jsonObjectImages.has("Images")) {
			JSONArray jsonArrayImages = jsonObjectImages.getJSONArray("Images");
			
			for (int i = 1; i < jsonArrayImages.length(); i++) {				//starts with index 1, because the first image is the primary image
				JSONArray arrayImage = jsonArrayImages.getJSONArray(i);
				JSONObject jsonImage = arrayImage.getJSONObject(0);
				
				if(jsonImage.has("Path")){
					String urlImage = modifyImageURL(jsonImage.getString("Path"));
					secondaryImagesArray.put(urlImage);
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
		String dimensionImageFinal = tokens2[0] + "-1200-1200";
		
		String urlReturn = url.replace(dimensionImage, dimensionImageFinal); //The image size is changed
		
		return urlReturn;	
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
		Element descriptionElement = document.select("#product-qd-v1-description").first();
		Element descriptionElementWithClass = document.select(".product-qd-v1-description").first();
		Element specElement = document.select("#caracteristicas").first();

		if (descriptionElement != null) description = description + descriptionElement.html();
		if (descriptionElementWithClass != null && description.equals("")) description = description + descriptionElementWithClass.html();
		if (specElement != null) description = description + specElement.html();

		return description;
	}
	
	/**
	 * To crawl this prices is accessed a api
	 * Is removed all accents for crawl price 1x like this:
	 * Visa à vista	R$ 1.790,00
	 * 
	 * @param internalId
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(String internalId, Float price){
		Prices prices = new Prices();

		if(price != null){
			String url = "http://www.havan.com.br/productotherpaymentsystems/" + internalId;

			Document doc = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, cookies);

			Element bank = doc.select("#ltlPrecoWrapper em").first();
			if(bank != null){
				prices.setBankTicketPrice(Float.parseFloat(bank.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim()));
			}

			Elements cardsElements = doc.select("#ddlCartao option");

			for(Element e : cardsElements){
				String text = e.text().toLowerCase();

				if (text.contains("visa")) {
					Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
					prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
					
				} else if (text.contains("mastercard")) {
					Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
					prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
					
				} else if (text.contains("diners")) {
					Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
					prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
					
				} else if (text.contains("american") || text.contains("amex")) {
					Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
					prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);	
					
				} else if (text.contains("hipercard") || text.contains("amex")) {
					Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
					prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);	
					
				} else if (text.contains("credicard") ) {
					Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
					prices.insertCardInstallment(Card.CREDICARD.toString(), installmentPriceMap);
					
				} else if (text.contains("elo") ) {
					Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
					prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
					
				}
			} 


		}

		return prices;
	}

	private Map<Integer,Float> getInstallmentsForCard(Document doc, String idCard){
		Map<Integer,Float> mapInstallments = new HashMap<>();

		Elements installmentsCard = doc.select(".tbl-payment-system#tbl" + idCard + " tr");
		for(Element i : installmentsCard){
			Element installmentElement = i.select("td.parcelas").first();

			if(installmentElement != null){
				String textInstallment = removeAccents(installmentElement.text().toLowerCase());
				Integer installment = null;

				if(textInstallment.contains("vista")){
					installment = 1;					
				} else {
					installment = Integer.parseInt(textInstallment.replaceAll("[^0-9]", "").trim());
				}

				Element valueElement = i.select("td:not(.parcelas)").first();

				if(valueElement != null){
					Float value = Float.parseFloat(valueElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());

					mapInstallments.put(installment, value);
				}
			}
		}

		return mapInstallments;
	}

	private String removeAccents(String str) {
		str = Normalizer.normalize(str, Normalizer.Form.NFD);
		str = str.replaceAll("[^\\p{ASCII}]", "");
		return str;
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
