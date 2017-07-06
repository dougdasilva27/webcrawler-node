package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.Seller;
import models.Util;
import models.prices.Prices;


/************************************************************************************************************************************************************************************
 * Crawling notes (03/11/2016):
 * 
 * 1) For this crawler, we have one url per each sku. At the time this crawler was done, no example of multiple skus
 * in an URL was found. Although there was some indication that this case could occur (eg. analysing the sku json), no
 * concrete example was found. The crawler always tries to get all the data only from the sku URL, without using any endpoint. 
 * We choose to use endpoint, only if the information isn't available anywhere else.
 * 
 * 2) The images are crawled from the sku json, fetched from an endpoint of shopfato.
 * 
 * 3) There is stock information for skus in this ecommerce only in the json from the endpoint.
 * 
 * 4) There is no marketplace in this ecommerce by the time this crawler was made. There are some indications that could exist
 * some other seller than the shopfato, but no concrete example was found. Still we try to get the seller name in the sku page, that
 * is 'shopfato' on all observed cases.  
 * 
 * 6) The sku page identification is done simply looking the URL format.
 * 
 * 7) When a product is unavailable, its price is not shown. But the crawler doesn't consider this
 * as a global rule. It tries to crawl the price the same way in both cases.
 * 
 * 8) There is internalPid for skus in this ecommerce. 
 * 
 * 9) In the json from the endpoint we have the stock from the seller. There is a field with sellers informations, but
 * we didn't saw any example with more than one seller (different from shopfato), for an sku.
 * 
 * 10) We have one method for each type of information for a sku (please carry on with this pattern).
 * 
 * Examples:
 * ...
 * 
 * Optimizations notes:
 * ...
 *
 ************************************************************************************************************************************************************************************/

public class BrasilShopfatoCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.shopfato.com.br/";
	private final String MAIN_SELLER_NAME_LOWER_CASE = "shopfato";

	public BrasilShopfatoCrawler(Session session) {
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

		if ( isProductPage(doc, this.session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// Pid
			String internalPid = crawlInternalPid(doc);

			// Categories
			ArrayList<String> categories = crawlCategories(doc);
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);

			// Descrição
			String description = crawlDescription(doc);

			// Estoque
			Integer stock = null;

			Element elementInternalId = doc.select("#___rc-p-sku-ids").first();
			String[] internalIds = null;
			if (elementInternalId != null) {
				internalIds = elementInternalId.attr("value").trim().split(",");
			}

			for (String internalId : internalIds) {
				// JSON api
				JSONObject productJsonAPI = crawlApi(internalId);				

				// Nome
				String name = productJsonAPI.getString("Name");

				// Imagens
				String primaryImage = crawlPrimaryImage(productJsonAPI);
				String secondaryImages = crawlSecondaryImages(productJsonAPI, primaryImage);

				// Marketplace map
				Map<String, Float> marketplaceMap = extractMarketplace(productJsonAPI);

				// Availability
				boolean available = crawlAvailability(marketplaceMap);

				// Price
				Float price = crawlPrice(marketplaceMap);

				// Marketplace
				Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap, internalId);

				// Prices
				Prices prices = crawlPrices(internalId, price);

				Product product = new Product();
				product.setUrl(this.session.getOriginalURL());
				product.setInternalId(internalId);
				product.setInternalPid(internalPid);
				product.setName(name);
				product.setAvailable(available);
				product.setPrice(price);
				product.setPrices(prices);
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

	private String crawlInternalPid(Document doc) {
		String internalPid = null;
		Element elementPid = doc.select("#___rc-p-id").first();

		if(elementPid != null){
			internalPid = elementPid.attr("value");
		}

		return internalPid;
	}

	private String crawlPrimaryImage(JSONObject productJsonAPI){
		String primaryImage = null;

		if(productJsonAPI.has("Images")){
			JSONArray images = productJsonAPI.getJSONArray("Images");

			for(int i = 0; i < images.length(); i++){
				JSONArray imagesArray = images.getJSONArray(i);
				if(imagesArray.length() > 0){
					JSONObject jsonImage = imagesArray.getJSONObject(0);

					if(jsonImage.has("IsMain")){
						if(jsonImage.getBoolean("IsMain")){
							if(jsonImage.has("Path")){
								primaryImage = modifyImageURL(jsonImage.getString("Path"));
							}

							break;
						}
					}
				}
			}

		}


		return primaryImage;
	}

	private String crawlSecondaryImages(JSONObject productJsonAPI, String primaryImage){
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		if(productJsonAPI.has("Images")){
			JSONArray images = productJsonAPI.getJSONArray("Images");

			for(int i = 0; i < images.length(); i++){
				JSONArray imagesArray = images.getJSONArray(i);
				if(imagesArray.length() > 0){
					JSONObject jsonImage = imagesArray.getJSONObject(0);

					if(jsonImage.has("Path")){
						String urlImage = modifyImageURL(jsonImage.getString("Path"));

						if(!urlImage.equals(primaryImage)){
							secondaryImagesArray.put(urlImage);
						}
					}

				}
			}
		}


		if(secondaryImagesArray.length() > 0){
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}



	private String modifyImageURL(String url) {
		String[] tokens = url.trim().split("/");
		String dimensionImage = tokens[tokens.length-2]; //to get dimension image and the image id

		String[] tokens2 = dimensionImage.split("-"); //to get the image-id
		String dimensionImageFinal = tokens2[0] + "-1000-1000";

		String urlReturn = url.replace(dimensionImage, dimensionImageFinal); //The image size is changed

		return urlReturn;	
	}

	private boolean crawlAvailability(Map<String, Float> marketplaceMap) {
		for (String seller : marketplaceMap.keySet()) {
			if (seller.equals(MAIN_SELLER_NAME_LOWER_CASE)) {
				return true;
			}
		}
		return false;
	}

	private Float crawlPrice(Map<String, Float> marketplaceMap) {
		Float price = null;

		for (String seller : marketplaceMap.keySet()) {
			if (seller.equals(MAIN_SELLER_NAME_LOWER_CASE)) {
				price = marketplaceMap.get(seller);
			}
		}

		return price;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(".bread-crumb li a");

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

	private String crawlDescription(Document doc) {
		String description = "";
		Element elementDescription = doc.select(".x-description-group .x-item .productDescription").first();
		Element elementEspecification = doc.select("#caracteristicas").first();

		if(elementDescription != null) 	description = description + elementDescription.html();
		if(elementEspecification != null) 	description = description + elementEspecification.html();


		return description;
	}

	private JSONObject crawlApi(String internalId){
		JSONObject productJsonAPI = new JSONObject();
		String url = "http://www.shopfato.com.br/produto/sku/" + internalId;

		String response = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, cookies);
		JSONArray jsonArrayAPI = new JSONArray();

		try{
			jsonArrayAPI = new JSONArray(response);
		} catch(Exception e){

		}

		if(jsonArrayAPI.length() > 0){
			productJsonAPI = jsonArrayAPI.getJSONObject(0);
		}

		return productJsonAPI;
	}

	private Map<String, Float> extractMarketplace(JSONObject skuJson) {
		Map<String, Float> marketplaceMap = new HashMap<String, Float>();

		JSONArray skuSellers = skuJson.getJSONArray("SkuSellersInformation");

		for (int i = 0; i < skuSellers.length(); i++) {
			JSONObject seller = skuSellers.getJSONObject(i);
			if(seller.has("Name") && seller.has("IsDefaultSeller")){
				if(seller.getBoolean("IsDefaultSeller")){
					String sellerName = seller.getString("Name").trim().toLowerCase();

					Double priceSellerDouble = seller.getDouble("Price");
					Float sellerPrice = priceSellerDouble.floatValue();

					if(!sellerPrice.equals(0.0f)){
						marketplaceMap.put(sellerName, sellerPrice);
					}
				}
			}
		}

		return marketplaceMap;		
	}

	private Marketplace assembleMarketplaceFromMap(Map<String, Float> marketplaceMap, String internalId) {
		Marketplace marketplace = new Marketplace();

		for (String seller : marketplaceMap.keySet()) {
			if ( !seller.equals(MAIN_SELLER_NAME_LOWER_CASE) ) { 
				Float price = marketplaceMap.get(seller);

				JSONObject sellerJSON = new JSONObject();
				sellerJSON.put("name", seller);
				sellerJSON.put("price", price);
				sellerJSON.put("prices", crawlPrices(internalId, price).toJSON());

				try {
					Seller s = new Seller(sellerJSON);
					marketplace.add(s);
				} catch (Exception e) {
					Logging.printLogError(logger, session, Util.getStackTraceString(e));
				}
			}
		}

		return marketplace;
	}

	private Prices crawlPrices(String internalId, Float price){
		Prices prices = new Prices();

		if(price != null){
			String url = "http://www.shopfato.com.br/productotherpaymentsystems/" + internalId;

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

	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(Document document, String url) {
		return (document.select("#___rc-p-sku-ids").first() != null && url.endsWith("/p"));
	}
}
