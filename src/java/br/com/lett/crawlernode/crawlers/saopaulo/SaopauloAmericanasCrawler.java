package br.com.lett.crawlernode.crawlers.saopaulo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;


/************************************************************************************************************************************************************************************
 * Crawling notes (19/07/2016):
 * 
 * 1) For this crawler, we have one url for mutiples skus.
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is marketplace for mutiple variations in this ecommerce accessed via the url to 
 *  mutiple variations "http://www.americanas.com.br/parceiros/" + internalID + "/?codItemFusion=" + variationID, 
 *  and for single product is a simply selector in htmlPage.
 *  
 * 4) First to get marketplaces in mutiple variations, is crawled ids from partners in main page looking a simply the html element and put this in map
 * 	  Second accessed the url "http://www.americanas.com.br/parceiros/" + internalID + "/?codItemFusion=" + variationID for getting marketPlaces
 * 
 * 5) The sku page identification is done simply looking the URL format or simply looking the html element.
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
	private final String MAIN_SELLER_ID_PARTNER = "02";

	public SaopauloAmericanasCrawler(CrawlerSession session) {
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

		if( isProductPage(session.getOriginalURL(), doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());


			/* *********************************************************
			 * crawling data common to both the cases of product page  *
			 ***********************************************************/

			// Pid
			String internalPid = this.crawlInternalPid(doc);

			// Name
			String name = this.crawlMainPageName(doc);

			// Categories
			ArrayList<String> categories = this.crawlCategories(doc);
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);

			// Primary image
			String primaryImage = this.crawlPrimaryImage(doc);

			// Secondary images
			String secondaryImages = this.crawlSecondaryImages(doc);

			// Description
			String description = this.crawlDescription(doc);

			// Api onde se consegue todos os preços
			JSONObject apiJson = fetchAPIPrices(internalPid);

			// Pega só o que interessa do json da api
			JSONObject infoProductJson = assembleJsonPrices(apiJson);
			
			/* **************************************
			 * crawling data of multiple variations *
			 ****************************************/
			if( hasProductVariations(doc) ) {

				Logging.printLogDebug(logger, session, "Crawling information of more than one product...");

				// sku data in json
				ArrayList<String> skuOptions = this.crawlSkuOptions(doc);		

				for(String internalId : skuOptions){	
					// InternalId
					String variationInternalID = internalPid + "-" + internalId;

					//variation name
					String variation = crawlNameVariation(doc, internalId);

					// Name
					String nameVariations = name;

					if(variation != null){
						nameVariations += " " + variation;
					}

					// Map of partners
					Map<String, String> partners = getIdsForMarketplaces(doc, internalId);

					// Marketplace map
					Map<String, Float> marketplaceMap = this.crawlMarketplacesForMutipleVariations(internalPid, internalId, partners, internalPid);

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
					product.setName(nameVariations);
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

				// idvariation
				String idVariation = this.crawlInternalIDSingleProduct(doc);

				// InternalId
				String internalID = internalPid + "-" + idVariation;

				// Marketplace map
				Map<String, Float> marketplaceMap = this.crawlMarketplacesForSingleProduct(doc);

				// Assemble marketplace from marketplace map
				JSONArray marketplace = this.assembleMarketplaceFromMap(marketplaceMap);

				// Available
				boolean available = this.crawlAvailability(marketplaceMap);

				// Price
				Float price = this.crawlPrice(marketplaceMap);

				// Prices
				Prices prices = crawlPrices(infoProductJson, price, idVariation);

				// Stock
				Integer stock = crawlStock(infoProductJson, idVariation);
				
				Product product = new Product();
				product.setUrl(this.session.getOriginalURL());
				product.setInternalId(internalID);
				product.setInternalPid(internalPid);
				product.setName(name);
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

		if (url.startsWith("http://www.americanas.com.br/produto/")) return true;
		return false;
	}



	/************************************
	 * Multiple products identification *
	 ************************************/

	private boolean hasProductVariations(Document document) {

		Elements skuChooser = document.select(".mb-sku-choose .custom-label-sku input");

		if (skuChooser.size() > 1) {
			return true;
		} else {
			
			Element sku = document.select("meta[itemprop=sku/list]").first();

			if (sku != null) {
				String ids = sku.attr("content");
				String[] tokens = ids.split(",");

				if(tokens.length > 1){
					return true;
				}

			} 
		}

		return false;

	}

	private String crawlInternalPid(Document document) {
		String internalId = null;
		Element elementInternalId = document.select(".a-cod-prod span[itemprop=productID]").first();
		if (elementInternalId != null) {
			internalId = elementInternalId.text().trim();
		}

		return internalId;
	}


	/*******************************
	 * Single product page methods *
	 *******************************/

	private String crawlInternalIDSingleProduct(Document document) {
		String internalIDMainPage = null;
		Element elementDataSku = document.select(".mp-pricebox-wrp").first();

		if(elementDataSku != null) {
			internalIDMainPage = elementDataSku.attr("data-sku");
		}
		else {
			elementDataSku = document.select("meta[itemprop=sku/list]").first();
			if(elementDataSku != null) {
				internalIDMainPage = elementDataSku.attr("content");
			}
		}

		return internalIDMainPage;
	}

	private Map<String, Float> crawlMarketplacesForSingleProduct(Document doc) {
		Map<String, Float>  marketplace = new HashMap<String, Float> ();
		
		Element elementPartnerName = doc.select(".pickup-store .stock-highlight").first();
		Element elementPartnerPrice = doc.select("span[itemprop=price/salesPrice]").first();
		
		if(elementPartnerName != null && elementPartnerPrice != null){
			String partnerPrincipalName = elementPartnerName.text().toLowerCase().trim();		
			Float partnerPrincipalPrice = Float.parseFloat(elementPartnerPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			
			marketplace.put(partnerPrincipalName, partnerPrincipalPrice);
		}
		
		
		Elements partnersElements = doc.select("ul.bp-list > li > div");
		
		for(Element e : partnersElements){
			
			String partnerName = e.select(".bp-name").text().toLowerCase().trim();
			Float partnerPrice;
			Element priceElement = e.select(".bp-link .bp-lnk").first();
			
			if( partnerName.equals(MAIN_SELLER_NAME_LOWER) && priceElement == null ){
				priceElement = doc.select("span[itemprop=\"price/salesPrice\"]").first();
				partnerPrice = Float.parseFloat(priceElement.attr("content").trim());
			} else {
				partnerPrice = Float.parseFloat(priceElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}
		 
			
			marketplace.put(partnerName, partnerPrice);
		}
		
		return marketplace;
	}
	/*********************************
	 * Multiple product page methods *
	 *********************************/


	private String crawlNameVariation(Document doc, String internalId) {
		String name = null;
		Element elementName = doc.select("input[value="+ internalId +"][data-value-name]").first();
		if(elementName == null) elementName = doc.select("option[content="+ internalId +"][data-value-name]").first();

		if(elementName != null){
			name = elementName.attr("data-value-name").replace("'","").replace("’","").trim();
		}

		return name;
	}

	private ArrayList<String> crawlSkuOptions(Document doc){
		ArrayList<String> internalIds = new ArrayList<String>();
				
		String ids = getIdsFromDataLayer(doc);

		if(ids != null){			
			String[] tokens = ids.split(",");			
			for(int i = 0; i < tokens.length; i++){
				internalIds.add(tokens[i].trim());
			}
		} else {
			Element elementsProductOptions = doc.select("meta[itemprop=\"sku/list\"]").first();

			if(elementsProductOptions != null){
				String[] tokens = elementsProductOptions.attr("content").split(",");			
				for(int i = 0; i < tokens.length; i++){
					internalIds.add(tokens[i].trim());
				}
			}
		}


		return internalIds;
	}

	private String getIdsFromDataLayer(Document doc){
		String internalIds = null;
		Elements scripts = doc.select("script[type=text/javascript]");

		for(Element e : scripts){
			String json = e.html();

			if(json.contains("var objProduct")){
				int x = json.indexOf("objProduct =") + 12;
				int y = json.indexOf("};", x);

				json = json.substring(x, y+1);

				JSONObject skus = new JSONObject(json);

				if(skus.has("objSKUsProduto")){
					internalIds = skus.getString("objSKUsProduto");
				}

				break;
			}
		}

		return internalIds;
	}

	private Map<String, Float> crawlMarketplacesForMutipleVariations(String internalID, String internalIDVariation, Map<String, String> idsMarketplaces, String pid) {
		Map<String, Float>  marketplace = new HashMap<String, Float> ();

		if(idsMarketplaces.size() > 0){
			String url = "http://www.americanas.com.br/parceiros/" + internalID + "/" + "?codItemFusion=" + internalIDVariation;

			Document docMarketplaceInfo = fetchMarketplace(internalID, url);		
			if(hasPidInMarketplacePage(docMarketplaceInfo, pid)){
				
				Elements lines = docMarketplaceInfo.select("table.offers-table tbody tr.partner-line");

				for(Element linePartner: lines) {

					String partnerName = linePartner.select(".part-info.part-name").first().text().trim().toLowerCase();
					Float partnerPrice = Float.parseFloat(linePartner.select(".value-prod").first().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));;

					String id = linePartner.attr("data-partner-id").trim();

					if(idsMarketplaces != null){
						if(idsMarketplaces.containsKey(id) || (partnerName.equals(MAIN_SELLER_NAME_LOWER) && idsMarketplaces.containsKey(MAIN_SELLER_ID_PARTNER))){
							marketplace.put(partnerName, partnerPrice);
						}
					} else {
						marketplace.put(partnerName, partnerPrice);
					}

				}
				
			}  else {
				
				Elements lines = docMarketplaceInfo.select(".card-seller-offer");
				
				for(Element linePartner: lines) {
					String partnerName = linePartner.select(".seller-picture img").first().attr("title").trim().toLowerCase();
					Float partnerPrice = Float.parseFloat(linePartner.select(".sales-price").first().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));;
					
					marketplace.put(partnerName, partnerPrice);	
				}
			}
		}

		return marketplace;
	}

	private boolean hasPidInMarketplacePage(Document doc, String internalPid){
		if(doc != null){
			Element hasPid = doc.select("input[name=codProdFusion]").first();

			if(hasPid != null){
				String pid = hasPid.attr("value").trim();

				if(pid.equals(internalPid)){
					return true;
				}
			}
		}

		return false;
	}

	private Map<String, String> getIdsForMarketplaces(Document doc, String internalID){
		Map<String, String> idsMarketplaces = new HashMap<String, String>();

		Elements ids = doc.select("span.custom-input-radio > input");
		String[] tokens = null;

		if(ids.size() >=1){
			for(Element e : ids){
				if(e.attr("value").equals(internalID)){
					String partnersCode = e.attr("data-partners");

					if(!partnersCode.isEmpty()){
						tokens = e.attr("data-partners").split(",");
					}

					break;
				}
			}

			if(tokens != null){
				for(int i = 0; i < tokens.length; i++){
					idsMarketplaces.put(tokens[i].trim(), internalID);
				}
			}
		}

		return idsMarketplaces;
	}



	/*******************
	 * General methods *
	 *******************/

	private Float crawlPrice(Map<String, Float> marketplaces) {
		Float price = null;

		for (String seller : marketplaces.keySet()) {
			if (seller.equals(MAIN_SELLER_NAME_LOWER)) {
				price = marketplaces.get(seller);
				break;
			}
		}		
		return price;
	}

	private boolean crawlAvailability(Map<String, Float> marketplaces) {
		boolean available = false;

		for (String seller : marketplaces.keySet()) {
			if (seller.equals(MAIN_SELLER_NAME_LOWER)) {
				available = true;
			}
		}

		return available;
	}

	private String crawlPrimaryImage(Document document) {

		String primaryImage = null;

		Element primaryImageElements = document.select(".mp-picture img").first();

		if(primaryImageElements != null){
			if(primaryImageElements.hasAttr("data-zoom-image")){
				primaryImage = primaryImageElements.attr("data-zoom-image");
				if(primaryImage.equals("#")){
					primaryImage = primaryImageElements.attr("src");
				}
			} else {
				primaryImage = primaryImageElements.attr("src");
			}
	
			if(primaryImage != null && primaryImage.contains("notFound.gif")) {
				primaryImage = null;
			}
		}

		return primaryImage;
	}

	private String crawlMainPageName(Document document) {
		String name = null;
		Elements elementName = document.select(".mp-title h1 span");
		if(elementName.size() > 0) {
			name = elementName.text().replace("'","").replace("’","").trim();
		}
		return name;
	}

	private ArrayList<String> crawlCategories(Document document) {
		Elements elementCategories = document.select(".breadcrumb span[itemprop=\"name\"]");
		ArrayList<String> categories = new ArrayList<String>();

		for(int i = 0; i < elementCategories.size(); i++) {
			Element e = elementCategories.get(i);
			String tmp = e.text().toString();

			tmp = tmp.replace(">", "");
			categories.add(tmp);
		}

		return categories;
	}

	private String getCategory(ArrayList<String> categories, int n) {
		if (n < categories.size()) {
			return categories.get(n);
		}

		return "";
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;

		JSONArray secondaryImagesArray = new JSONArray();
		Elements elementFotoSecundaria = document.select(".a-carousel-item img");

		if (elementFotoSecundaria.size()>1) {
			for(int i = 1; i < elementFotoSecundaria.size(); i++) { //starts with index 1 because de primary image is the first image
				Element e = elementFotoSecundaria.get(i);

				if(e.hasAttr("data-szimg"))	secondaryImagesArray.put(e.attr("data-szimg"));
				else 						secondaryImagesArray.put(e.attr("src"));
			}

		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}	

	private JSONArray assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		JSONArray marketplace = new JSONArray();

		for(String sellerName : marketplaceMap.keySet()) {
			if ( !sellerName.equals(MAIN_SELLER_NAME_LOWER) ) {
				JSONObject seller = new JSONObject();
				seller.put("name", sellerName);
				seller.put("price", marketplaceMap.get(sellerName));

				marketplace.put(seller);
			}
		}

		return marketplace;
	}

	private Document fetchMarketplace(String internalID, String url) {
		Document docMarketplaceInfo = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, null);

		return docMarketplaceInfo;
	}


	private String crawlDescription(Document document) {
		String description = "";
		Element elementProductDetails = document.select("#productdetails").first();
		if(elementProductDetails != null) 	description = description + elementProductDetails.html();

		return description;
	}	
	
	private Integer crawlStock(JSONObject jsonProduct, String id){
		Integer stock = null;
		
		if(jsonProduct.has(id)){
			JSONObject product = jsonProduct.getJSONObject(id);
			
			if(product.has("stock")){
				stock = product.getInt("stock");
			}
		}
		
		return stock;
	}

	/**
	 * Para pegar todos os preços acessamos uma api que retorna um json
	 * com todos preços de todos os marketplaces, depois pegamos somente
	 * as parcelas e o preço no boleto do shoptime. Em seguida coloco somente
	 * o id do produto com seu preço no boleto e as parcelas no cartão, também coloco
	 * as parcelas do produto com maior quantidade de parcelas, pois foi verificado
	 * que produtos com variações, a segunda variação está vindo com apenas
	 * uma parcela no json da api. Vale lembrar que pegamos as parcelas do Cartão VISA. ex:
	 * 
	 * Endereço api: 
	 * http://product-v3.shoptime.com.br/product?q=itemId:(125628846)&responseGroups=medium&limit=5&offer.condition=ALL&paymentOptionIds=CARTAO_VISA,BOLETO
	 * 
	 * Parse Json:
	 * http://json.parser.online.fr/
	 * 
	 *{ 125628854":{
	 *	"installments":[
	 *		{
	 *			"interestRate":0,
	 * 			"total":1699,
	 *			"quantity":1,
	 *			"interestAmount":0,
	 *			"value":1699,
	 *			"annualCET":0
	 *		},
	 *		{...},
	 *		{...}
	 *	],
	 *	"bankTicket":1699,
	 *	"stock":72
	 *	},
	 *  "moreQuantityOfInstallments":[
	 *	{
	 *		"interestRate":0,
	 *		"total":1699,
	 *		"quantity":1,
	 *		"interestAmount":0,
	 *		"value":1699,
	 *		"annualCET":0
	 *	}
	 *}
	 */
	
	private JSONObject assembleJsonPrices(JSONObject api){
		JSONObject jsonPrices = new JSONObject();

		if(api.has("products")){
			JSONObject productJson = api.getJSONArray("products").getJSONObject(0);

			if(productJson.has("offers")){
				JSONArray offersJson = productJson.getJSONArray("offers");
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
						jsonPrices.put("moreQuantityOfInstallments", moreQuantityOfInstallments);
					}

				}
				
				if(jsonPrices.has("moreQuantityOfInstallments")){
					if(jsonPrices.getJSONArray("moreQuantityOfInstallments").length() == 1){
						String url = session.getOriginalURL();
						
						if(url.contains("?")){
							int x = url.indexOf("?");
							
							url = url.substring(0, x);
						}
						
						url = url + "?loja=02";
						
						Document doc = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, cookies);	
						Element parcels = doc.select(".all-installment").first();
						
						if(parcels != null){
							Elements installmentsElements = parcels.select("> .lp tr");
							JSONArray installmentsJsonArray = new JSONArray();
							
							for(Element e : installmentsElements){
								JSONObject jsonTemp = new JSONObject();
								Element parcel = e.select(".qtd-parcel").first();
								
								if(parcel != null){
									Integer installment = Integer.parseInt(parcel.text().replaceAll("[^0-9]", "").trim());
									
									Element values = e.select(".parcel").first();
									
									if(values != null){
										Float priceInstallment = Float.parseFloat(values.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());
										
										jsonTemp.put("quantity", installment);
										jsonTemp.put("value", priceInstallment);
										installmentsJsonArray.put(jsonTemp);
									}
								}
							}
							
							jsonPrices.put("installmentsMainPage", installmentsJsonArray);
						}
					}
				}

			}
		}
		
		return jsonPrices;
	}

	private JSONObject fetchAPIPrices(String internalPid){
		JSONObject api = new JSONObject();
		
		if(internalPid != null){
			String url = "http://product-v3.americanas.com.br/product?q=itemId:("+ internalPid +")"
					+ "&responseGroups=medium&limit=5&offer.condition=ALL&paymentOptionIds=CARTAO_VISA,BOLETO";
	
			api = DataFetcher.fetchJSONObject(DataFetcher.GET_REQUEST, session, url, null, cookies);
		}

		return api;
	}

	private Prices crawlPrices(JSONObject apiJson, Float priceBase, String id){
		Prices prices = new Prices();

		if(priceBase != null){
			JSONArray moreQuantityOfInstallments = new JSONArray();
			if(apiJson.has("moreQuantityOfInstallments")){
				moreQuantityOfInstallments = apiJson.getJSONArray("moreQuantityOfInstallments");
			}
			
			JSONArray installmentsMainPage = new JSONArray();
			if(apiJson.has("installmentsMainPage")){
				installmentsMainPage = apiJson.getJSONArray("installmentsMainPage");
			}
			
			if(apiJson.has(id)){
				JSONObject pricesJson = apiJson.getJSONObject(id);
				
				if(pricesJson.has("bankTicket")){
					Double price = pricesJson.getDouble("bankTicket");
		
					prices.insertBankTicket(price.floatValue());
				}
		
				if(pricesJson.has("installments")){
					Map<Integer,Float> installmentPriceMap = new HashMap<>();
					JSONArray installmentsArray = pricesJson.getJSONArray("installments");
					
					if(installmentsArray.length() == 1 && installmentsArray.length() < moreQuantityOfInstallments.length()){
						installmentsArray = moreQuantityOfInstallments;
					}
					
					if(installmentsArray.length() == 1 && installmentsArray.length() < installmentsMainPage.length()){
						for(int i = 0; i < installmentsMainPage.length(); i++){
							installmentsArray.put(installmentsMainPage.getJSONObject(i));
						}
					}
					
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
		
					prices.insertCardInstallment(Prices.VISA, installmentPriceMap);
				}
			}
		}

		return prices;
	}

}
