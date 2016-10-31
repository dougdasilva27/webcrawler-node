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
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.util.CommonMethods;
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

	public SaopauloExtramarketplaceCrawler(CrawlerSession session) {
		super(session);
	}

	private final String MAIN_SELLER_NAME_LOWER = "extra";
	private final String HOME_PAGE = "http://www.extra.com.br/";

	@Override
	public boolean shouldVisit() {
		String href = session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if( isProductPage(doc, session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL()); 

			// Pegando url padrão no doc da página, para lidar com casos onde tem url em formato diferente no banco
			String modifiedURL = makeUrlFinal(getRedirectUrl());

			// Variations
			boolean hasVariations = hasProductVariations(doc);

			if (hasVariations) Logging.printLogDebug(logger, session, "Identifiquei que possui variações");

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

			// Seller principal
			String principalSeller = crawlPrincipalSeller(doc);

			if( hasVariations ) {				
				Elements productVariationElements = this.crawlSkuOptions(doc);

				// Array de ids para url para pegar marketplace
				List<String> idsForUrlMarketPlace = this.identifyIDForUrlLojistas(modifiedURL, doc, productVariationElements, unnavailableForAll);

				// Pegando os documents das páginas de marketPlace para produtos especiais
				Map<String, Document> documentsMarketPlaces = this.fetchDocumentMarketPlacesToProductSpecial(idsForUrlMarketPlace, modifiedURL);

				for(int i = 0; i < productVariationElements.size(); i++) {

					Element sku = productVariationElements.get(i);

					// InternalId
					String variationInternalID = internalPid + "-" + sku.attr("value");

					// Getting name variation
					String variationName = makeVariationName(name, sku).trim();

					// Document marketplace
					Document docMarketplace = getDocumentMarketpalceForSku(documentsMarketPlaces, variationName, sku, modifiedURL);

					// Marketplace map
					Map<String,Prices> marketplaceMap = this.crawlMarketplaces(docMarketplace, principalSeller, doc);

					// Assemble marketplace from marketplace map
					JSONArray marketplace = this.assembleMarketplaceFromMap(marketplaceMap);

					// Available
					boolean available = this.crawlAvailability(marketplaceMap);

					// Price
					Float variationPrice = this.crawlPrice(docMarketplace);

					// Prices
					Prices prices = crawlPricesForProduct(marketplaceMap);

					Product product = new Product();

					product.setUrl(session.getOriginalURL());
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
					product.setMarketplace(marketplace);
					product.setAvailable(available);

					products.add(product);

				}
			}


			/* *******************************************
			 * crawling data of only one product in page *
			 *********************************************/
			else {

				// Second part internalId
				String internalIdSecondPart = this.crawlInternalIDSingleProduct(doc);
				
				// InternalId
				String internalID = internalPid + "-" + internalIdSecondPart;
				
				// Document marketplace
				Document docMarketplace = fetchDocumentMarketPlace(internalIdSecondPart, modifiedURL);

				// Marketplace map
				Map<String,Prices> marketplaceMap = this.crawlMarketplaces(docMarketplace, principalSeller, doc);

				// Assemble marketplace from marketplace map
				JSONArray marketplace = this.assembleMarketplaceFromMap(marketplaceMap);

				// Available
				boolean available = this.crawlAvailability(marketplaceMap);

				// Price
				Float price = this.crawlPrice(docMarketplace);

				// Prices
				Prices prices = crawlPricesForProduct(marketplaceMap);

				Product product = new Product();

				product.setUrl(session.getOriginalURL());
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
			if(skuChooser.size() == 2) {
				String prodOne = skuChooser.get(0).text();
				if(prodOne.contains("|")) {
					prodOne = prodOne.split("\\|")[0].trim();
				}

				String prodTwo = skuChooser.get(1).text();
				if(prodTwo.contains("|")) {
					prodTwo = prodTwo.split("\\|")[0].trim();
				}


				if(prodOne.equals(prodTwo)) {
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

	private String getRedirectUrl(){
		String urlRedirect = null;

		if(session.getRedirectedToURL(session.getOriginalURL()) != null){
			urlRedirect = session.getRedirectedToURL(session.getOriginalURL());
		} else {
			urlRedirect = session.getOriginalURL();
		}


		return urlRedirect;
	}

	private String crawlInternalIDSingleProduct(Document document) {
		String internalIDMainPage = null;
		Element elementDataSku = document.select("#ctl00_Conteudo_hdnIdSkuSelecionado").first();

		if(elementDataSku != null) {
			internalIDMainPage = elementDataSku.attr("value");
		}

		return internalIDMainPage;
	}

	/*********************************
	 * Multiple product page methods *
	 *********************************/

	private Map<String, Document> fetchDocumentMarketPlacesToProductSpecial(List<String> idsForUrlMarketPlace, String url){
		Map<String, Document> documentsMarketPlaces = new HashMap<>();

		if(idsForUrlMarketPlace != null){
			Document docMarketPlaceProdOne = this.fetchDocumentMarketPlace(idsForUrlMarketPlace.get(0), url);
			String[] namev = docMarketPlaceProdOne.select("#ctl00_Conteudo_lnkProdutoDescricao").text().split("-");
			documentsMarketPlaces.put(namev[namev.length-1].trim().toLowerCase(),docMarketPlaceProdOne);

			if(idsForUrlMarketPlace.size() == 2){
				Document docMarketPlaceProdTwo = this.fetchDocumentMarketPlace(idsForUrlMarketPlace.get(1), url);
				String[] namev2 = docMarketPlaceProdTwo.select("#ctl00_Conteudo_lnkProdutoDescricao").text().split("-");
				documentsMarketPlaces.put(namev2[namev2.length-1].trim(), docMarketPlaceProdTwo);
			}
		}

		return documentsMarketPlaces;
	}

	private List<String> identifyIDForUrlLojistas(String url, Document doc, Elements skuOptions, boolean unnavailableForAll){
		if(!unnavailableForAll){
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

		return null;
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

	private Map<String,Prices> crawlMarketplaces(Document docMarketplaceInfo, String principalSeller, Document doc) {
		Map<String,Prices> marketplace = new HashMap<> ();

		Elements lines = docMarketplaceInfo.select("table#sellerList tbody tr");

		for(Element linePartner: lines) {			
			String partnerName = linePartner.select("a.seller").first().text().trim().toLowerCase();
			Float partnerPrice = Float.parseFloat(linePartner.select(".valor").first().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

			Element comprar = linePartner.select(".adicionarCarrinho > a.bt-comprar-disabled").first();

			Prices prices = new Prices();

			if(principalSeller!= null){
				if(!principalSeller.equals(partnerName)){
					prices.insertBankTicket(partnerPrice);

					Map<Integer,Float> installmentPriceMap = new HashMap<>();
					installmentPriceMap.put(1, partnerPrice);

					Elements installments = linePartner.select(".valorTotal span strong");

					if(installments.size() > 1){
						Integer installment = Integer.parseInt(installments.get(0).text().trim());
						Float value = Float.parseFloat(installments.get(1).text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

						installmentPriceMap.put(installment, value);

					}

					prices.insertCardInstallment("visa", installmentPriceMap);	
				} else {
					prices = crawlPrices(doc, partnerPrice);
				}
			}else {
				prices = crawlPrices(doc, partnerPrice);
			}


			if(comprar == null) marketplace.put(partnerName, prices);
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

	private Float crawlPrice(Document docMarketplaceInfo) {
		Float price = null;

		Elements lines = docMarketplaceInfo.select("table#sellerList tbody tr");

		for(Element linePartner: lines) {			
			String partnerName = linePartner.select("a.seller").first().text().trim().toLowerCase();

			Element comprar = linePartner.select(".adicionarCarrinho > a.bt-comprar-disabled").first();
			
			if(comprar == null && partnerName.equals(MAIN_SELLER_NAME_LOWER)){
				price = Float.parseFloat(linePartner.select(".valor").first().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));;
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


		return CommonMethods.removeIllegalArguments(primaryImage);
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
						secondaryImagesArray.put(CommonMethods.removeIllegalArguments(e.attr("rev")));
					} else {
						secondaryImagesArray.put(CommonMethods.removeIllegalArguments(e.attr("href")));
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

	private Document getDocumentMarketpalceForSku(Map<String,Document> documentsMarketPlaces, String name, Element sku, String url){
		Document docMarketplaceInfo = new Document(url);
		if(!sku.text().contains("Esgotado")){
			if(documentsMarketPlaces.size() > 0){

				if(documentsMarketPlaces.size() == 1){
					for(String key : documentsMarketPlaces.keySet()){
						docMarketplaceInfo = documentsMarketPlaces.get(key);
					}
				} else {
					String[] tokens = name.split("-");
					String nameV = tokens[tokens.length-1].trim().toLowerCase();

					if(documentsMarketPlaces.containsKey(nameV)){
						docMarketplaceInfo = documentsMarketPlaces.get(nameV);
					}
				}

			} else {
				docMarketplaceInfo = fetchDocumentMarketPlace(sku.attr("value"), url);
			}
		}

		return docMarketplaceInfo;
	}

	private Prices crawlPrices(Document doc, Float price){
		Prices prices = new Prices();
		if(price != null){
			Map<Integer,Float> installmentPriceMap = new HashMap<>();

			// Preço no boleto e preço á vista no cartão são iguais
			Element priceDiscount = doc.select(".price.discount").first();

			if(priceDiscount != null){
				Float priceVista = Float.parseFloat(priceDiscount.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
				prices.insertBankTicket(priceVista);
				installmentPriceMap.put(1, priceVista);
			} else {
				prices.insertBankTicket(price);
				installmentPriceMap.put(1, price);
			}


			Elements installments = doc.select(".tabsCont tr");

			for(int i = 1; i < installments.size(); i++){ // start with index 1 because the first item is the title
				Element e = installments.get(i);
				String id = e.attr("id");

				if(!id.contains("CartaoFlex")){

					Element parcela = e.select("> th").first();

					if(parcela != null){
						String parcelaText = parcela.text().toLowerCase();
						int x = parcelaText.indexOf("x");

						Integer installment = Integer.parseInt(parcelaText.substring(0, x).replaceAll("[^0-9]", "").trim());

						Element valor = e.select("> td").first();

						if(valor != null){
							Float value = Float.parseFloat(valor.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

							installmentPriceMap.put(installment, value);
						}
					}
				}
			}


			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
		}

		return prices;
	}

	private Prices crawlPricesForProduct(Map<String, Prices> marketplaces){
		Prices prices = new Prices();

		for (String seller : marketplaces.keySet()) {
			if (seller.equals(MAIN_SELLER_NAME_LOWER)) {
				prices = marketplaces.get(seller);
				break;
			}
		}	

		return prices;
	}

	private String crawlPrincipalSeller(Document doc){
		String seller = "";

		Element sellerElement = doc.select(".buying > a").first();

		if(sellerElement != null){
			seller = sellerElement.text().toLowerCase().trim();
		}

		return seller;
	}


}