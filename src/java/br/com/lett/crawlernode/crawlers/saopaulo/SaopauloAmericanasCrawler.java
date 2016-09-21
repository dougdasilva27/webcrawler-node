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

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.core.task.Crawler;
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
		String href = this.session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}
	
	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if( isProductPage(session.getUrl(), doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());


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

			// Estoque
			Integer stock = null;

			/* **************************************
			 * crawling data of multiple variations *
			 ****************************************/
			if( hasProductVariations(doc) ) {

				Logging.printLogDebug(logger, session, "Crawling information of more than one product...");


				Elements productVariationElements = this.crawlSkuOptions(doc);
				for(int i = 0; i < productVariationElements.size(); i++) {

					Element sku = productVariationElements.get(i);

					if( !sku.attr("value").equals("") ) { // se tem o atributo value diferente de vazio, então é uma variação de produto

						// InternalId
						String variationInternalID = internalPid + "-" + sku.attr("value");
						
						// Getting name variation
						String variationName = name + " - " + sku.attr("data-value-name");

						// Map of partners
						Map<String, String> partners = getIdsForMarketplaces(doc, sku.attr("value"));
						
						// Marketplace map
						Map<String, Float> marketplaceMap = this.crawlMarketplacesForMutipleVariations(internalPid, sku.attr("value"), partners);

						// Assemble marketplace from marketplace map
						JSONArray variationMarketplace = this.checkMarketPlace(doc, this.assembleMarketplaceFromMap(marketplaceMap), sku);

						// Available
						boolean available = this.crawlAvailability(marketplaceMap);
						
						// Price
						Float variationPrice = this.crawlPrice(marketplaceMap);

						Product product = new Product();
						product.setUrl(this.session.getUrl());
						
						product.setInternalId(variationInternalID);
						product.setInternalPid(internalPid);
						product.setName(variationName);
						product.setPrice(variationPrice);
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
				}
			}


			/* *******************************************
			 * crawling data of only one product in page *
			 *********************************************/
			else {

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
				
				Product product = new Product();
				product.setUrl(this.session.getUrl());
				
				product.setInternalId(internalID);
				product.setInternalPid(internalPid);
				product.setName(name);
				product.setPrice(price);
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
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}
		
		return products;
	}



	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url, Document doc) {
		Element productElement = doc.select("section.a-main-product").first();

		if (productElement != null && url.startsWith("http://www.americanas.com.br/produto/")) return true;
		return false;
	}



	/************************************
	 * Multiple products identification *
	 ************************************/

	private boolean hasProductVariations(Document document) {
		Elements skuChooser = document.select(".mb-sku-choose");
		
		if (skuChooser != null) {

			if (skuChooser.size() > 0) {
		
				Elements variations = document.select(".mb-sku-choose .custom-label-sku input");
				if (variations.size() > 1) {
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		} else {
			return false;
		}
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
			Float partnerPrice = Float.parseFloat(e.select(".bp-link .bp-lnk").first().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));;
			
			marketplace.put(partnerName, partnerPrice);
		}
		
		return marketplace;
	}

	/*********************************
	 * Multiple product page methods *
	 *********************************/


	private Elements crawlSkuOptions(Document document) {
		Elements skuOptions = document.select(".mb-sku-choose .custom-label-sku input");

		return skuOptions;
	}
	
	private Map<String, Float> crawlMarketplacesForMutipleVariations(String internalID, String internalIDVariation, Map<String, String> idsMarketplaces) {
		Map<String, Float>  marketplace = new HashMap<String, Float> ();
		
		String url = "http://www.americanas.com.br/parceiros/" + internalID + "/" + "?codItemFusion=" + internalIDVariation;
	
		Document docMarketplaceInfo = fetchMarketplace(internalID, url);		
		Elements lines = docMarketplaceInfo.select("table.offers-table tbody tr.partner-line");

		for(Element linePartner: lines) {

			String partnerName = linePartner.select(".part-info.part-name").first().text().trim().toLowerCase();
			Float partnerPrice = Float.parseFloat(linePartner.select(".value-prod").first().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));;

			String id = linePartner.attr("data-partner-id").trim();
			
			if(idsMarketplaces.containsKey(id) || (partnerName.equals(MAIN_SELLER_NAME_LOWER) && idsMarketplaces.containsKey(MAIN_SELLER_ID_PARTNER))){
				marketplace.put(partnerName, partnerPrice);
			}

		}

		return marketplace;
	}
	
	private Map<String, String> getIdsForMarketplaces(Document doc, String internalID){
		Map<String, String> idsMarketplaces = new HashMap<String, String>();
		
		Elements ids = doc.select("span.custom-input-radio > input");
		String[] tokens = null;
		
		if(ids.size() >=1){
			for(Element e : ids){
				if(e.attr("value").equals(internalID)){
					tokens = e.attr("data-partners").split(",");
					break;
				}
			}
			
			for(int i = 0; i < tokens.length; i++){
				idsMarketplaces.put(tokens[i].trim(), internalID);
			}
		}
		
		return idsMarketplaces;
	}
	
	/**
	 * o método abaixo deixa o marketplace null,
	 * pois quando uma versão do produto está
	 * indisponível em todas as lojas,
	 * não há seletor de variação do produto no
	 * radio box, e não há uma url diferente
	 * para o marketplace da versão
	 * indisponível
	 * 
	 * @param doc
	 * @param marketplace
	 * @return
	 */
	private JSONArray checkMarketPlace(Document doc, JSONArray marketplace, Element sku){
		
		Elements variationsOnPartnersUrl = doc.select(".mb-sku-choose .custom-input-radio input");
		if (variationsOnPartnersUrl.size() < 1) {
			if (!variationsOnPartnersUrl.first().attr("value").equals(sku.attr("value"))) {
				marketplace = null;
			}
		}
		
		return marketplace;
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
	
	/*@Override
	public void extractInformation(Document doc, String seedId, String url, ProcessedModel truco) {
		super.extractInformation(doc, seedId, url, truco);
		
		Element productElement = doc.select("section.a-main-product").first();

		if(url.startsWith("http://www.americanas.com.br/produto/") && productElement != null) {
			
			Logging.printLogDebug(marketCrawlerLogger, "Página de produto: " + url);

			// ID interno
			String internalId = null;
			Element elementInternalId = doc.select(".a-cod-prod span[itemprop=productID]").first();
			if (elementInternalId != null) {
				internalId = elementInternalId.text().trim();
			}

			// Pid
			String internalPid = internalId;

			// Nome
			String name = null;
			Elements elementName = doc.select(".mp-title h1 span");
			if(elementName.size() > 0) {
				name = elementName.text().replace("'","").replace("’","").trim();
			}

			// Categorias
			Elements elementCategories = doc.select(".breadcrumb span[itemprop=\"name\"]"); 
			String category1 = ""; 
			String category2 = ""; 
			String category3 = "";

			ArrayList<String> categories = new ArrayList<String>();

			for(int i = 0; i < elementCategories.size(); i++) {
				Element e = elementCategories.get(i);
				String tmp = e.text().toString();
				tmp = tmp.replace(">", "");
				categories.add(tmp);
			}

			if(categories.size() >= 3) {
				category1 = categories.get(0);
				category2 = categories.get(1);
				category3 = categories.get(2);
			}
			else if(categories.size() >= 2) {
				category1 = categories.get(0);
				category2 = categories.get(1);
			}
			else if(categories.size() >= 1) {
				category1 = categories.get(0);
			}

			// Imagem primária
			Elements primaryImageElement = doc.select(".mp-picture img");
			String primaryImage = primaryImageElement.attr("src");

			if(primaryImage != null && primaryImage.contains("notFound.gif")) {
				primaryImage = null;
			}

			// Imagens secundárias
			String secondaryImages = null;
try {
//
//			String sql = "SELECT url FROM processed "
//					+ "WHERE "
//					+ 	"market = " + this.db.getMarketId(this.city, this.market) + " AND "
//					+ 	"lrt < '" + minimumDate.toString("yyyy-MM-dd HH:mm:ss.SSSSSS") + "'";;
//
//			ResultSet rs = db.runSqlConsult(sql);
//
//			int count = 0;
//
//			while(rs.next()) {
//				count++;
//				this.controller.addSeed(rs.getString("url"));
//			}
//
//			Logging.printLogDebug(crawlerControllerLogger, city, market, count + " previous ALL product url added to seed list...");
//
//		} catch (SQLException e) {
//			Logging.printLogError(crawlerControllerLogger, city, market, " Erro ao tentar selecionar urls de produtos no banco de dados!");
//			Logging.printLogError(crawlerControllerLogger, city, market, e.getMessage());
//		}
			JSONArray secondaryImagesArray = new JSONArray();
			Elements element_fotosecundaria = doc.select(".a-carousel-item img");

			if(element_fotosecundaria.size()>1){
				for(int i=1; i<element_fotosecundaria.size();i++){
					Element e = element_fotosecundaria.get(i);
					secondaryImagesArray.put(e.attr("src"));
				}

			}
			if(secondarytry {
//
//			String sql = "SELECT url FROM processed "
//					+ "WHERE "
//					+ 	"market = " + this.db.getMarketId(this.city, this.market) + " AND "
//					+ 	"lrt < '" + minimumDate.toString("yyyy-MM-dd HH:mm:ss.SSSSSS") + "'";;
//
//			ResultSet rs = db.runSqlConsult(sql);
//
//			int count = 0;
//
//			while(rs.next()) {
//				count++;
//				this.controller.addSeed(rs.getString("url"));
//			}
//
//			Logging.printLogDebug(crawlerControllerLogger, city, market, count + " previous ALL product url added to seed list...");
//
//		} catch (SQLException e) {
//			Logging.printLogError(crawlerControllerLogger, city, market, " Erro ao tentar selecionar urls de produtos no banco de dados!");
//			Logging.printLogError(crawlerControllerLogger, city, market, e.getMessage());
//		}ImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			String description = "";
			Element elementProductDetails = doc.select("#productdetails").first();
			if(elementProductDetails != null) 	description = description + elementProductDetails.html();

			// Estoque
			Integer stoctry {
//
//			String sql = "SELECT url FROM processed "
//					+ "WHERE "
//					+ 	"market = " + this.db.getMarketId(this.city, this.market) + " AND "
//					+ 	"lrt < '" + minimumDate.toString("yyyy-MM-dd HH:mm:ss.SSSSSS") + "'";;
//
//			ResultSet rs = db.runSqlConsult(sql);
//
//			int count = 0;
//
//			while(rs.next()) {
//				count++;
//				this.controller.addSeed(rs.getString("url"));
//			}
//
//			Logging.printLogDebug(crawlerControllerLogger, city, market, count + " previous ALL product url added to seed list...");
//
//		} catch (SQLException e) {
//			Logging.printLogError(crawlerControllerLogger, city, market, " Erro ao tentar selecionar urls de produtos no banco de dados!");
//			Logging.printLogError(crawlerControllerLogger, city, market, e.getMessage());
//		}k = null;

			// Restrições por categoria
			boolean mustInsert = true;	

			boolean hasMoreProducts = false;

			if (mustInsert) {

				// analisar se existe mais de uma variação do produto
				Elements skuChooser = doc.select(".mb-sku-choose");
				if (skuChooser != null) {

					if (skuChooser.size() > 0) {

						Elements variations = doc.select(".mb-sku-choose .custom-label-sku input");
						if (variations.size() > 1) {

							hasMoreProducts = true;

							for (Element productVariation : variations) {

								// montar o id apendando o atributo value do sku no id que já temos
								String internalIDVariation = internalId + "-" + productVariation.attr("value");

								// apendar variação ao nome do produto
								String variationName = name + " - " + productVariation.attr("data-value-name");

								// disponibilidade estamos considerando que se o id da Americanas consta na lista de
								// parceiros, então ele está disponível. Foi observado que esse comportamento
								// ocorria em todos os casos testados.
								// Existe a chance de não ser sempre dessa forma,
								// mas até o momento, estamos considerando isso.
								boolean availableVariation = false;
								String[] partnersId = productVariation.attr("data-partners").split(",");
								for (int i = 0; i < partnersId.length; i++) {
									if (partnersId[i].equals("02")) {
										availableVariation = true;
										break;
									}
								}

								// marketplace e preço
								Float variationPrice = null;
								JSONArray variationMarketplace = new JSONArray();

								String variationUrlMarketplaceInfo = "http://www.americanas.com.br/parceiros/"
										+ internalId + "/" + "?codItemFusion=" + productVariation.attr("value");
								Document docVariationMarketplaceInfo = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, variationUrlMarketplaceInfo, null, this, null);

								Elements lines = docVariationMarketplaceInfo.select("table.offers-table tbody tr.partner-line");

								for (Element linePartner : lines) {

									String partnerName = linePartner.select(".part-info.part-name").first().text()
											.trim().toLowerCase();
									Float partnerPrice = Float.parseFloat(linePartner.select(".value-prod").first()
											.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "")
											.replaceAll(",", "."));
									

									if (linePartner.hasAttr("data-partner")) {
										variationPrice = partnerPrice;

									} else {

										JSONObject partner = new JSONObject();
										partner.put("name", partnerName);
										partner.put("price", partnerPrice);

										variationMarketplace.put(partner);

									}

								}

								// o código abaixo deixa o marketplace null,
								// pois quando uma versão do produto está
								// indisponível em todas as lojas,
								// não há seletor de variação do produto no
								// radio box, e não há uma url diferente
								// para o
								// marketplace da versão
								// indisponível
								Elements variationsOnPartnersUrl = docVariationMarketplaceInfo.select(".mb-sku-choose .custom-input-radio input");
								if (variationsOnPartnersUrl.size() < 1) {
									if (!variationsOnPartnersUrl.first().attr("value").equals(productVariation.attr("value"))) {
										variationMarketplace = null;
									}
								}

								Product product = new Product();
								product.setSeedId(seedId);
								product.setUrl(url);
								product.setInternalId(internalIDVariation);
								product.setInternalPid(internalPid);
								product.setName(variationName);
								product.setPrice(variationPrice);
								product.setCategory1(category1);
								product.setCategory2(category2);
								product.setCategory3(category3);
								product.setPrimaryImage(primaryImage);
								product.setSecondaryImages(secondaryImages);
								product.setDescription(description);
								product.setStock(stock);
								product.setMarketplace(variationMarketplace);
								product.setAvailable(availableVariation);


								// inserir a variação no banco de dados

								// Se estamos em modo clients e não encontramos as informações principais do produto
								// Então escalonamos a página para ser reprocessada
								if ( this.missingProduct(product) && Main.mode.equals(Main.MODE_INSIGHTS) ) {
									this.crawlerController.scheduleUrlToReprocess(url);
								}

								else {

									// Print information on console
									this.printExtractedInformation(product);

									// Upload image to s3
									if (primaryImage != null && !primaryImage.equals("")) {
										db.uploadImageToAmazon(this, primaryImage, internalIDVariation);
									}

									// Persist information on database
									this.persistInformation(product, this.marketId, truco, url);
									
								}

							}
						}
					}
				}

				// Inserir o produto sem variação normalmente
				if(hasMoreProducts == false) {

					// Marketplace e disponibilidade
					JSONArray marketplace = new JSONArray();
					Float preco = null;
					boolean available = false;

					String urlMarketplaceInfo = "http://www.americanas.com.br/parceiros/" + internalId + "/";

					Document docMarketplaceInfo = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, urlMarketplaceInfo, null, this, null);

					Elements lines = docMarketplaceInfo.select("table.offers-table tbody tr.partner-line");

					for(Element linePartner: lines) {

						String partnerName = linePartner.select(".part-info.part-name").first().text().trim().toLowerCase();
						Float partnerPrice = Float.parseFloat(linePartner.select(".value-prod").first().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));;

						if(linePartner.hasAttr("data-partner")) {

							available = true;
							preco = partnerPrice;

						} else {

							JSONObject partner = new JSONObject();
							partner.put("name", partnerName);
							partner.put("price", partnerPrice);

							marketplace.put(partner);

						}

					}

					// montar o id apendando o atributo value do sku no id que já temos
					Element elementDataSku = doc.select(".mp-pricebox-wrp").first();
					if(elementDataSku != null) {
						internalId = internalId + "-" + elementDataSku.attr("data-sku");
					}
					else {
						elementDataSku = doc.select("meta[itemprop=sku/list]").first();
						if(elementDataSku != null) {
							internalId = internalId + "-" + elementDataSku.attr("content");
						}
					}

					// checagem de disponibilidade pelo form de unavailable product na página principal do produto
					Element elementUnavailableProduct = doc.select(".unavailable-product").first();
					if(elementUnavailableProduct != null) {
						available = false;
						marketplace = null;
						preco = null;
					}

					Product product = new Product();
					product.setSeedId(seedId);
					product.setUrl(url);
					product.setInternalId(internalId);
					product.setInternalPid(internalPid);
					product.setName(name);
					product.setPrice(preco);
					product.setCategory1(category1);
					product.setCategory2(category2);
					product.setCategory3(category3);
					product.setPrimaryImage(primaryImage);
					product.setSecondaryImages(secondaryImages);
					product.setDescription(description);
					product.setStock(stock);
					product.setMarketplace(marketplace);
					product.setAvailable(available);

					// Se estamos em modo clients e não encontramos as informações principais do produto
					// Então escalonamos a página para ser reprocessada
					if ( this.missingProduct(product) && Main.mode.equals(Main.MODE_INSIGHTS) ) {
						
						this.crawlerController.scheduleUrlToReprocess(url);
					
					} else {

						// Print information on console
						this.printExtractedInformation(product);

						// Upload image to s3
						if (product.getPrimaryImage() != null && !product.getPrimaryImage().equals("")) {
							db.uploadImageToAmazon(this, product.getPrimaryImage(), product.getInternalId());
						}

						// Persist information on database
						this.persistInformation(product, this.marketId, truco, url);
						
					}

				}
			}

		} else {
			Logging.printLogTrace(marketCrawlerLogger, "Não é uma página de produto!" + seedId);
			
			if ( Main.mode.equals(Main.MODE_INSIGHTS) ) {
				this.crawlerController.scheduleUrlToReprocess(url);
			}
		}
	}*/

}
