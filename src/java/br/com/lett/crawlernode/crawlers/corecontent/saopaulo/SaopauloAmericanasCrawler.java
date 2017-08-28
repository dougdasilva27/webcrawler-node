package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SaopauloB2WCrawlersUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.Seller;
import models.Util;
import models.prices.Prices;


/************************************************************************************************************************************************************************************
 * Crawling notes (24/10/2016):
 * 
 * 1) For this crawler, we have one url for mutiples skus.
 *  
 * 2) There is stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is marketplace for mutiple variations in this ecommerce accessed via the url to 
 *  mutiple variations "http://www.americanas.com.br/parceiros/" + internalID + "/?codItemFusion=" + variationID, 
 *  and for single product is a simply selector in htmlPage.
 *  
 * 4) The most important information of skus are in a json in html.
 *  
 * 5) The sku page identification is done simply looking the URL format.
 * 
 * 6) Even if a product is unavailable, its price is not displayed, then price is null.
 * 
 * 7) There is internalPid for skus in this ecommerce. The internalPid is a number that is the same for all
 * the variations of a given sku.
 * 
 * 8) The first image in secondary images is the primary image.
 * 
 * 9) When the sku has variations, the variation name it is added to the name found in the main page. 
 * 
 * 10) When the market crawled not appear on the page of the partners, the sku is unavailable.
 * 
 * 11) The Id from the main Page is the internalPid.
 * 
 * 12) InternalPid from the main page is used to make internalId final.
 * 
 * Examples:
 * ex1 (available): http://www.americanas.com.br/produto/127115083/smartphone-moto-g-4-dual-chip-android-6.0-tela-5.5-16gb-camera-13mp-preto
 * ex2 (unavailable): http://www.americanas.com.br/produto/119936092/pneu-toyo-tires-aro-18-235-60r18-107v
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class SaopauloAmericanasCrawler extends Crawler {
	private final String HOME_PAGE = "http://www.americanas.com.br/";

	private final String MAIN_SELLER_NAME_LOWER = "americanas.com";

	public SaopauloAmericanasCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	@Override
	public void handleCookiesBeforeFetch() {
		Logging.printLogDebug(logger, session, "Adding cookie...");

		/**
		 * O cookie abaixo foi colocado pois no dia que foi feito
		 * esse crawler, o site americanas.com estava fazendo um testeAB
		 * e o termo new, seria de um suposto site novo.
		 */
		
		BasicClientCookie cookie = new BasicClientCookie("catalogTestAB", "new");
		cookie.setDomain("www.americanas.com.br");
		cookie.setPath("/");
		
		BasicClientCookie cookie2 = new BasicClientCookie("catalogTestAB", "new");
		cookie2.setDomain(".americanas.com.br");
		cookie2.setPath("/");
		
		this.cookies.add(cookie);
		this.cookies.add(cookie2);
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<>();
		
		if( isProductPage(session.getOriginalURL(), doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());


			/* *********************************************************
			 * crawling data common to both the cases of product page  *
			 ***********************************************************/

			// Api onde se consegue todos os preços
			JSONObject initialJson = SaopauloB2WCrawlersUtils.getDataLayer(doc);
			
			// Pega só o que interessa do json da api
			JSONObject infoProductJson = SaopauloB2WCrawlersUtils.assembleJsonProductWithNewWay(initialJson);
			
			// Pid
			String internalPid = this.crawlInternalPid(infoProductJson);

			// Name
			String name = this.crawlMainPageName(doc);

			// Categories
			ArrayList<String> categories = this.crawlCategories(infoProductJson);
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);

			// Primary image
			String primaryImage = this.crawlPrimaryImage(infoProductJson);

			// Secondary images
			String secondaryImages = this.crawlSecondaryImages(infoProductJson);

			// Description
			String description = this.crawlDescription(doc, internalPid);

			// sku data in json
			Map<String,String> skuOptions = this.crawlSkuOptions(infoProductJson);		

			// JSON with stock Information
			JSONObject jsonWithStock = fetchJSONWithStockInformation(internalPid);
			
			for (String internalId : skuOptions.keySet()) {	

				//variation name
				String variationName = (name + " " + skuOptions.get(internalId)).trim();

				// Marketplace map
				Map<String, Prices> marketplaceMap = this.crawlMarketplace(internalId, internalPid);
				
				// Assemble marketplace from marketplace map
				Marketplace variationMarketplace = this.assembleMarketplaceFromMap(marketplaceMap);

				// Available
				boolean available = this.crawlAvailability(marketplaceMap);

				// Price
				Float variationPrice = this.crawlPrice(marketplaceMap);

				// Prices 
				Prices prices = crawlPrices(infoProductJson, variationPrice, internalId);

				// Stock
				Integer stock = crawlStock(infoProductJson, internalId, jsonWithStock);

				Product product = new Product();
				product.setUrl(this.session.getOriginalURL());
				product.setInternalId(internalId);
				product.setInternalPid(internalPid);
				product.setName(variationName);
				product.setPrice(variationPrice);
				product.setPrices(prices);
				product.setCategory1(category1);
				product.setCategory2(category2);
				product.setCategory3(category3);
				product.setPrimaryImage(primaryImage);
				product.setSecondaryImages(secondaryImages);
				product.setDescription(description);
				product.setStock(stock);
				product.setMarketplace(variationMarketplace);
				product.setAvailable(available);
				
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

	private boolean isProductPage(String url, Document doc) {

		if (url.startsWith("https://www.americanas.com.br/produto/") || url.startsWith("http://www.americanas.com.br/produto/")) {
			return true;
		}
		return false;
	}

	private String crawlInternalPid(JSONObject assembleJsonProduct) {
		String internalPid = null;

		if (assembleJsonProduct.has("internalPid")) {
			internalPid = assembleJsonProduct.getString("internalPid").trim();
		}

		return internalPid;
	}

	private Map<String,String> crawlSkuOptions(JSONObject infoProductJson){
		Map<String,String> skuMap = new HashMap<>();

		if(infoProductJson.has("skus")){
			JSONArray skus = infoProductJson.getJSONArray("skus");

			for(int i = 0; i < skus.length(); i++){
				JSONObject sku = skus.getJSONObject(i);

				if(sku.has("internalId")){
					String internalId = sku.getString("internalId");
					String name = "";

					if (sku.has("variationName")) {
						name = sku.getString("variationName");
					}

					skuMap.put(internalId, name);
				}
			}
		}

		return skuMap;
	}

	private Map<String,Prices> crawlMarketplace(String internalId, String pid) {
		Map<String,Prices> marketplaces = new HashMap<>();

		String url = "http://www.americanas.com.br/parceiros/" + pid + "/" + "?codItemFusion=" + internalId + "&productSku=" + internalId;

		Document doc = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, cookies);
		Elements lines = doc.select(".more-offers-table-row");
		
		if(lines.size() < 1){
			lines = doc.select(".card-seller-offer");
		}

		for(Element linePartner: lines) {
			Prices prices = new Prices();
			Map<Integer,Float> installmentMapPrice = new HashMap<>();
			
			String partnerName = linePartner.select("img[title]").first().attr("title").trim().toLowerCase();
			Float partnerPrice = Float.parseFloat(linePartner.select(".sales-price").first().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

			installmentMapPrice.put(1, partnerPrice);
			prices.setBankTicketPrice(partnerPrice);
			
			Element installmentElement = linePartner.select("span.payment-option").first();
			if(installmentElement != null){ 
				String text = installmentElement.text().toLowerCase().trim();
				
				// When text is empty has no installment for this marketplace.
				if(!text.isEmpty()){
					int x = text.indexOf("x");
					
					Integer installment = Integer.parseInt(text.substring(0, x).trim());
					Float value = Float.parseFloat(text.substring(x).replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
					
					installmentMapPrice.put(installment, value);
				}
			}
			
			prices.insertCardInstallment(Card.VISA.toString(), installmentMapPrice);
			marketplaces.put(partnerName, prices);
		}

		return marketplaces;
	}

	/*******************
	 * General methods *
	 *******************/

	private Float crawlPrice(Map<String, Prices> marketplaces) {
		Float price = null;

		for (String seller : marketplaces.keySet()) {
			if (seller.equals(MAIN_SELLER_NAME_LOWER)) {
				Prices prices = marketplaces.get(MAIN_SELLER_NAME_LOWER);
				
				if ( prices.getCardPaymentOptions(Card.VISA.toString()).containsKey(1) ) {
					Double priceDouble = prices.getCardPaymentOptions(Card.VISA.toString()).get(1);
					price = priceDouble.floatValue(); 
				}
				
				break;
			}
		}		
		return price;
	}
	private boolean crawlAvailability(Map<String, Prices> marketplaces) {
		boolean available = false;

		for (String seller : marketplaces.keySet()) {
			if (seller.equals(MAIN_SELLER_NAME_LOWER)) {
				available = true;
			}
		}

		return available;
	}

	private String crawlPrimaryImage(JSONObject infoProductJson) {
		String primaryImage = null;

		if(infoProductJson.has("images")){
			JSONObject images = infoProductJson.getJSONObject("images");

			if(images.has("primaryImage")){
				primaryImage = images.getString("primaryImage");
			}
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(JSONObject infoProductJson) {
		String secondaryImages = null;

		JSONArray secondaryImagesArray = new JSONArray();

		if(infoProductJson.has("images")){
			JSONObject images = infoProductJson.getJSONObject("images");

			if(images.has("secondaryImages")){
				secondaryImagesArray = images.getJSONArray("secondaryImages");
			}
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}	

	private String crawlMainPageName(Document document) {
		String name = null;
		Element elementName = document.select(".product-name").first();
		if(elementName != null) {
			name = elementName.text().replace("'","").replace("’","").trim();
		} else {
			elementName = document.select(".card-title h3").first();
			
			if(elementName != null) {
				name = elementName.text().replace("'","").replace("’","").trim();
			}
		}
		
		return name;
	}

	private ArrayList<String> crawlCategories(JSONObject infoProductJson) {
		ArrayList<String> categories = new ArrayList<>();
		if(infoProductJson.has("categories")){
			JSONArray categoriesJson = infoProductJson.getJSONArray("categories");
			for(int i = categoriesJson.length()-1; i >= 0; i--) {
				JSONObject categorie = categoriesJson.getJSONObject(i);

				if(categorie.has("name")){
					categories.add(categorie.getString("name"));
				}
			}
		}

		return categories;
	}

	private String getCategory(ArrayList<String> categories, int n) {
		if (n < categories.size()) {
			return categories.get(n);
		}

		return "";
	}

	private Marketplace assembleMarketplaceFromMap(Map<String, Prices> marketplaceMap) {
		Marketplace marketplace = new Marketplace();

		for (String sellerName : marketplaceMap.keySet()) {
			if ( !sellerName.equals(MAIN_SELLER_NAME_LOWER) ) {
				JSONObject sellerJSON = new JSONObject();
				sellerJSON.put("name", sellerName);
				
				Prices prices = marketplaceMap.get(sellerName);
				
				if ( prices.getCardPaymentOptions(Card.VISA.toString()).containsKey(1) ) {
					// Pegando o preço de uma vez no cartão
					Double price = prices.getCardPaymentOptions(Card.VISA.toString()).get(1);
					Float priceFloat = price.floatValue();				
					
					sellerJSON.put("price", priceFloat); // preço de boleto é o mesmo de preço uma vez.
				}
				sellerJSON.put("prices", marketplaceMap.get(sellerName).toJSON());
				
				try {
					Seller seller = new Seller(sellerJSON);
					marketplace.add(seller);
				} catch (Exception e) {
					Logging.printLogError(logger, session, Util.getStackTraceString(e));
				}
			}
		}
		
		return marketplace;
	}

	private String crawlDescription(Document document, String internalPid) {
		String description = "";
		
		if(internalPid != null) {
			String url = HOME_PAGE + "product-description/shop/" + internalPid;
			Document docDescription = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, cookies);
			if(docDescription != null){
				description = description + docDescription.html();
			}
			Element desc2 = document.select(".info-description-frame-inside").first();
			
			if(desc2 != null) {
				String urlDesc2 = HOME_PAGE + "product-description/acom/" + internalPid;
				Document docDescriptionFrame = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, urlDesc2, null, cookies);
				if(docDescriptionFrame != null){
					description = description + docDescriptionFrame.html();
				}
			}
			
			Element elementProductDetails = document.select(".info-section").last();
			if(elementProductDetails != null){
				description = description + elementProductDetails.html();
			}
		}
		
		return description;
	}	

	private Integer crawlStock(JSONObject jsonProduct, String internalId, JSONObject jsonWithStock){
		Integer stock = null;
		
		if(jsonProduct.has("prices")){
			if(jsonProduct.getJSONObject("prices").has(internalId)){
				JSONObject product = jsonProduct.getJSONObject("prices").getJSONObject(internalId);
				
				if(product.has("stock")){
					stock = product.getInt("stock");
				}
			}
		}
		
		if(stock == null && jsonWithStock.has(internalId)){
			JSONObject product = jsonWithStock.getJSONObject(internalId);

			if(product.has("stock")){
				stock = product.getInt("stock");
			}
		}
		
		return stock;
	}

	private JSONObject fetchJSONWithStockInformation(String internalPid) {
		JSONObject apiWithStock = SaopauloB2WCrawlersUtils.fetchAPIInformationsWithOldWay(internalPid, session, cookies, SaopauloB2WCrawlersUtils.AMERICANAS);
		
		return SaopauloB2WCrawlersUtils.assembleJsonProductWithOldWay(apiWithStock, internalPid, session, cookies, SaopauloB2WCrawlersUtils.AMERICANAS);
	}

	private Prices crawlPrices(JSONObject infoProductJson, Float priceBase, String id){
		Prices prices = new Prices();

		if(priceBase != null){
			if(infoProductJson.has("prices")){
				JSONObject pricesJson = infoProductJson.getJSONObject("prices");
				
				if(pricesJson.has(id)){
					JSONObject pricesJsonProduct = pricesJson.getJSONObject(id);

					if(pricesJsonProduct.has("bankTicket")){
						Double price = pricesJsonProduct.getDouble("bankTicket");

						prices.setBankTicketPrice(price.floatValue());
					}

					if(pricesJsonProduct.has("installments")){
						Map<Integer,Float> installmentPriceMap = new HashMap<>();
						JSONArray installmentsArray = pricesJsonProduct.getJSONArray("installments");

						for(int i = 0; i < installmentsArray.length(); i++){
							JSONObject installmentJson = installmentsArray.getJSONObject(i);

							if(installmentJson.has("quantity")){
								Integer installment = installmentJson.getInt("quantity");

								if(installmentJson.has("value")){
									Double valueDouble = installmentJson.getDouble("value");
									Float value = valueDouble.floatValue();

									installmentPriceMap.put(installment, value);
								}
							}
						}

						prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
					}
				}
			}
		}

		return prices;
	}
}
