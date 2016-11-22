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

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;


/************************************************************************************************************************************************************************************
 * Crawling notes (24/10/2016):
 * 
 * 1) For this crawler, we have one url for mutiples skus.
 *  
 * 2) There is stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is marketplace for mutiple variations in this ecommerce accessed via the url to 
 *  mutiple variations "http://www.submarino.com.br/parceiros/" + internalID + "/?codItemFusion=" + variationID, 
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
 * ex1 (available): http://www.submarino.com.br/produto/127115083/smartphone-moto-g-4-dual-chip-android-6.0-tela-5.5-16gb-camera-13mp-preto
 * ex2 (unavailable): http://www.submarino.com.br/produto/119936092/pneu-toyo-tires-aro-18-235-60r18-107v
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class SaopauloSubmarinoCrawler extends Crawler {
	private final String HOME_PAGE = "http://www.submarino.com.br/";
	private final String MAIN_SELLER_NAME_LOWER = "submarino";

	public SaopauloSubmarinoCrawler(Session session) {
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
		 * esse crawler, o site submarino.com estava fazendo um testeAB
		 * e o termo new, seria de um suposto site novo.
		 */

		BasicClientCookie cookie = new BasicClientCookie("catalogTestAB", "new");
		cookie.setDomain("www.submarino.com.br");
		cookie.setPath("/");
		this.cookies.add(cookie);
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if( isProductPage(session.getOriginalURL(), doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// Api onde se consegue todos os preços
			JSONObject initialJson = getDataLayer(doc);

			// Pega só o que interessa do json da api
			JSONObject infoProductJson = assembleJsonProduct(initialJson);

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
			String description = this.crawlDescription(doc);

			// sku data in json
			Map<String,String> skuOptions = this.crawlSkuOptions(infoProductJson);		

			for (String internalId : skuOptions.keySet()) {

				// InternalId
				String variationInternalID = internalId;

				//variation name
				String variationName = (name + " " + skuOptions.get(internalId)).trim();

				// Marketplace map
				Map<String, Prices> marketplaceMap = this.crawlMarketplace(internalId, internalPid);

				// Assemble marketplace from marketplace map
				JSONArray variationMarketplace = this.assembleMarketplaceFromMap(marketplaceMap);

				// Available
				boolean available = this.crawlAvailability(marketplaceMap);

				// Price
				Float variationPrice = this.crawlPrice(marketplaceMap);

				// Prices 
				Prices prices = crawlPrices(infoProductJson, variationPrice, internalId);

				// Stock
				Integer stock = crawlStock(infoProductJson, internalId);

				Product product = new Product();
				product.setUrl(this.session.getOriginalURL());
				product.setInternalId(variationInternalID);
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

		if (url.startsWith("http://www.submarino.com.br/produto/")) return true;
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

		if (infoProductJson.has("skus")) {
			JSONArray skus = infoProductJson.getJSONArray("skus");

			for(int i = 0; i < skus.length(); i++){
				JSONObject sku = skus.getJSONObject(i);

				if(sku.has("internalId")){
					String internalId = sku.getString("internalId");
					String name = "";

					if(sku.has("variationName")){
						name = sku.getString("variationName");
					}

					skuMap.put(internalId, name);
				}
			}
		}

		return skuMap;
	}

	private JSONObject getDataLayer(Document doc){
		JSONObject skus = new JSONObject();
		Elements scripts = doc.select("script");
		String json = null;

		for (Element e : scripts) {
			json = e.outerHtml();

			if (json.contains("__INITIAL_STATE__")) {
				int x = json.indexOf("_ =") + 3;
				int y = json.indexOf("};", x);

				json = json.substring(x, y+1);

				break;
			}
		}

		try {
			skus = new JSONObject(json);
		} catch (Exception e) {
			skus = new JSONObject();
		}

		return skus;
	}

	private Map<String,Prices> crawlMarketplace(String internalId, String pid) {
		Map<String,Prices> marketplaces = new HashMap<>();

		String url = "http://www.submarino.com.br/parceiros/" + pid + "/" + "?codItemFusion=" + internalId + "&productSku=" + internalId;

		Document doc = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, cookies);
		Elements lines = doc.select(".more-offers-table-row");
		
		if(lines.size() < 1){
			lines = doc.select(".card-seller-offer");
		}

		for(Element linePartner: lines) {
			Prices prices = new Prices();
			Map<Integer,Float> installmentMapPrice = new HashMap<>();

			String partnerName = linePartner.select("img").first().attr("title").trim().toLowerCase();
			Float partnerPrice = Float.parseFloat(linePartner.select(".sales-price").first().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

			installmentMapPrice.put(1, partnerPrice);
			prices.insertBankTicket(partnerPrice);

			Element installmentElement = linePartner.select(".installment-price").first();
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
				if(marketplaces.get(MAIN_SELLER_NAME_LOWER).getRawCardPaymentOptions(Card.VISA.toString()).has("1")){
					Double priceDouble = marketplaces.get(MAIN_SELLER_NAME_LOWER).getRawCardPaymentOptions(Card.VISA.toString()).getDouble("1");
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
		ArrayList<String> categories = new ArrayList<String>();
		if(infoProductJson.has("categories")){
			JSONArray categoriesJson = infoProductJson.getJSONArray("categories");
			for(int i = 0; i < categoriesJson.length(); i++) {
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

	private JSONArray assembleMarketplaceFromMap(Map<String, Prices> marketplaceMap) {
		JSONArray marketplace = new JSONArray();

		for(String sellerName : marketplaceMap.keySet()) {
			if ( !sellerName.equals(MAIN_SELLER_NAME_LOWER) ) {
				JSONObject seller = new JSONObject();
				seller.put("name", sellerName);

				if(marketplaceMap.get(sellerName).getRawCardPaymentOptions(Card.VISA.toString()).has("1")){
					// Pegando o preço de uma vez no cartão
					Double price = marketplaceMap.get(sellerName).getRawCardPaymentOptions(Card.VISA.toString()).getDouble("1");
					Float priceFloat = price.floatValue();				

					seller.put("price", priceFloat); // preço de boleto é o mesmo de preço uma vez.
				}
				seller.put("prices", marketplaceMap.get(sellerName).getPricesJson());

				marketplace.put(seller);
			}
		}

		return marketplace;
	}

	private String crawlDescription(Document document) {
		String description = "";
		Element elementProductDetails = document.select(".card-info").first();
		if(elementProductDetails != null) 	description = description + elementProductDetails.html();

		return description;
	}	

	private Integer crawlStock(JSONObject jsonProduct, String id){
		Integer stock = null;
		
		if(jsonProduct.has("prices")){
			if(jsonProduct.getJSONObject("prices").has(id)){
				JSONObject product = jsonProduct.getJSONObject("prices").getJSONObject(id);
				
				if(product.has("stock")){
					stock = product.getInt("stock");
				}
			}
		}

		return stock;
	}

	/**
	 * Nesse novo site da submarino todas as principais informações dos skus
	 * estão em um json no html, esse json é muito grande, por isso pego somente
	 * o que preciso e coloco em outro json para facilitar a captura de informações
	 * 
	 *{ 
	 *	internalPid = '51612',
	 *	skus:[
	 *		{
	 *			internal_id: '546',
	 *			variationName: '110v'
	 *		}
	 *	],
	 *	images:{
	 *		primaryImage: '123.jpg'.
	 *		secondaryImages: [
	 *			'1.jpg',
	 *			'2.jpg'
	 *		]
	 *	},
	 *	categories:[
	 *		{
	 *			id: '123',
	 *			name: 'cafeteira'
	 *		}
	 *	],
	 *	prices:{
	 *		546:{
	 *			stock: 1706
	 *			bankTicket: 59.86
	 *			installments: [
	 *				{
	 *					quantity: 1,
	 *					value: 54.20
	 *				}
	 *			]
	 *		}
	 *	}
	 *	
	 *}
	 */

	private JSONObject assembleJsonProduct(JSONObject initialJson){
		JSONObject jsonProduct = new JSONObject();

		if(initialJson.has("product")){
			JSONObject productJson = initialJson.getJSONObject("product");

			if(productJson.has("id")){
				jsonProduct.put("internalPid", productJson.getString("id"));
			}

			JSONObject jsonPrices = getJsonPrices(initialJson);
			jsonProduct.put("prices", jsonPrices);

			JSONObject jsonImages = getJSONImages(productJson);
			jsonProduct.put("images", jsonImages);

			JSONArray jsonCategories = getJSONCategories(productJson);
			jsonProduct.put("categories", jsonCategories);

			JSONArray skus = getJSONSkus(initialJson);
			jsonProduct.put("skus", skus);
		}

		return jsonProduct;

	}

	private JSONArray getJSONSkus(JSONObject initialJson){
		JSONArray skus = new JSONArray();

		if(initialJson.has("skus")){
			JSONArray skusJson = initialJson.getJSONArray("skus");

			for(int i = 0; i < skusJson.length(); i++){
				JSONObject skuJson = skusJson.getJSONObject(i);
				JSONObject sku = new JSONObject();

				if(skuJson.has("id")){
					sku.put("internalId", skuJson.getString("id"));

					if(skuJson.has("name")){
						String name = "";

						if(skuJson.has("diffs")){
							JSONArray diffs = skuJson.getJSONArray("diffs");

							for(int j = 0; j < diffs.length(); j++){
								JSONObject variation = diffs.getJSONObject(j);

								if(variation.has("value")){
									name += " " + variation.getString("value").trim();
								}
							}

							sku.put("variationName", name);
						}
					}
				}
				skus.put(sku);
			}
		}

		return skus;
	}

	private JSONArray getJSONCategories(JSONObject productJson){
		JSONArray jsonCategories = new JSONArray();

		if(productJson.has("category")){
			JSONObject category = productJson.getJSONObject("category");

			if(category.has("breadcrumb")){
				jsonCategories = category.getJSONArray("breadcrumb");
			}
		}

		return jsonCategories;
	}

	private JSONObject getJSONImages(JSONObject productJson){
		JSONObject jsonImages = new JSONObject();

		if(productJson.has("images")){
			JSONArray imagesArray = productJson.getJSONArray("images");
			JSONArray secondaryImages = new JSONArray();

			for(int i = 0; i < imagesArray.length(); i++){
				JSONObject images = imagesArray.getJSONObject(i);
				String image = null;

				if(images.has("extraLarge")){
					image = images.getString("extraLarge");
				} else if(images.has("large")){
					image = images.getString("large");
				} else if(images.has("big")){
					image = images.getString("big");
				} else if(images.has("medium")){
					image = images.getString("medium");
				}

				if(i == 0){
					jsonImages.put("primaryImage", image);
				} else {
					secondaryImages.put(image);
				}
			}

			jsonImages.put("secondaryImages", secondaryImages);
		}

		return jsonImages;
	}

	private JSONObject getJsonPrices(JSONObject initialJson){
		JSONObject jsonPrices = new JSONObject();

		if(initialJson.has("offers")){
			JSONArray offersJson = initialJson.getJSONArray("offers");
			JSONObject correctSeller = new JSONObject();
			JSONArray moreQuantityOfInstallments = new JSONArray();

			for(int i = 0; i < offersJson.length(); i++){
				JSONObject jsonOffer = offersJson.getJSONObject(i);
				JSONObject jsonSeller = new JSONObject();
				String idProduct = null;

				if(jsonOffer.has("_embedded")){
					JSONObject embedded = jsonOffer.getJSONObject("_embedded");

					if(embedded.has("seller")){
						JSONObject seller = embedded.getJSONObject("seller");

						if(seller.has("name")){
							if(seller.getString("name").toLowerCase().equals("b2w")){
								correctSeller = jsonOffer;
							}
						}
					}
				}

				if(correctSeller.has("_links")){
					JSONObject links = correctSeller.getJSONObject("_links");

					if(links.has("sku")){
						JSONObject sku = links.getJSONObject("sku");

						if(sku.has("id")){
							idProduct = sku.getString("id");
						}
					}

					if(correctSeller.has("paymentOptions")){
						JSONObject payment = correctSeller.getJSONObject("paymentOptions");

						if(payment.has("BOLETO")){
							JSONObject boleto = payment.getJSONObject("BOLETO");

							if(boleto.has("price")){
								jsonSeller.put("bankTicket", boleto.getDouble("price"));
							}
						}

						if(payment.has("CARTAO_VISA")){
							JSONObject visa = payment.getJSONObject("CARTAO_VISA");

							if(visa.has("installments")){
								JSONArray installments = visa.getJSONArray("installments");
								jsonSeller.put("installments", installments);

								if(installments.length() > moreQuantityOfInstallments.length()){
									moreQuantityOfInstallments = installments;
								}
							}
						}

						if(correctSeller.has("availability")){
							JSONObject availability = correctSeller.getJSONObject("availability");

							if(availability.has("_embedded")){
								JSONObject embeddedStock = availability.getJSONObject("_embedded");

								if(embeddedStock.has("stock")){
									JSONObject jsonStock = embeddedStock.getJSONObject("stock");

									if(jsonStock.has("quantity")){
										jsonSeller.put("stock", jsonStock.getInt("quantity"));
									}
								}
							}
						}
					}

					jsonPrices.put(idProduct, jsonSeller);
				}

			}
		}

		return jsonPrices;
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

						prices.insertBankTicket(price.floatValue());
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
