package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.text.Normalizer;
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

import br.com.lett.crawlernode.core.fetcher.DataFetcherNO;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.Seller;
import models.Util;
import models.prices.Prices;

/************************************************************************************************************************************************************************************
 * Crawling notes (01/08/2016):
 * 
 * 1) For this crawler, we have one url per mutiple skus. 
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is marketplace in this ecommerce by the time this crawler was made in some cases.
 * 
 * 4) The sku page identification is done simply looking for url.
 * 
 * 5) Is used a script in html to get name, price, availability, marketplace and internalIDS. 
 * 
 * 6) Even if a product is unavailable, its price is not displayed.
 * 
 * 7) There is internalPid for skus in this ecommerce. The internalPid is a number that is the same for all
 * the variations of a given sku.
 * 
 * 8) The primary image is the first image in the secondary images selector.
 * 
 * Examples:
 * ex1 (available): http://www.casashow.com.br/cafeteira-eletrica-arno-cafp---preta-1000008369/p
 * ex2 (unavailable): http://www.casashow.com.br/ar-condicionado-janela-7500btus-duo-mecanico-frio-127v-branco-qca078bbb-springer/p
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilCasashowCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.casashow.com.br/";
	private final String SELLER_NAME_LOWER = "casashow";
	
	public BrasilCasashowCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			/* ***********************************
			 * crawling data of only one product *
			 *************************************/

			// Name main page
			String nameMainPage = this.crawlNameMainPage(doc);
			
			// Pid
			String internalPid = crawlInternalPid(doc);

			// Categories
			ArrayList<String> categories = crawlCategories(doc);
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);

			// Primary image
			String primaryImage = crawlPrimaryImage(doc);

			// Secondary images
			String secondaryImages = crawlSecondaryImages(doc);

			// Description
			String description = crawlDescription(doc);

			// Stock
			Integer stock = null;
			
			// Skus
			JSONArray arraySkus = this.crawlSkuJsonArray(doc);

			for(int i = 0; i < arraySkus.length(); i++){
				
				JSONObject jsonSku = arraySkus.getJSONObject(i);
				
				// InternalId
				String internalId = crawlInternalId(jsonSku);
				
				// Name
				String name = crawlName(jsonSku, nameMainPage);
				
				// Marketplace map
				Map<String, Float> marketplaceMap = crawlMarketplace(jsonSku);

				// Marketplace
				Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap, internalId);
				
				// Availability
				boolean available = crawlAvailability(jsonSku);
				
				// Price
				Float price = crawlPrice(marketplaceMap, available);
				
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
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
		}
		
		return products;
	}



	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		if ( url.endsWith("/p")  || url.contains("/p?attempt=")) return true;
		return false;
	}
	
	
	/*******************
	 * General methods *
	 *******************/
	
	private String crawlInternalId(JSONObject jsonSku) {
		String internalId = null;
	
		if(jsonSku.has("sku")) internalId = Integer.toString(jsonSku.getInt("sku"));

		return internalId;
	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;
		Element inElement = document.select("#___rc-p-id").first();
		
		if(inElement != null) internalPid = inElement.attr("value");
		
		return internalPid;
	}
	
	private String crawlNameMainPage(Document doc) {
		String name = null;
		Element nameElement = doc.select(".productName").first();
		
		if(nameElement != null) name = nameElement.text();

		return name;
	}
	
	private String crawlName(JSONObject jsonSku, String nameMainPage) {
		String name = null;
		
		if(jsonSku.has("skuname")){
			name = jsonSku.getString("skuname");
			
			if(!name.startsWith(nameMainPage)){
				name = nameMainPage + " " + name;
			}
		}

		return name;
	}

	private Float crawlPrice(Map<String,Float> marketplaces, boolean available) {
		Float price = null;
		
		if(marketplaces.containsKey(SELLER_NAME_LOWER) && available){
			price = marketplaces.get(SELLER_NAME_LOWER);
		}

		return price;
	}
	
	private boolean crawlAvailability(JSONObject jsonSku) {
		boolean available = false;
		
		if(jsonSku.has("available")){
			if(jsonSku.has("seller")){
				if(jsonSku.getString("seller").toLowerCase().equals(SELLER_NAME_LOWER)){
					available = jsonSku.getBoolean("available");
				}
			}
		}
		
		return available;
	}

	private Map<String, Float> crawlMarketplace(JSONObject jsonSku) {
		Map<String, Float> marketplaces = new HashMap<String, Float>();
		String partnerName = null;
		Float partnerPrice = null;
		
		if(jsonSku.has("seller")){
			partnerName = jsonSku.getString("seller").toLowerCase();
			
			if(jsonSku.has("bestPriceFormated")){
				partnerPrice =  Float.parseFloat( jsonSku.getString("bestPriceFormated").replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
				
				marketplaces.put(partnerName, partnerPrice);
			}	
		}
		
		return marketplaces;
	}
	
	private Marketplace assembleMarketplaceFromMap(Map<String, Float> marketplaceMap, String internalId) {
		Marketplace marketplaces = new Marketplace();
		
		for(String market : marketplaceMap.keySet()){
			if(!market.equals(SELLER_NAME_LOWER)){
				JSONObject sellerJSON = new JSONObject();
				sellerJSON.put("name", market);
				sellerJSON.put("price", marketplaceMap.get(market));
				sellerJSON.put("prices", crawlPrices(internalId, marketplaceMap.get(market)).toJSON());
				
				try {
					Seller seller = new Seller(sellerJSON);
					marketplaces.add(seller);
				} catch (Exception e) {
					Logging.printLogError(logger, session, Util.getStackTraceString(e));
				}
			}
		}
		
		return marketplaces;
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select("#image a").first();

		if (primaryImageElement != null) {
			primaryImage = primaryImageElement.attr("href").trim();
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select("#botaoZoom");

		for (int i = 1; i < imagesElement.size(); i++) { // starts with index 1 because the first item is the primary image
			Element e = imagesElement.get(i);
			if(e.hasAttr("zoom") && !e.attr("zoom").isEmpty()){
				secondaryImagesArray.put( e.attr("zoom").trim() );
			} else {
				secondaryImagesArray.put( e.attr("rel").trim() );
			}
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
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
		Element descriptionElement = document.select(".row.description").first();
		Element specificationElement = document.select(".row.specification").first();

		if (descriptionElement != null) 	description = description + descriptionElement.html();
		if (specificationElement != null) 	description = description + specificationElement.html();

		return description;
	}

	/**
	 * No momento em que peguei os preços não foi achado prçeo no boleto com desconto
	 * @param internalId
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(String internalId, Float price){
		Prices prices = new Prices();

		if(price != null){
			String url = "http://www.casashow.com.br/productotherpaymentsystems/" + internalId;

			Document docPrices = DataFetcherNO.fetchDocument(DataFetcherNO.GET_REQUEST, session, url, null, cookies);

			Element bankTicketElement = docPrices.select("#divBoleto em").first();
			if(bankTicketElement != null){				
				Float bankTicketPrice = MathUtils.parseFloatWithComma(bankTicketElement.text());
				prices.setBankTicketPrice(bankTicketPrice);
			}

			Elements cardsElements = docPrices.select("#ddlCartao option");

			for(Element e : cardsElements){
				String text = e.text().toLowerCase();

				if (text.contains("visa")) {
					Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"));
					prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
					
				} else if (text.contains("mastercard")) {
					Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"));
					prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
					
				} else if (text.contains("diners")) {
					Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"));
					prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
					
				} else if (text.contains("american") || text.contains("amex")) {
					Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"));
					prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);	
					
				} else if (text.contains("hipercard")) {
					Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"));
					prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);	
					
				} else if (text.contains("credicard") ) {
					Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"));
					prices.insertCardInstallment(Card.CREDICARD.toString(), installmentPriceMap);
					
				} else if (text.contains("elo") ) {
					Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"));
					prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
					
				} else if (text.contains("aura") ) {
					Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"));
					prices.insertCardInstallment(Card.AURA.toString(), installmentPriceMap);
					
				} else if (text.contains("discover") ) {
					Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"));
					prices.insertCardInstallment(Card.DISCOVER.toString(), installmentPriceMap);
					
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
