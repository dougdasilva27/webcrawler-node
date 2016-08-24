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

import br.com.lett.crawlernode.kernel.CrawlerSession;
import br.com.lett.crawlernode.kernel.fetcher.DataFetcher;
import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.Crawler;
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
 * Examples:
 * ex1 (available): http://www.extra.com.br/Eletronicos/Televisores/SmartTV/Smart-TV-LED-39-HD-Philco-PH39U21DSGW-com-Conversor-Digital-MidiaCast-PVR-Wi-Fi-Entradas-HDMI-e-Endrada-USB-7323247.html
 * ex2 (unavailable): http://www.extra.com.br/Eletroportateis/Cafeteiras/CafeteirasEletricas/Cafeteira-Eletrica-Philco-PH16-Vermelho-Aco-Escovado-4451511.html
 * ex3 (only_marketplace): http://www.extra.com.br/CamaMesaBanho/ToalhaAvulsa/Banho/Toalha-de-Banho-Desiree-Pinta-e-Borda---Santista-4811794.html
 * ex4 (Product with marketplace special): http://www.extra.com.br/Eletrodomesticos/FornodeMicroondas/Microondas-30-Litros-Midea-Liva-Grill---MTAG42-7923503.html
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class SaopauloExtramarketplaceCrawler extends Crawler {

	private final String MAIN_SELLER_NAME_LOWER = "extra";
	private final String HOME_PAGE = "http://www.extra.com.br/";
	
	public SaopauloExtramarketplaceCrawler(CrawlerSession session) {
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

		if( isProductPage(doc, session.getUrl()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());
			
			// Pegando url padrão no doc da página, para lidar com casos onde tem url em formato diferente no banco
			session.setUrl(makeUrlFinal(session.getUrl()));
			
			// Variations
			boolean hasVariations = hasProductVariations(doc);
			
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
			if( hasVariations ) {

				Logging.printLogDebug(logger, session, "Crawling information of more than one product...");
				
				Elements productVariationElements = this.crawlSkuOptions(doc);
				
				// Array de ids para url para pegar marketplace
				List<String> idsForUrlMarketPlace = this.identifyIDForUrlLojistas(session.getUrl(), doc, productVariationElements);

				// Pegando os documents das páginas de marketPlace para produtos especiais
				Map<String, Document> documentsMarketPlaces = this.fetchDocumentMarketPlacesToProductSpecial(idsForUrlMarketPlace, session.getUrl());
				
				for(int i = 0; i < productVariationElements.size(); i++) {

					Element sku = productVariationElements.get(i);

					// InternalId
					String variationInternalID = sku.attr("value");

					// Getting name variation
					String variationName = makeVariationName(name, sku).trim();

					// Marketplace map
					Map<String, Float> marketplaceMap = this.crawlMarketplacesForMutipleVariations(session.getUrl(), sku, documentsMarketPlaces, variationName);

					// Assemble marketplace from marketplace map
					JSONArray marketplace = this.assembleMarketplaceFromMap(marketplaceMap);

					// Available
					boolean available = this.crawlAvailability(marketplaceMap);

					// Price
					Float variationPrice = this.crawlPrice(marketplaceMap);

					Product product = new Product();
					product.setSeedId(session.getSeedId());
					product.setUrl(session.getUrl());
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
				Map<String, Float> marketplaceMap = this.crawlMarketplacesForSingleProduct(doc, session.getUrl());

				// Assemble marketplace from marketplace map
				JSONArray marketplace = this.assembleMarketplaceFromMap(marketplaceMap);

				// Available
				boolean available = this.crawlAvailability(marketplaceMap);

				// Price
				Float price = this.crawlPrice(marketplaceMap);

				Product product = new Product();
				product.setSeedId(session.getSeedId());
				product.setUrl(session.getUrl());
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
			Logging.printLogTrace(logger, "Not a product page" + session.getSeedId());
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

	private Map<String, Float> crawlMarketplacesForSingleProduct(Document doc, String url) {
		Map<String, Float>  marketplace = new HashMap<String, Float> ();

		Document docMarketplaceInfo = fetchDocumentMarketPlace(null, url);

		Elements lines = docMarketplaceInfo.select("table#sellerList tbody tr");

		for(Element linePartner: lines) {

			String partnerName = linePartner.select("a.seller").first().text().trim().toLowerCase();
			Float partnerPrice = Float.parseFloat(linePartner.select(".valor").first().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));;

			marketplace.put(partnerName, partnerPrice);
			
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
			Document docMarketplaceInfo;
			if(documentsMarketPlaces.size() > 0){
				String[] nameV = name.split("-");
				docMarketplaceInfo = documentsMarketPlaces.get(nameV[nameV.length-1].trim());
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

		Element primaryImageElements = document.select("#divFullImage a").first();

		if(primaryImageElements.attr("href").isEmpty() && primaryImageElements.attr("href").startsWith("http")){
			primaryImage = primaryImageElements.attr("href");
		} else {
			Element e = primaryImageElements.select("img").first();
			
			if(e != null){
				primaryImage = e.attr("src");
			}
		}


		return primaryImage;
	}
	
	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;

		JSONArray secondaryImagesArray = new JSONArray();
		Elements elementFotoSecundaria = document.select(".thumbsImg li a");

		if (elementFotoSecundaria.size()>1) {
			for(int i = 1; i < elementFotoSecundaria.size(); i++) { //starts with index 1 because de primary image is the first image
				Element e = elementFotoSecundaria.get(i);

				if(e.attr("href").isEmpty() && e.attr("href").startsWith("http")){
					secondaryImagesArray.put(e.attr("value"));
				} else {
					Element x = e.select("img").first();
					
					if(x != null){
						secondaryImagesArray.put(x.attr("src"));
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