package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

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
 * TODO crawler está quebrado para o caso abaixo, precisa revisar
 * With variations: http://www.casaevideo.com.br/loja/liquidificador-com-filtro-370w--capacidade-de-1-5l-e-4-velocidades-brit%C3%A2nia-diamante-black-filter-preto-127v
 *
 * Optimizations notes: no optimization
 *
 ************************************************************************************************************************************************************************************/

public class BrasilCasaevideoCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.casaevideo.com.br/";

	public BrasilCasaevideoCrawler(Session session) {
		super(session);
	}

	@Override
	public void handleCookiesBeforeFetch() {

//		BasicClientCookie cookieWSE = new BasicClientCookie("WC_SESSION_ESTABLISHED", "true");
//		cookieWSE.setDomain("www.casaevideo.com.br");
//		cookieWSE.setPath("/");
//		this.cookies.add(cookieWSE);
//
//		String cookie = DataFetcher.fetchCookie(session, "http://www.casaevideo.com.br/webapp/wcs/stores/servlet/pt/auroraesite", "WC_PERSISTENT", null, 1);
//
//		BasicClientCookie cookieDC = new BasicClientCookie("WC_PERSISTENT", cookie);
//		cookieDC.setDomain("www.casaevideo.com.br");
//		cookieDC.setPath("/");
//		this.cookies.add(cookieDC);
//		
		Map<String,String> cookiesMap = DataFetcher.fetchCookies(session,  "http://www.casaevideo.com.br/loja/liquidificador-com-filtro-370w--capacidade-de-1-5l-e-4-velocidades-brit%C3%A2nia-diamante-black-filter-preto-127v", null, 1);
		
		for(String cookieName : cookiesMap.keySet()){
			if(!cookieName.equals("WC_GENERIC_ACTIVITYDATA") && !cookieName.equals("WC_USERACTIVITY_-1002")){
				BasicClientCookie cookie = new BasicClientCookie(cookieName, cookiesMap.get(cookieName));
				cookie.setDomain("www.casaevideo.com.br");
				cookie.setPath("/");
				this.cookies.add(cookie);
			}
		}
	}

	@Override
	public boolean shouldVisit() {
		String href = session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

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
			
			// Prices
			Prices prices = crawlPrices(doc);

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
					boolean available = crawlAvailabilityVariation(internalID);

					// Creating the product
					Product product = new Product();
					product.setUrl(session.getOriginalURL());
					product.setInternalId(internalID);
					product.setInternalPid(internalPid);
					product.setName(nameVariation);
					product.setPrice(priceVariation);
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
				product.setUrl(session.getOriginalURL());
				product.setInternalId(internalID);
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

	/**
	 * Para pegar o preço é acessado uma api que aparentemente está comentada
	 * dentro dela conseguimos pegar o preço
	 * 
	 * Para acessa-la fazemos uma requisição GET com os seguintes parâmetros
	 * storeId=10152&langId=-6&catalogId=10001&catalogEntryId="+ internalID +"&productId="+ internalPid
	 * 
	 * @param internalPid
	 * @param internalID
	 * @return
	 */
	private JSONObject crawlPriceFromApi(String internalPid, String internalID){
		JSONObject jsonSku = new JSONObject();
		String params = "storeId=10152&langId=-6&catalogId=10001&catalogEntryId="+ internalID +"&productId="+ internalPid;
		String urlPost = "http://www.casaevideo.com.br/webapp/wcs/stores/servlet/GetCatalogEntryDetailsByIDView";

		Map<String,String> headers = new HashMap<>();
		headers.put("Content-Type", "application/x-www-form-urlencoded");

		//String response = DataFetcher.fetchPagePOSTWithHeaders(urlPost, session, params, cookies, 1, headers);
		String response = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, urlPost+"?"+params, null, cookies);
		
		if(response != null){
			if(response.contains("/*") && response.contains("*/")){
				int x = response.indexOf("/*")+2;
				int y = response.indexOf("*/", x);
	
				JSONObject jsonResponse = new JSONObject(response.substring(x, y).trim());
				
				if(jsonResponse.has("catalogEntry")){
					jsonSku = jsonResponse.getJSONObject("catalogEntry");
				}
			}
		}

		return jsonSku;
	}

	private boolean crawlAvailabilityVariation(String internalId) {
		String params = "storeId=10152&catalogId=10001&langId=-6&itemId=" + internalId;
		String urlPost = "http://www.casaevideo.com.br/loja/GetInventoryStatusByIDView";

		Map<String,String> headers = new HashMap<>();
		headers.put("Content-Type", "application/x-www-form-urlencoded");

		//String response = DataFetcher.fetchPagePOSTWithHeaders(urlPost, session, params, cookies, 1, headers);
		String response = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, urlPost+"?"+params, null, cookies).toLowerCase();
		
		if(response != null){
			if(response.contains("sem estoque")){
				return false;
			} else {
				return true;
			}
		}

		return false;
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

	/**
	 * Casos com variação o parcelamento na página não se altera
	 * No caso de um liquidificador citado no começo da classe
	 * O preço do parcelamento não se refere a nenhuma das variações
	 * Mesmo assim é pego, pois é o que aparece para o usuário
	 * 
	 * @param price
	 * @param doc
	 * @return
	 */
	private Prices crawlPrices(Document doc){
		Prices prices = new Prices();

		Element bankTicketElement = doc.select(".widget_catalogentry_installment .input_section div span").first();
		
		if(bankTicketElement != null){
			Float bankTicketPrice = MathCommonsMethods.parseFloat(bankTicketElement.text());
			prices.insertBankTicket(bankTicketPrice);
		}
		
		Elements installments = doc.select(".widget_catalogentry_installment .installmentTable tr");
		Map<Integer,Float> installmentPriceMap = new HashMap<>();
		
		for(Element e : installments){
			String text = e.text().toLowerCase().trim();
			
			if(text.contains("vista")){
				Integer installment = 1;
				int x = text.indexOf("juros");
				
				Float value = MathCommonsMethods.parseFloat(text.substring(0, x));
				installmentPriceMap.put(installment, value);
				
			} else if(text.contains("x")) {
				int x = text.indexOf("x")+1;
				int y = text.indexOf("juros", x);
				
				Integer installment = Integer.parseInt(text.substring(0,x).replaceAll("[^0-9]", ""));
				Float value = MathCommonsMethods.parseFloat(text.substring(x,y));
				
				installmentPriceMap.put(installment, value);
			}
		}
		
		prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
		prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
		prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
		prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
		prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
		prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
	
		
		return prices;
	}
}
