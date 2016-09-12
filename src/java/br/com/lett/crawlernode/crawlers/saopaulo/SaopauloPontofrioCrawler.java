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

import br.com.lett.crawlernode.kernel.fetcher.DataFetcher;
import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.Crawler;
import br.com.lett.crawlernode.kernel.task.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;

/************************************************************************************************************************************************************************************
 * Crawling notes (18/08/2016):
 * 
 * 1) For this crawler, we have one url for mutiples skus.
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is marketplace information in this ecommerce. 
 *  
 * 4) To get marketplaces is accessed the url "url + id +/lista-de-lojistas.html" for getting marketPlaces
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
 * 11) In some cases in products with variations, internalId is not the same id to put in url from marketplaces, so is crawl anothers ids in main page.
 * 
 * 12)Em casos de produtos com variação de voltagem, os ids que aparecem no seletor de internalIDs não são os mesmos 
 *	para acessar a página de marketplace. Em alguns casos esses ids de página de marketplace aparacem 
 *	na url e no id do produto (Cod item ID). Nesses casos eu pego esses dois ids e acesso a página de marketplace de cada um, 
 *	e nessa página pego o nome do produto e o document dela e coloco em um mapa. Quando entro na variação eu pego esse mapa
 *	e proucuro o document com o nome do produto. 
 *	
 * 13)Quando os ids da url e o (Cod Item ID) são iguais, pego os mesmos marketplaces para as variações, vale lembrar que esse 
 *	market a página de marketplace é a mesma para as variações.
 *	
 * 14)Quando as variações não são de voltagem, os ids para entrar na página de marketplace aparaecem no seletor 
 *	de internalID das variações, logo entro diretamente nas páginas de marketplaces de cada um normalmente.
 *	
 * 15)Para produtos sem variações, apenas troco o final da url de “.html” para “/lista-de-lojistas.html”.
 * 
 * Examples:
 * ex1 (available): http://www.pontofrio.com.br/Eletronicos/Televisores/SmartTV/Smart-TV-LED-39-HD-Philco-PH39U21DSGW-com-Conversor-Digital-MidiaCast-PVR-Wi-Fi-Entradas-HDMI-e-Endrada-USB-7323247.html
 * ex2 (unavailable): http://www.pontofrio.com.br/Eletroportateis/Cafeteiras/CafeteirasEletricas/Cafeteira-Eletrica-Philco-PH16-Vermelho-Aco-Escovado-4451511.html
 * ex3 (only_marketplace): http://www.pontofrio.com.br/CamaMesaBanho/ToalhaAvulsa/Banho/Toalha-de-Banho-Desiree-Pinta-e-Borda---Santista-4811794.html
 * ex4 (Product with marketplace special): http://www.pontofrio.com.br/Eletrodomesticos/FornodeMicroondas/Microondas-30-Litros-Midea-Liva-Grill---MTAG42-7923503.html
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class SaopauloPontofrioCrawler extends Crawler {

	private final String MAIN_SELLER_NAME_LOWER = "pontofrio";
	private final String HOME_PAGE = "http://www.pontofrio.com.br/";
	
	public SaopauloPontofrioCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();
		
		if( isProductPage(doc, session.getUrl()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			// Pegando url padrão no doc da página, para lidar com casos onde tem url em formato diferente no banco
			String modifiedURL = makeUrlFinal(session.getUrl());
			
			// Variations
			boolean hasVariations = hasProductVariations(doc);
			
			// Pid
			String internalPid = this.crawlInternalPid(doc);

			// Check if all products in page are unnavailable
			boolean unnavailableForAll = this.checkUnnavaiabilityForAll(doc);

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
			String secondaryImages = this.crawlSecondaryImages(doc, unnavailableForAll);

			// Description
			String description = this.crawlDescription(doc);

			// Estoque
			Integer stock = null;

			/* **************************************
			 * crawling data of multiple variations *
			 ****************************************/
			if( hasVariations && !unnavailableForAll) {

				Elements productVariationElements = this.crawlSkuOptions(doc);

				// Array de ids para url para pegar marketplace
				List<String> idsForUrlMarketPlace = this.identifyIDForUrlLojistas(modifiedURL, doc, productVariationElements);

				// Pegando os documents das páginas de marketPlace para produtos especiais
				Map<String, Document> documentsMarketPlaces = this.fetchDocumentMarketPlacesToProductSpecial(idsForUrlMarketPlace, modifiedURL);

				for(int i = 0; i < productVariationElements.size(); i++) {

					Element sku = productVariationElements.get(i);

					// InternalId
					String variationInternalID = sku.attr("value");

					// Getting name variation
					String variationName = makeVariationName(name, sku).trim();

					// Marketplace map
					Map<String, Float> marketplaceMap = this.crawlMarketplacesForMutipleVariations(modifiedURL, sku, documentsMarketPlaces, variationName);

					// Assemble marketplace from marketplace map
					JSONArray marketplace = this.assembleMarketplaceFromMap(marketplaceMap);

					// Available
					boolean available = this.crawlAvailability(marketplaceMap);

					// Price
					Float variationPrice = this.crawlPrice(marketplaceMap);

					Product product = new Product();
					product.setSeedId(session.getSeedId());
					product.setUrl(modifiedURL);
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
					product.setMarketplace(marketplace);
					product.setAvailable(available);

					products.add(product);

				}
			}


			/* *******************************************
			 * crawling data of only one product in page *
			 *********************************************/
			else {

				// InternalId
				String internalID = this.crawlInternalIDSingleProduct(doc);

				// Marketplace map
				Map<String, Float> marketplaceMap = this.crawlMarketplacesForSingleProduct(doc, modifiedURL, unnavailableForAll);

				// Assemble marketplace from marketplace map
				JSONArray marketplace = this.assembleMarketplaceFromMap(marketplaceMap);

				// Available
				boolean available = this.crawlAvailability(marketplaceMap);

				// Price
				Float price = this.crawlPrice(marketplaceMap);

				Product product = new Product();
				product.setSeedId(session.getSeedId());
				product.setUrl(modifiedURL);
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

	private boolean isProductPage(Document doc, String url) {
		Element productElement = doc.select(".produtoNome h1 span").first();

		if (productElement != null) return true;
		return false;
	}



	/************************************
	 * Multiple products identification *
	 ************************************/

	private boolean hasProductVariations(Document document) {
		Elements skuChooser = document.select(".produtoSku option[value]:not([value=\"\"])");

		if (skuChooser.size() > 1) {
			if(skuChooser.size() == 2){
				String prodOne = skuChooser.get(0).text();
				String prodTwo = skuChooser.get(1).text();

				if(prodOne.equals(prodTwo)){
					return false;
				}
			}
			return true;
		} 

		return false;

	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;
		Elements elementInternalId = document.select("script[type=text/javascript]");

		String idenfyId = "idProduct";

		for(Element e : elementInternalId){
			String script = e.outerHtml();

			if(script.contains(idenfyId)){
				script = script.replaceAll("\"", "");

				int x = script.indexOf(idenfyId);
				int y = script.indexOf(",", x + idenfyId.length());

				internalPid = script.substring(x + idenfyId.length(), y).replaceAll("[^0-9]", "").trim();
			}
		}


		return internalPid;
	}


	/*******************************
	 * Single product page methods *
	 *******************************/

	private String crawlInternalIDSingleProduct(Document document) {
		String internalIDMainPage = null;
		Element elementDataSku = document.select("#ctl00_Conteudo_hdnIdSkuSelecionado").first();

		if(elementDataSku != null) {
			internalIDMainPage = elementDataSku.attr("value");
		}

		return internalIDMainPage;
	}

	private Map<String, Float> crawlMarketplacesForSingleProduct(Document doc, String url, boolean unnavailableForAll) {
		Map<String, Float>  marketplace = new HashMap<String, Float> ();

		if(!unnavailableForAll){
			Document docMarketplaceInfo = fetchDocumentMarketPlace(null, url);

			Elements lines = docMarketplaceInfo.select("table#sellerList tbody tr");

			for(Element linePartner: lines) {

				String partnerName = linePartner.select("a.seller").first().text().trim().toLowerCase();
				Float partnerPrice = Float.parseFloat(linePartner.select(".valor").first().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));;

				marketplace.put(partnerName, partnerPrice);

			}
		}

		return marketplace;
	}

	/*********************************
	 * Multiple product page methods *
	 *********************************/

	private Map<String, Document> fetchDocumentMarketPlacesToProductSpecial(List<String> idsForUrlMarketPlace, String url){
		Map<String, Document> documentsMarketPlaces = new HashMap<>();

		if(idsForUrlMarketPlace != null){
			Document docMarketPlaceProdOne = this.fetchDocumentMarketPlace(idsForUrlMarketPlace.get(0), url);
			String[] namev = docMarketPlaceProdOne.select("#ctl00_Conteudo_lnkProdutoDescricao").text().split("-");
			documentsMarketPlaces.put(namev[namev.length-1].trim(),docMarketPlaceProdOne);

			if(idsForUrlMarketPlace.size() == 2){
				Document docMarketPlaceProdTwo = this.fetchDocumentMarketPlace(idsForUrlMarketPlace.get(1), url);
				String[] namev2 = docMarketPlaceProdTwo.select("#ctl00_Conteudo_lnkProdutoDescricao").text().split("-");
				documentsMarketPlaces.put(namev2[namev2.length-1].trim(), docMarketPlaceProdTwo);
			} else {
				documentsMarketPlaces.put(namev[namev.length-1].trim(), docMarketPlaceProdOne);
			}
		}

		return documentsMarketPlaces;
	}

	private List<String> identifyIDForUrlLojistas(String url, Document doc, Elements skuOptions){
		List<String> ids = new ArrayList<>();

		// first ID
		String[] tokens = url.split("-");
		String firstIdMainPage = tokens[tokens.length-1].replaceAll("[^0-9]", "").trim();

		// second ID
		String secondIdMainPage = doc.select("#ctl00_Conteudo_hdnIdSkuSelecionado").first().attr("value").trim();

		ids.add(firstIdMainPage);

		// se os ids forem iguais, não há necessidade de enviar os 2
		if(!firstIdMainPage.equals(secondIdMainPage)){
			ids.add(secondIdMainPage);
		}

		// Ids variations
		boolean correctId = false;
		for(Element e : skuOptions){
			String id = e.attr("value").trim();

			if(id.equals(firstIdMainPage) || id.equals(secondIdMainPage)){
				correctId = true;
				break;
			} 
		}

		// se os ids estiverem corretos, não há necessidade de retornar nada
		if(correctId) return null;		

		return ids;
	}

	private Document fetchDocumentMarketPlace(String id, String url){
		Document doc = new Document(url);

		if(id != null){
			String[] tokens = url.split("-");
			String urlMarketPlace = url.replace(tokens[tokens.length-1], id + "/lista-de-lojistas.html");

			doc = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, urlMarketPlace, null, null);
		} else {
			String urlMarketPlace = url.replace(".html", "/lista-de-lojistas.html"); 
			doc = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, urlMarketPlace, null, null);
		}

		return doc;
	}

	private Elements crawlSkuOptions(Document document) {
		Elements skuOptions = document.select(".produtoSku option[value]:not([value=\"\"])");

		return skuOptions;
	}

	private Map<String, Float> crawlMarketplacesForMutipleVariations(String url, Element sku, Map<String, Document> documentsMarketPlaces, String name ) {
		Map<String, Float>  marketplace = new HashMap<String, Float> ();

		if(!sku.text().contains("Esgotado")){
			Document docMarketplaceInfo = new Document(url);
			if(documentsMarketPlaces.size() > 0){
				
				if(documentsMarketPlaces.size() == 1){
					for(String key : documentsMarketPlaces.keySet()){
						docMarketplaceInfo = documentsMarketPlaces.get(key);
					}
				} else {
					String[] tokens = name.split("-");
					String nameV = tokens[tokens.length-1].trim();
					
					if(documentsMarketPlaces.containsKey(nameV)){
						docMarketplaceInfo = documentsMarketPlaces.get(nameV);
					}
				}
				
			} else {
				docMarketplaceInfo = fetchDocumentMarketPlace(sku.attr("value"), url);
			}

			Elements lines = docMarketplaceInfo.select("table#sellerList tbody tr");

			for(Element linePartner: lines) {

				String partnerName = linePartner.select("a.seller").first().text().trim().toLowerCase();
				Float partnerPrice = Float.parseFloat(linePartner.select(".valor").first().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));;

				marketplace.put(partnerName, partnerPrice);

			}
		}

		return marketplace;
	}


	private String makeVariationName(String name, Element sku){
		String nameV = name;

		String[] tokens = sku.text().split("\\|");
		String variation = tokens[0].trim();

		if(!variation.isEmpty()){
			nameV += " - " + variation;
		}

		return nameV;
	}

	/*******************
	 * General methods *
	 *******************/

	private boolean checkUnnavaiabilityForAll(Document doc){

		return (doc.select(".alertaIndisponivel").first() != null);
	}

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

		Element primaryImageElement = document.select(".carouselBox .thumbsImg li a").first();

		if(primaryImageElement != null){
			if(!primaryImageElement.attr("rev").isEmpty() && primaryImageElement.attr("rev").startsWith("http")){
				primaryImage = primaryImageElement.attr("rev");
			} else {
				primaryImage = primaryImageElement.attr("href");
			}
		} else {
			primaryImageElement = document.select("#divFullImage a img").first();
			
			if(primaryImageElement != null){
				primaryImage = primaryImageElement.attr("src");
			}
		}


		return primaryImage;
	}

	private String crawlSecondaryImages(Document document, boolean unnavailableForAll) {
		String secondaryImages = null;

		JSONArray secondaryImagesArray = new JSONArray();


		if(!unnavailableForAll){
			Elements elementFotoSecundaria = document.select(".carouselBox .thumbsImg li a");

			if (elementFotoSecundaria.size()>1) {
				for(int i = 1; i < elementFotoSecundaria.size(); i++) { //starts with index 1 because de primary image is the first image
					Element e = elementFotoSecundaria.get(i);

					if(!e.attr("rev").isEmpty() && e.attr("rev").startsWith("http")){
						secondaryImagesArray.put(e.attr("rev"));
					} else {
						secondaryImagesArray.put(e.attr("href"));
					}
				}

			}
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}	

	private String crawlMainPageName(Document document) {
		String name = null;
		Elements elementName = document.select(".produtoNome h1 b");

		if(elementName.size() > 0) {
			name = elementName.text().replace("'","").replace("’","").trim();
		}

		return name;
	}

	private ArrayList<String> crawlCategories(Document document) {
		Elements elementCategories = document.select(".breadcrumb a");
		ArrayList<String> categories = new ArrayList<String>();

		for(int i = 1; i < elementCategories.size(); i++) { // starts with index 1 because the first item is the home page
			Element e = elementCategories.get(i);
			String tmp = e.text().toString();

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


	private String crawlDescription(Document document) {
		String description = "";
		Element elementProductDetails = document.select("#detalhes").first();
		if(elementProductDetails != null) 	description = description + elementProductDetails.html();

		return description;
	}

	private String makeUrlFinal(String url){
		String urlFinal = url;

		if(url.contains("?")){
			int x = url.indexOf("?");

			urlFinal = url.substring(0, x);
		}

		return urlFinal;
	}

}



//package br.com.lett.crawlers.saopaulo;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import org.json.JSONArray;
//import org.json.JSONObject;
//import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
//import org.jsoup.nodes.Element;
//import org.jsoup.select.Elements;
//import org.openqa.selenium.WebElement;
//
//import br.com.lett.Main;
//import br.com.lett.crawlers.models.Product;
//import br.com.lett.crawlers.requestutils.DataFetcher;
//import br.com.lett.crawlers.models.ProcessedModel;
//import br.com.lett.util.MarketCrawler;
//import edu.uci.ics.crawler4j.crawler.Page;
//import edu.uci.ics.crawler4j.url.WebURL;
//
//public class SaopauloPontofrioCrawler extends MarketCrawler {
//
//	private final String MAIN_SELLER_NAME_LOWER = "pontofrio";
//
//
//	@Override
//	public boolean shouldVisit(Page referringPage, WebURL url) {
//		String href = url.getURL().toLowerCase();
//		return !FILTERS.matcher(href).matches() && href.startsWith("http://www.pontofrio.com.br/");
//	}
//
//
//	@Override
//	public void extractInformation(Document doc, String seedId, String url, ProcessedModel truco) {
//		super.extractInformation(doc, seedId, url, truco);
//
//		if( isProductPage(doc) ) {
//
//			this.logInfo("Product page detected: " + url);
//
//
//			/* *********************************************************
//			 * crawling data common to both the cases of product page  *
//			 ***********************************************************/
//
//			// InternalId
//			String internalID = this.crawlInternalIdSingleProduct(doc);
//
//			// Pid
//			String internalPid = internalID;
//
//			// Name
//			String name = this.crawlMainPageName(doc);
//
//			// Price
//			Float price = this.crawlMainPagePrice(doc);
//
//			// Categories
//			ArrayList<String> categories = this.crawlCategories(doc);
//			String category1 = getCategory(categories, 0);
//			String category2 = getCategory(categories, 1);
//			String category3 = getCategory(categories, 2);
//
//			// Primary image
//			String primaryImage = this.crawlPrimaryImage(doc);
//
//			// Secondary images
//			String secondaryImages = this.crawlSecondaryImages(doc, primaryImage);
//
//			// Description
//			String description = this.crawlDescription(doc);
//
//			// Estoque
//			Integer stock = null;
//			
//			
//
//
//			/* **************************************
//			 * crawling data of multiple variations *
//			 ****************************************/
//			if( hasProductVariations(doc) ) {
//
//				this.logInfo("Crawling information of more than one product...");
//
//
//				/* ***************************************************
//				 * crawling variations internal ids in case we 		 *
//				 * have a voltage selector instead of a sku selector *
//				 *****************************************************/
//				Map<String, String> voltageIdToInternalIdMap = null;
//				if ( isVoltageSelector(doc) ) {
//					this.logInfo("Voltage selector detected for variations...");
//					this.logInfo("Will use remote firefox webdriver to fetch data...");
//
//					if (Main.crawlerWebdriver == null) {
//						this.logInfo("Webdriver is null, aborting this sku crawling...");
//						return;
//					}
//
//					voltageIdToInternalIdMap = this.crawlInternalIds(url);
//
//					// check if the two internalIds are equal, if they are, the fetching with webdriver went wrong
//					ArrayList<String> ids = new ArrayList<String>();
//					for (String s : voltageIdToInternalIdMap.keySet()) {
//						ids.add(voltageIdToInternalIdMap.get(s));
//					}
//					String id1 = ids.get(0);
//					String id2 = ids.get(1);
//					if (id1 != null && id2 != null) {
//						if (id1.equals(id2)) {
//							this.logError("The waiting on webdriver was not sufficient...discarding read");
//							return;
//						}
//					} else {
//						this.logError("One or both the ids are null...discarding read");
//						return;
//					}
//
//				}
//				
//				Elements productVariationElements = this.crawlSkuOptions(doc);
//				for(int i = 0; i < productVariationElements.size(); i++) {
//
//					Element sku = productVariationElements.get(i);
//
//					if( !sku.attr("value").equals("") ) { // se tem o atributo value diferente de vazio, então é uma variação de produto
//
//						// InternalId
//						String variationInternalID = null;
//						if ( isVoltageSelector(doc) ) {
//							if (voltageIdToInternalIdMap != null) {
//								variationInternalID = voltageIdToInternalIdMap.get( sku.attr("value") );
//							} else {
//								this.logError("Has a voltage selector but the map of voltage to internalId is null...");
//								return;
//							}
//						} else {
//							variationInternalID = sku.attr("value").trim();
//						}
//
//						// Fetch marketplace page for this sku
//						Document docMarketplaceInfo = this.fetchMarketplacePageForMultipleSkus(url, variationInternalID);
//
//						// Getting name from marketplace page
//						String variationName = docMarketplaceInfo.select(".fn.name a").text();
//
//						// Marketplace map
//						Map<String, Float> marketplaceMap = this.crawlMarketplace(docMarketplaceInfo);
//
//						// Assemble marketplace from marketplace map
//						JSONArray marketplace = this.assembleMarketplaceFromMap(marketplaceMap);
//
//						// Availability and Price
//						boolean available = false;
//						Float variationPrice = null;
//
//						for (String seller : marketplaceMap.keySet()) {
//							if (seller.equals(MAIN_SELLER_NAME_LOWER)) {
//								available = true;
//								variationPrice = marketplaceMap.get(seller);
//							}
//						}
//
//						if( !available ) price = null;
//
//						Product product = new Product();
//						product.setSeedId(seedId);
//						product.setUrl(url);
//						product.setInternalId(variationInternalID);
//						product.setInternalPid(internalPid);
//						product.setName(variationName);
//						product.setPrice(variationPrice);
//						product.setCategory1(category1);
//						product.setCategory2(category2);
//						product.setCategory3(category3);
//						product.setPrimaryImage(primaryImage);
//						product.setSecondaryImages(secondaryImages);
//						product.setDescription(description);
//						product.setStock(stock);
//						product.setMarketplace(marketplace);
//						product.setAvailable(available);
//
//						// execute finalization routines of this sku crawling
//						executeFinishingRoutines(product, truco);
//					}
//				}
//			}
//
//
//			/* *******************************************
//			 * crawling data of only one product in page *
//			 *********************************************/
//			else {
//				
//				// Fetch marketplace page
//				Document docMarketplaceInfo = fetchMarketplacePageForSingleProduct(url);
//
//				// Marketplace map
//				Map<String, Float> marketplaceMap = this.crawlMarketplace(docMarketplaceInfo);
//
//				// Assemble marketplace from marketplace map
//				JSONArray marketplace = this.assembleMarketplaceFromMap(marketplaceMap);
//
//				// Availability and Price
//				boolean available = false;
//				for (String seller : marketplaceMap.keySet()) {
//					if (seller.equals(MAIN_SELLER_NAME_LOWER)) {
//						available = true;
//						price = marketplaceMap.get(seller);
//					}
//				}
//
//				if(!available) price = crawlPriceRetirarLoja(price, doc);
//				
//				Product product = new Product();
//				product.setSeedId(seedId);
//				product.setUrl(url);
//				product.setInternalId(internalID);
//				product.setInternalPid(internalPid);
//				product.setName(name);
//				product.setPrice(price);
//				product.setCategory1(category1);
//				product.setCategory2(category2);
//				product.setCategory3(category3);
//				product.setPrimaryImage(primaryImage);
//				product.setSecondaryImages(secondaryImages);
//				product.setDescription(description);
//				product.setStock(stock);
//				product.setMarketplace(marketplace);
//				product.setAvailable(available);
//
//				// execute finalization routines of this sku crawling
//				executeFinishingRoutines(product, truco);
//			}
//
//		} else {
//			this.logInfo("Não é uma página de produto!", seedId);
//
//			if ( Main.mode.equals(Main.MODE_INSIGHTS) ) {
//				this.crawlerController.scheduleUrlToReprocess(url);
//			}
//		}
//	}
//
//
//
//	/*******************************
//	 * Product page identification *
//	 *******************************/
//
//	private boolean isProductPage(Document document) {
//		Element elementInternalID = document.select(".fn.name span").first();
//
//		if (elementInternalID != null) return true;
//		return false;
//	}
//
//
//
//	/************************************
//	 * Multiple products identification *
//	 ************************************/
//
//	private boolean hasProductVariations(Document document) {
//		Element selectProductVariation = document.select(".produtoSku .listaSku.selSku option").first();
//		if( selectProductVariation != null ) return true;
//
//		selectProductVariation = document.select(".produtoSku .lista-voltagem.sel-voltagem option").first();
//		if ( selectProductVariation != null ) return true;
//
//		return false;
//	}
//
//	//	private boolean isSkuSelector(Document document) {
//	//		Element selectProductVariation = document.select(".produtoSku .listaSku.selSku").first();
//	//		if( selectProductVariation != null ) return true;
//	//
//	//		return false;
//	//	}
//
//	private boolean isVoltageSelector(Document document) {		
//		Element selectProductVariation = document.select(".produtoSku .lista-voltagem.sel-voltagem").first();
//		if ( selectProductVariation != null ) return true;
//
//		return false;
//	}
//
//
//	/*******************************
//	 * Single product page methods *
//	 *******************************/
//
//	private String crawlInternalIdSingleProduct(Document document) {
//		String internalId = null;
//		Element internalIdElement = document.select(".fn.name span").first();
//		if (internalIdElement != null) {
//			internalId = Integer.toString(Integer.parseInt(internalIdElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "")));
//		}
//
//		return internalId;
//	}
//
//	private Float crawlMainPagePrice(Document document) {
//		Float price = null;
//		Element elementprice = document.select(".sale.price").first();
//		if(elementprice != null) {
//			price = Float.parseFloat(elementprice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
//		}
//
//		return price;
//	}
//
//	private Document fetchMarketplacePageForSingleProduct(String mainProductURL) {
//		String urlMarketplaceInfo = (mainProductURL.split(".html")[0] + "/lista-de-lojistas.html");
//		Document docMarketplaceInfo = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, urlMarketplaceInfo, null, this, null);
//
//		return docMarketplaceInfo;
//	}
//
//
//	private Float crawlPriceRetirarLoja(Float priceMainPage, Document doc){
//		Float price = null;		
//		Element retirarLoja = doc.select("#ctl00_Conteudo_upMasterBtnComprar a.retira-loja-bt-more").first();
//		
//		if(retirarLoja != null){
//			Element lojista = doc.select("div.buying a").first();
//			
//			if(lojista != null){
//				if(lojista.attr("title").trim().toLowerCase().equals("pontofrio")){
//					price = priceMainPage;
//				}
//			}
//		}
//		
//		return price;
//	}
//	
//	/*********************************
//	 * Multiple product page methods *
//	 *********************************/
//
//	private Document fetchMarketplacePageForMultipleSkus(String mainProductURL, String skuVariationId) {
//		String regex = "(-)(\\d+)(\\/lista-de-lojistas\\.html)";
//		String urlMarketplaceInfo = (mainProductURL.split(".html")[0] + "/lista-de-lojistas.html").replaceAll(regex, "$1" + skuVariationId + "$3");
//		Document docMarketplaceInfo = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, urlMarketplaceInfo, null, this, null);
//
//		return docMarketplaceInfo;
//	}
//
//	private Map<String, String> crawlInternalIds(String url) {
//		Map<String, String> voltageIdToInternalId = new HashMap<String, String>();
//
//		// Load the first product url
//		Document docMainPageSKU1 = Jsoup.parse( Main.crawlerWebdriver.loadUrl(url) );
//
//		// Load the second product url
//		Document docMainPageSKU2 = null;
//		List<WebElement> options = Main.crawlerWebdriver.findElementsByCssSelector(".produtoSku .lista-voltagem.sel-voltagem option");
//
//		for(WebElement option : options) {
//			String voltageId = option.getAttribute("value").trim();
//			String internalId = null;
//
//			if ( !option.isSelected() ) {
//				option.click();
//
//				try {
//					Thread.sleep(5000);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//
//				docMainPageSKU2 = Jsoup.parse( Main.crawlerWebdriver.getCurrentPageSource() );
//				internalId = crawlInternalIdSingleProduct(docMainPageSKU2);
//
//			} else {
//				internalId = crawlInternalIdSingleProduct(docMainPageSKU1);
//			}
//
//			voltageIdToInternalId.put(voltageId, internalId);
//		}
//
//		return voltageIdToInternalId;
//	}
//
//	private Elements crawlSkuOptions(Document document) {
//		Elements skuOptions = null;
//
//		skuOptions = document.select(".produtoSku .listaSku.selSku option");
//		if (skuOptions.size() == 0) {
//			skuOptions = document.select(".produtoSku .lista-voltagem.sel-voltagem option");
//		}
//
//		return skuOptions;
//	}
//
//	/*******************
//	 * General methods *
//	 *******************/
//
//	private String crawlPrimaryImage(Document document) {
//		String primaryImage = null;
//		Element primaryImageElement = document.select("#divFullImage a").first();
//
//		if (primaryImageElement != null) {
//			String image = primaryImageElement.attr("href");
//			if(image.startsWith("http") && !image.contains("indisponivel.gif")){
//				primaryImage = image;
//			} else {
//				Element e = primaryImageElement.select("img").first();
//				
//				if(e != null){
//					image = e.attr("src");
//					if(image.startsWith("http") && !image.contains("indisponivel.gif")){
//						primaryImage = image;
//					}
//				}
//			}
//		}
//
//		return primaryImage;
//	}
//
//	private String crawlSecondaryImages(Document document, String primaryImage) {
//		String secondaryImages = null;
//		Elements elementsSecondaryImages = document.select(".boxImg .carouselBox ul li a");
//		JSONArray secondaryImagesArray = new JSONArray();
//
//		if(elementsSecondaryImages.size() > 0) {
//			for(int i = 0; i < elementsSecondaryImages.size(); i++) {
//				Element e = elementsSecondaryImages.get(i);
//				String image = e.attr("href");
//				if( !image.equals(primaryImage) && image.startsWith("http") && !image.contains("indisponivel.gif")) {
//					secondaryImagesArray.put(image);
//				} else {
//					Element x = e.select("img").first();
//					
//					if(x != null){
//						image = x.attr("src");
//						
//						if(!image.equals(primaryImage) && image.startsWith("http") && !image.contains("indisponivel.gif")){
//							secondaryImagesArray.put(image);
//						}
//					}
//				}
//			}
//		}
//		if(secondaryImagesArray.length() > 0) {
//			secondaryImages = secondaryImagesArray.toString();
//		}
//
//		return secondaryImages;
//	}
//	
//	private String crawlMainPageName(Document document) {
//		Elements elementName = document.select(".fn.name b");
//		String name = elementName.text().replace("'", "").replace("’", "").trim();
//
//		return name;
//	}
//
//	private ArrayList<String> crawlCategories(Document document) {
//		ArrayList<String> categories = new ArrayList<String>();
//		Elements elementCategories = document.select(".breadcrumb a span");
//
//		for (int i = 1; i < elementCategories.size(); i++) { // starting from index 1, because the first is the market name
//			categories.add( elementCategories.get(i).text().trim() );
//		}
//
//		return categories;
//	}
//
//	private String getCategory(ArrayList<String> categories, int n) {
//		if (n < categories.size()) {
//			return categories.get(n);
//		}
//
//		return "";
//	}
//
//	private Map<String, Float> crawlMarketplace(Document documentMarketplaceInfo) {
//		Map<String, Float> marketplace = new HashMap<String, Float>();
//
//		Elements lines = documentMarketplaceInfo.select("table#sellerList tbody tr");
//
//		for(Element linePartner: lines) { // olhar cada parceiro
//			Element elementPartnerName = linePartner.select("td.lojista div a.seller").first();
//			String partnerName = "";
//			if(elementPartnerName != null) {
//				partnerName = elementPartnerName.text().trim().toLowerCase();
//
//				Element elementPartnerPrice = linePartner.select(".valor").first();
//				Float partnerPrice = null;
//				if(elementPartnerPrice != null) {
//					partnerPrice = Float.parseFloat(elementPartnerPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
//				}
//
//				marketplace.put(partnerName, partnerPrice);
//			}			
//		}
//
//		return marketplace;
//	}
//
//	private JSONArray assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
//		JSONArray marketplace = new JSONArray();
//
//		for(String sellerName : marketplaceMap.keySet()) {
//			if ( !sellerName.equals(MAIN_SELLER_NAME_LOWER) ) {
//				JSONObject seller = new JSONObject();
//				seller.put("name", sellerName);
//				seller.put("price", marketplaceMap.get(sellerName));
//
//				marketplace.put(seller);
//			}
//		}
//
//		return marketplace;
//	}
//
//	private String crawlDescription(Document document) {
//		Elements elementDescription = document.select("#detalhes");
//		String description = elementDescription.html().trim();
//
//		return description;
//	}
//
//	private void executeFinishingRoutines(Product product, ProcessedModel truco) {
//		try {
//
//			if ( this.missingProduct(product) && Main.mode.equals(Main.MODE_INSIGHTS) ) {
//				this.crawlerController.scheduleUrlToReprocess( product.getUrl() );
//			}
//
//			else {
//
//				// print information on console
//				this.printExtractedInformation(product);
//
//				// upload image to s3
//				if (product.getPrimaryImage() != null && !product.getPrimaryImage().equals("")) {
//					db.uploadImageToAmazon(this, product.getPrimaryImage(), product.getInternalId());
//				}
//
//				// persist information on database
//				this.persistInformation(product, this.marketId, truco, product.getUrl());
//
//			}
//
//		} catch (Exception e1) {
//			e1.printStackTrace();
//		}
//	}
//
//}
