package br.com.lett.crawlernode.crawlers.corecontent.brasil;

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
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilFastshopCrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

/*************************************************************************************************************************
 * Crawling notes (18/11/2016):
 * 
 * 1) For this crawler, we have multiple skus on the same page.
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 3) There is marketplace information in this ecommerce, but when was made, product with variations has no marketplace info.
 * 4) The sku page identification is done simply looking for an specific html element.
 * 5) Even if a product is unavailable, its price is not displayed if product has no variations.
 * 6) There is internalPid for skus in this ecommerce. The internalPid is a number that is the same for all
 * the variations of a given sku.
 * 7) To get price of variations is accessed a api to get them.
 * 8) Avaiability of product is crawl in another api.
 * 
 * Price crawling notes:
 * 1) For get prices, is parsed a json in the same api of principal price.
 * 
 * Examples:
 * ex1 (available): http://www.fastshop.com.br/webapp/wcs/stores/servlet/ProductDisplay?productId=4611686018426492507
 * ex2 (unavailable): http://www.fastshop.com.br/loja/portateis/eletroportateis-cozinha/cafeteira/maquina-de-cafe-espresso-delonghi-manual-ec220cd-5728-fast
 * ex3 (variations): http://www.fastshop.com.br/loja/ofertas-especiais-top-7/portateis-top/cafeteira-nespresso-modo-04-preta-d40brbkne-fast
 * ex4 (marketplace): http://www.fastshop.com.br/loja/portateis/eletroportateis-cozinha/cafeteira/maquina-de-cafe-espresso-magnifica-s-delonghi-superautomatica-preta-ecam-22-110-b-5575-fast
 *
 * Optimizations notes:
 * No optimizations.
 *
 ***************************************************************************************************************************/

public class BrasilFastshopCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.fastshop.com.br/";

	public BrasilFastshopCrawler(Session session) {
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

		if ( isProductPage(session.getOriginalURL(), doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// Json com informações dos produtos
			JSONArray jsonArrayInfo = BrasilFastshopCrawlerUtils.crawlSkusInfo(doc);
			
			// internal pid
			String internalPid = crawlInternalPid(doc);

			// name
			String name = crawlName(doc);
			
			// Categories
			ArrayList<String> categories = crawlCategories(doc);
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);
			
			// primary image
			String primaryImage = crawlPrimaryImage(doc);

			// secondary images
			String secondaryImages = crawlSecondaryImages(doc);

			// description
			String description = crawlDescription(doc);

			// has variations
			boolean hasVariations = hasVariations(jsonArrayInfo);
			
			// Estoque
			Integer stock = null;
	
			for (int i = 0; i < jsonArrayInfo.length(); i++) {
				JSONObject productInfo = jsonArrayInfo.getJSONObject(i);
				
				// InternalId
				String internalId = crawlInternalId(productInfo);
				
				// Avaiability
				boolean available = crawlAvailability(internalId, hasVariations, doc);
				
				// Name
				String variationName = crawlVariationName(productInfo, name);
				
				// Json prices
				JSONObject jsonPrices = fetchPrices(internalId, available);
				
				// Marketplace
				JSONArray marketplace = crawlMarketPlace(doc, jsonPrices, available);
				
				// boolean
				boolean availableForFastshop = (available && (marketplace.length() < 1));
				
				// Price
				Float price = crawlPrice(jsonPrices, availableForFastshop);
				
				// Prices
				Prices prices = crawlPrices(jsonPrices, price);
				
				Product product = new Product();
				product.setUrl(this.session.getOriginalURL());
				product.setInternalId(internalId);
				product.setInternalPid(internalPid);
				product.setName(variationName);
				product.setPrice(price);
				product.setPrices(prices);
				product.setAvailable(availableForFastshop);
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

	private boolean isProductPage(String url, Document doc) {
		Element elementProductInfoViewer = doc.select("#widget_product_info_viewer").first();
		return elementProductInfoViewer != null;
	}

	private boolean hasVariations(JSONArray jsonInfo){
		return jsonInfo.length() > 1;
	}
	
	private String crawlInternalId(JSONObject jsonInfo){
		String internalId = null;
		
		if (jsonInfo.has("catentry_id")) {
			internalId = jsonInfo.getString("catentry_id").trim();
		}
		
		return internalId;
	}
	
	private String crawlInternalPid(Document document) {
		String internalPid = null;
		Element elementInternalPid = document.select(".newLeftSKU").first();
		if (elementInternalPid != null) {
			internalPid = elementInternalPid.attr("id").split("_")[2].trim();
		}

		return internalPid;
	}

	private boolean crawlAvailability(String internalId, boolean hasVariations, Document doc){
		boolean available = false;
		
		if(hasVariations){
			String url = "http://www.fastshop.com.br/loja/GetInventoryStatusByIDView?storeId=10151&catalogId=11052&langId=-6"
					+ "&hotsite=fastshop&itemId=" + internalId;
			
			String json = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, cookies);
						
			JSONObject jsonStock = new JSONObject();
			try{
				int x = json.indexOf("/*");
				int y = json.indexOf("*/", x + 2);
	
				json = json.substring(x+2, y);
				
				jsonStock = new JSONObject(json);
			} catch(Exception e){
				e.printStackTrace();
			}
			
			if (jsonStock.has("onlineInventory")) {
				JSONObject jsonInventory = jsonStock.getJSONObject("onlineInventory");
				
				if(jsonInventory.has("status")){
					available = jsonInventory.getString("status").trim().toLowerCase().equals("em estoque");
				}
			}
		} else {
			Element buyButton = doc.select("#buy_holder_buy_button").first();
			
			if(buyButton != null){
				available = true;
			}
		}
		
		return available;
	}
	
	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select(".newTitleBar").first();
		if (nameElement != null) {
			name = nameElement.text().trim();
		}

		return name;
	}

	private String crawlVariationName(JSONObject productInfo, String mainName){
		String name = mainName;
		
		if(productInfo.has("Attributes")){
			JSONObject jsonAttributes = productInfo.getJSONObject("Attributes");
			
			if(jsonAttributes.has("Voltagem_110V")){
				if (jsonAttributes.get("Voltagem_110V").equals("1")){
					name += " 110V";
				}
			} else if(jsonAttributes.has("Voltagem_220V")){
				if (jsonAttributes.get("Voltagem_220V").equals("1")){
					name += " 220V";
				}
			}
		}
		
		return name;
	}
	
	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element elementPrimaryImage = document.select(".image_container #productMainImage").first();
		if(elementPrimaryImage != null) {
			primaryImage = "http:" + elementPrimaryImage.attr("src");
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();
		Elements elementsSecondaryImages = document.select(".other_views ul li a img");
		for (Element e : elementsSecondaryImages) {
			String secondaryImage = e.attr("src");
			if( !secondaryImage.contains("PRD_447_1.jpg") ) {
				secondaryImagesArray.put("http:" + e.attr("src"));
			}
		}
		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select("#widget_breadcrumb > ul li a");

		for (int i = 1; i < elementCategories.size(); i++) { // start with index 1 because the first item is home page
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
	
	/**
	 * Json Prices
	 * {
	 * 	priceData":{
			"catalogEntryId":"4611686018426116511",
			"displayPriceProdInactive":"true",
			"offerPrice":"R$ 439,61",
			"offerPriceValue":"439.61",
			"listPrice":"R$ 649,00",
			"installmentPrice":"3x de R$ 153,00 iguais",
			"interestPrice":"juros de 2,19% a.m. e 29,69% a.a.",
			"totalPrice":"Total a prazo: R$ 459,00",
			"displayPriceRange":"",
			"displayLinkWhyInterest":""
	 *		}
	 *}
	 */

	private JSONObject fetchPrices(String internalId, boolean available){
		JSONObject jsonPrice = new JSONObject();
		
		if(available){
			String url = "http://www.fastshop.com.br/loja/AjaxPriceDisplayView?"
					+ "catEntryIdentifier="+ internalId +"&hotsite=fastshop&fromWishList=false&"
					+ "storeId=10151&displayPriceRange=true&displayLinkWhyInterest=true";
	
			String json = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, null);
	
			try{
				int x = json.indexOf("/*");
				int y = json.indexOf("*/", x + 2);
	
				json = json.substring(x+2, y);
				
				jsonPrice = new JSONObject(json);
			} catch(Exception e){
				e.printStackTrace();
			}
		}
		return jsonPrice;
	}
	
	private Float crawlPrice(JSONObject jsonPrices, boolean available){
		Float price = null;
		
		if(available){
			if(jsonPrices.has("priceData")){
				JSONObject jsonCatalog = jsonPrices.getJSONObject("priceData");
	
				if(jsonCatalog.has("totalPrice")){
					String text = jsonCatalog.getString("totalPrice");
					if (!text.isEmpty()) {
						price = MathCommonsMethods.parseFloat(text);
					} else if(jsonCatalog.has("offerPrice")){
						price = Float.parseFloat(jsonCatalog.getString("offerPrice").trim().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
					}
				} else if(jsonCatalog.has("offerPrice")){
					price = Float.parseFloat(jsonCatalog.getString("offerPrice").trim().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
				}
			}
		}

		
		return price;
	}
	
	private Prices crawlPrices(JSONObject jsonPrices, Float price){
		Prices prices = new Prices();
		
		if(price != null){
			Map<Integer,Float> installmentPriceMap = new HashMap<>();
			
			if(jsonPrices.has("priceData")){
				JSONObject priceData = jsonPrices.getJSONObject("priceData");
				
				if(priceData.has("offerPrice")){
					Float offerPrice = MathCommonsMethods.parseFloat(priceData.getString("offerPrice"));
					
					// Preço de boleto e 1 vez no cartão são iguais.
					installmentPriceMap.put(1, offerPrice);
					prices.insertBankTicket(offerPrice);
				}
				
				if(priceData.has("installmentPrice")){
					String text = priceData.getString("installmentPrice").toLowerCase();
					
					if(text.contains("x")){
						int x = text.indexOf("x");
						
						Integer installment = Integer.parseInt(text.substring(0, x));
						Float value = MathCommonsMethods.parseFloat(text.substring(x));
						
						installmentPriceMap.put(installment, value);
					}
				}
				
				prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
				prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
				prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
				prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
				prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
				prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
			}
		}
		
		return prices;
	}
	
	private JSONArray crawlMarketPlace(Document doc, JSONObject jsonPrices, boolean available){
		JSONArray marketplace = new JSONArray();
		
		Element mktElement = doc.select("span.mktPartnerGreen").first();
		if (mktElement != null) {
			JSONObject seller = new JSONObject();
			Float price = crawlPrice(jsonPrices, available);
			Prices prices = crawlPrices(jsonPrices, price);
			
			seller.put("name", mktElement.text().toLowerCase().trim());
			seller.put("price", price);
			seller.put("prices", prices.getPricesJson());
			
			if (available) {
				marketplace.put(seller);
			}
		}
		
		return marketplace;
	}
	
	private String crawlDescription(Document document) {
		String description = "";
		Element productTabContainer = document.select("#productTabContainer").first();
		if (productTabContainer != null) {
			description = productTabContainer.text().trim();
		}
		return description;
	}
}
