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
 * Crawling notes (22/07/2016):
 * 
 * 1) For this crawler, we have one URL for multiple skus.
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is marketplace information in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking for an specific url element.
 * 
 * 5) If the sku is unavailable, it's price is not displayed.
 * 
 * 6) The internalId and the name of sku, found in json script.
 * 
 * 7) There is internalPid for skus in this ecommerce. The internalPid is a number that is the same for all
 * the variations of a given sku.
 * 
 * 7) The primary image is the first image on the secondary images.
 * 
 * 8) In this ecommerce one marketplace may appear as available in the marketplaces but it is not possible to purchase the sku , then it is registered as unavailable
 * 
 * Examples:
 * ex1 (available): https://www.walmart.com.br/ar-condicionado-split-9-000-btus-samsung-smart-inverter-frio-branco/3260632/pr
 * ex2 (unavailable): https://www.walmart.com.br/frigobar-ngv-10-preto-220v-venax/eletrodomesticos/geladeiras/4039561/pr
 * ex3 (only_marketplace): https://www.walmart.com.br/smartphone-lg-k8-indigo-lgk350ds-abraku-dual-chip-android-6-0-marshmallow-4g-wi-fi-camera-8-mp/4029601/pr
 *
 *
 ************************************************************************************************************************************************************************************/


public class SaopauloSubmarinoCrawler extends Crawler {
	
	public SaopauloSubmarinoCrawler(CrawlerSession session) {
		super(session);
	}

	private final String SUBMARINO_ID = "03";
	private final String HOME_PAGE = "http://www.submarino.com.br/";
	
	@Override
	public boolean shouldVisit() {
		String href = session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
			
			// Pid
			String internalPid = crawlInternalPid(doc);
			
			// Name
			String name = this.crawlName(doc);
			
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
			
			// Variations
			boolean hasVariations = hasVariationsSkus(doc);
			
			// sku data in json
			ArrayList<String> skuOptions = this.crawlSkuOptions(doc);		
			
			if(skuOptions.size() > 0){
				
				// Api onde se consegue todos os preços
				JSONObject apiJson = fetchAPIPrices(internalPid);

				// Pega só o que interessa do json da api
				JSONObject infoProductJson = assembleJsonPrices(apiJson);
				
				for(String internalId : skuOptions){				
					//variation name
					String variation = crawlNameVariation(doc, internalId);
					
					// Name
					String nameVariations = name;
					
					if(variation != null){
						nameVariations += " " + variation;
					}
					
					// Get ids partners
					Map<String, String> mapPartners = crawlIdPartners(doc, internalId);
					
					// Document market places
					Document docMarketplace  = fetchMarketplaceInfoDoc(internalPid, internalId);
	
					// Marketplace map
					Map<String, Float> marketplaceMap = crawlMarketplace(doc, docMarketplace, mapPartners, hasVariations, internalPid);
					
					// Marketplace
					JSONArray marketplace = assembleMarketplaceFromMap(marketplaceMap);
					
					// Availability
					boolean available = crawlAvailability(marketplaceMap);
					
					// Price
					Float price = crawlPrice(marketplaceMap, available);
					
					// Prices 
					Prices prices = crawlPrices(infoProductJson, price, internalId);

					// Stock
					Integer stock = crawlStock(infoProductJson, internalId);
					
					// Creating the product
					Product product = new Product();
					
					product.setUrl(session.getOriginalURL());
					product.setInternalId(internalId);
					product.setInternalPid(internalPid);
					product.setName(nameVariations);
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
				
				//*** Produto Indisponível ***//
				
			} else {
				
				// nesses casos o crawler não estava pegando o nome corretamente
				// coloquei esse seletor aqui para consertar na urgência
				Element unavailableProductName = doc.select("h1.mp-tit-name[itemprop=name]").first();
				if (unavailableProductName != null) name = unavailableProductName.text().trim();
				
				// InternalId
				String internalId = crawlInternalIdSingleProduct(doc);
				
				// Creating the product
				Product product = new Product();
				
				product.setUrl(session.getOriginalURL());
				product.setInternalId(internalId);
				product.setInternalPid(internalPid);
				product.setName(name);
				product.setPrice(null);
				product.setPrices(new Prices());
				product.setAvailable(false);
				product.setCategory1(category1);
				product.setCategory2(category2);
				product.setCategory3(category3);
				product.setPrimaryImage(primaryImage);
				product.setSecondaryImages(secondaryImages);
				product.setDescription(description);
				product.setStock(null);
				product.setMarketplace(new JSONArray());
	
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

	private boolean isProductPage(String url) {
		if ( url.startsWith("http://www.submarino.com.br/produto/") && (!url.contains("?") || url.contains("?attempt")) ) return true;
		return false;
	}
	
	private boolean hasVariationsSkus(Document doc){
		Elements variations = doc.select(".mb-sku-choose label input");
		
		if(variations.size() > 1){
			return true;
		}
		
		return false;
	}
	
	/*******************
	 * General methods *
	 *******************/


	private String crawlInternalIdSingleProduct(Document doc){
		String internalId = null;
		Element internalIdElement = doc.select(".buy-form [data-sku]").first();
		
		if (internalIdElement != null) {
			internalId = internalIdElement.attr("data-sku").trim();
		}
		
		if (internalId == null) {
			internalIdElement = doc.select(".mp-title .a-cod-prod [name=codItemFusionWithoutStock]").first();
			if (internalIdElement != null) {
				internalId = internalIdElement.attr("value").toString().trim();
			}
		}
		
		return internalId;
	}

	
	private String crawlInternalPid(Document doc) {
		String internalPid = null;
		
		Element elementInternalPid = doc.select(".a-cod-prod span[itemprop=productID]").first();
		if (elementInternalPid != null) {
			internalPid = elementInternalPid.text().toString().replaceAll("[()]", "").trim();
		}
		
		return internalPid;
	}
	
	private String crawlName(Document doc) {
		String name = null;
		Element elementName = doc.select("h1.prodTitle span").first();
		
		if(elementName != null){
			name = elementName.text().replace("'","").replace("’","").trim();
		} else {
			elementName = doc.select("h1.mp-tit-name").first();
			
			if(elementName != null){
				name = elementName.text().replace("'","").replace("’","").trim();
			}
		}
	
		return name;
	}

	private String crawlNameVariation(Document doc, String internalId) {
		String name = null;
		Element elementName = doc.select("input[value="+ internalId +"][data-value-name]").first();
		if(elementName == null) elementName = doc.select("option[content="+ internalId +"][data-value-name]").first();
		
		if(elementName != null){
			name = elementName.attr("data-value-name").replace("'","").replace("’","").trim();
		}
	
		return name;
	}
	
	private Float crawlPrice(Map<String, Float> marketplaceMap, boolean available) {
		Float price = null;
		
		if(available){
			for (String partnerName : marketplaceMap.keySet()) {
				if (partnerName.equals("submarino")) { // se o walmart está no mapa dos lojistas, então o produto está disponível
					price = marketplaceMap.get(partnerName);
				}
			}
		}

		return price;
	}
	
	private boolean crawlAvailability(Map<String, Float> marketplaceMap) {
		boolean available = false;
		
		for (String partnerName : marketplaceMap.keySet()) {
			if (partnerName.equals("submarino")) { // se o walmart está no mapa dos lojistas, então o produto está disponível
				available = true;
			}
		}
		
		return available;
	}
	
	private Document fetchMarketplaceInfoDoc(String internalPid, String internalId) {
		
		String urlMarketplaceInfo = "http://www.submarino.com.br/parceiros/"+ internalPid +"/?codItemFusion="+ internalId;
		Document docMarketplace = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, urlMarketplaceInfo, null, null);

		return docMarketplace;
	}
	
	private Map<String, String> crawlIdPartners(Document doc, String internalId){
		Map<String, String> mapPartners = new HashMap<String, String>();
		Element elementName = doc.select("input[value="+ internalId +"][data-partners]").first();
		
		if(elementName == null) elementName = doc.select("option[content="+ internalId +"][data-partners]").first();
		
		if(elementName != null){
			String partners = elementName.attr("data-partners");
		
			if(partners.contains(",")){
				String[] tokens = partners.split(",");
				
				for(int i = 0; i < tokens.length; i++){
					mapPartners.put(tokens[i].trim(), internalId);
				}
			} else {
				mapPartners.put(partners.trim(), internalId);
			}
		}
		
		return mapPartners;
	}

	private Map<String, Float> crawlMarketplace(Document doc, Document marketplaceDocPage, Map<String, String> partnersIds, boolean hasVariations, String pid) {
		Map<String, Float> marketplace = new HashMap<String, Float>();

		if(!hasVariations){
			// pegando o lojista da página principal
			String mainPageSellerName = this.extractMainPageSeller(doc);
			Float mainPageSellerPrice = this.extractMainPagePrice(doc);
			
			if (mainPageSellerName != null && mainPageSellerPrice != null) {
				marketplace.put(mainPageSellerName, mainPageSellerPrice);
			}
		}
		
		if(hasPidInMarketplacePage(marketplaceDocPage, pid)){
			
			Elements lines = marketplaceDocPage.select("table.offers-table tbody tr.partner-line");
	
			for(Element linePartner: lines) {
				String idPartner = linePartner.attr("data-partner-id").trim();
				
				Element partnerNameElement = linePartner.select("td .part-logo a [alt]").first();
				if (partnerNameElement != null) {
					String partnerName = partnerNameElement.attr("alt").trim().toLowerCase();
					Float partnerPrice = Float.parseFloat(linePartner.select(".value-prod").text().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
					
					if (partnerName.contains("submarino")) partnerName = "submarino";
					
					if(partnersIds.containsKey(idPartner) || (partnerName.equals("submarino") && partnersIds.containsKey(SUBMARINO_ID))){
						marketplace.put(partnerName, partnerPrice);
					}
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
	
	private JSONArray assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		JSONArray marketplace = new JSONArray();
		for (String partnerName : marketplaceMap.keySet()) {
			if (!partnerName.equals("submarino")) { 
				
				JSONObject partner = new JSONObject();
				partner.put("name", partnerName);
				partner.put("price", marketplaceMap.get(partnerName));

				marketplace.put(partner);
			}
		}
		
		return marketplace;
	}
	
	private String extractMainPageSeller(Document doc) {
		String seller = null;
		Element elementSeller = doc.select(".mp-delivered-by a").first();
		if (elementSeller != null) {
			seller = elementSeller.text().toString().toLowerCase();
		}
		
		return seller;
	}
	
	private Float extractMainPagePrice(Document doc) {
		Float price = null;

		Element elementPrice = doc.select(".mp-pb-to.mp-price [itemprop=\"price/salesPrice\"]").first();
		if(elementPrice != null) {			
			price = Float.parseFloat(elementPrice.text().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
		}

		return price;
	}

	
	private String crawlPrimaryImage(Document doc) {
		String primaryImage = null;
		Element primaryImageElement = doc.select(".mp-picture figure img").first();
		
		if(primaryImageElement != null){
			String imgUrl = null;

			if(primaryImageElement.attr("data-zoom-image") != null && !primaryImageElement.attr("data-zoom-image").isEmpty() && !primaryImageElement.attr("data-zoom-image").equals("#")) {
				imgUrl = primaryImageElement.attr("data-zoom-image");
			} else if(primaryImageElement.attr("src") != null && !primaryImageElement.attr("src").isEmpty()) {
				imgUrl = primaryImageElement.attr("src");
			}

			if(imgUrl != null && !imgUrl.startsWith("http")) {
				imgUrl = "http:" + imgUrl;
			}
			
			primaryImage = imgUrl;
		}
		
		return primaryImage;
	}

	private String crawlSecondaryImages(Document doc) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();
	
		Elements secondaryImagesElements = doc.select(".carousel-item img");
		
		int count = 0;
		
		for(Element e : secondaryImagesElements){
			count++;
			if(count > 1){ // first image is the primary image
				String imgUrl = null;
	
				if(e.attr("data-szimg") != null && !e.attr("data-szimg").isEmpty() && !e.attr("data-szimg").equals("#")) {
					imgUrl = e.attr("data-szimg");
				} else if(e.attr("src") != null && !e.attr("src").isEmpty()) {
					imgUrl = e.attr("src");
				}
	
				if(imgUrl != null && !imgUrl.startsWith("http")) {
					imgUrl = "http:" + imgUrl;
				}
				
				secondaryImagesArray.put(imgUrl);
			}
		}
		
		
		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}
		
		return secondaryImages;
	}
	

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(".breadcrumb span[itemprop=name]");

		for (int i = 0; i < elementCategories.size(); i++) { 
			categories.add( elementCategories.get(i).text().replace(">", "").trim() );
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
		Element elementDescription = doc.select("#productdetails").first(); 
		
		if(elementDescription != null) 	description = description + elementDescription.html();

		return description;
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
			
			if(json.contains("var crmWA_dataLayer")){
				int x = json.indexOf("push(");
				int y = json.indexOf("});", x + 5);
				
				json = json.substring(x + 5, y+1);
				
				JSONObject skus = new JSONObject(json);
				
				if(skus.has("objSKUsProduto")){
					internalIds = skus.getString("objSKUsProduto");
				}
				
				break;
			}
		}
		
		return internalIds;
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
						
						url = url + "?loja=03";
						
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
		String url = "http://product-v3.submarino.com.br/product?q=itemId:("+ internalPid +")"
				+ "&responseGroups=medium&limit=5&offer.condition=ALL&paymentOptionIds=CARTAO_VISA,BOLETO";

		api = DataFetcher.fetchJSONObject(DataFetcher.GET_REQUEST, session, url, null, cookies);

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