package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.fetcher.DataFetcherNO;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/************************************************************************************************************************************************************************************
 * Crawling notes (29/08/2016):
 * 
 * 1) For this crawler, we have one url for mutiples skus.
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace information.
 *  
 * 4) In some cases in products has color variation, a variation can be void, so is treated in this crawler.
 * 
 * 5) The sku page identification is done simply looking the URL format or simply looking the html element.
 * 
 * 6) Even if a product is unavailable, its price is not displayed.
 * 
 * 7) There is no internalPid for skus in this ecommerce.
 * 
 * 8) The first image in secondary images is the primary image.
 * 
 * 9) When the sku has variations, the variation name it is added to the name found in the main page. 
 * 
 * 10) In case with color variations is make a url with internalId for crawl iformations for this sku.
 * 
 * 11) In case with voltage variations, we have informations in html selector.
 * 
 * 12) When one sku with color variations is void, this crawler accessed a page diferent, then verify if cod in main page is empty, if is empty, this crawler
 * ignore this variation because that is void.
 * 
 * Examples:
 * ex1 (available): http://www.multiloja.com.br/produto/TV-43-Led-Conversor+TV+Digital+Integrado-PHILIPS/5669
 * ex2 (unavailable): http://www.multiloja.com.br/produto/Antena+Interna+DTV-4500+para+TV+(VHFUHFHDTV)-Aquario/3688
 * ex3 (Color Variation): http://www.multiloja.com.br/produto/guarda+roupa+aires+3+portas+3+gavetas-araplac/7271
 * ex4 (Variation): http://www.multiloja.com.br/produto/Cafeteira+CP15+600W+com+Chapa+Aquecedora+Indicador+do+Nivel+de+agua+15xicaras-BRITANIA/2689
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilMultilojaCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.multiloja.com.br/";

	public BrasilMultilojaCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if( isProductPage(session.getOriginalURL(), doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// Variations
			boolean hasVariations = hasProductVariations(doc);

			// Pid
			String internalPid = this.crawlInternalPid(session.getOriginalURL());

			// Name
			String name = this.crawlMainPageName(doc, hasVariations);

			// Categories
			ArrayList<String> categories = this.crawlCategories(doc);
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);

			Map<String, Float> marketplaceMap = this.crawlMarketplaces(doc);

			// Assemble marketplace from marketplace map
			Marketplace marketplaces =  this.assembleMarketplaceFromMap(marketplaceMap);

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
			if( hasVariations && !hasColorVariations(doc)) {

				Logging.printLogDebug(logger, session, "Crawling information of more than one product...");

				Elements productVariationElements = crawlSkuOptions(doc);


				for(int i = 0; i < productVariationElements.size(); i++) {

					Element sku = productVariationElements.get(i);

					// InternalId
					String internalId = sku.attr("value");

					// Getting name variation
					String variationName = name + " - " + sku.attr("title");				

					// Available
					boolean available = crawlAvailabilityVoltage(sku);

					// Prices Json
					JSONObject jsonPrices = crawlJSONPrices(internalId, available);

					// Price
					Float price = crawlPriceVariation(jsonPrices);

					// Prices
					Prices prices = crawlPrices(jsonPrices, price);

					Product product = new Product();
					product.setUrl(session.getOriginalURL());
					product.setInternalId(internalId);
					product.setInternalPid(internalPid);
					product.setName(variationName);
					product.setPrice(price);
					product.setPrices(prices);
					product.setCategory1(category1);
					product.setCategory2(category2);
					product.setCategory3(category3);
					product.setPrimaryImage(primaryImage);
					product.setSecondaryImages(secondaryImages);
					product.setDescription(description);
					product.setStock(stock);
					product.setMarketplace(marketplaces);
					product.setAvailable(available);

					products.add(product);

				}
			}


			/* *******************************************
			 * crawling data of only one product in page *
			 *********************************************/
			else {

				// idvariation
				String internalID = crawlInternalIDSingleProduct(doc);

				// Available
				boolean available = crawlAvailability(doc);

				// Price
				Float price = crawlPrice(doc, available);

				Product product = new Product();
				product.setUrl(session.getOriginalURL());
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
				product.setMarketplace(marketplaces);
				product.setAvailable(available);

				products.add(product);

			}

		} else {
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
		}

		return products;
	}



	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url, Document doc) {
		Element productElement = doc.select("#nomeproduto h1").first();

		if (productElement != null && url.startsWith(HOME_PAGE + "produto/")){

			Element pageNotFound = doc.select("#codigoProduto").first();

			if(pageNotFound != null){
				String cod = pageNotFound.text().replaceAll("[^0-9]", "").trim();

				if(!cod.isEmpty()){
					return true;
				}
			}
		}
		return false;
	}



	/************************************
	 * Multiple products identification *
	 ************************************/

	private boolean hasProductVariations(Document document) {
		Elements skuChooser = document.select(".caixadeOpcoesProduto input[type=radio]");

		if (skuChooser != null) {
			if(skuChooser.size() > 1){
				return true;
			}

		} 

		return false;

	}


	/*******************************
	 * Single product page methods *
	 *******************************/

	private String crawlInternalIDSingleProduct(Document document) {
		String internalIDMainPage = null;
		Element elementDataSku = document.select("#itemVenda").first();

		if(elementDataSku == null) {
			elementDataSku = document.select(".caixadeOpcoesProduto input[type=radio]").first();
		}

		if(elementDataSku != null){
			internalIDMainPage = elementDataSku.attr("value");
		}

		return internalIDMainPage;
	}

	private Map<String, Float> crawlMarketplaces(Document doc) {
		Map<String, Float>  marketplace = new HashMap<String, Float> ();

		return marketplace;
	}

	/*********************************
	 * Multiple product page methods *
	 *********************************/


	private Elements crawlSkuOptions(Document document) {
		Elements skuOptions = document.select(".caixadeOpcoesProduto input[type=radio]");

		return skuOptions;
	}

	private boolean crawlAvailabilityVoltage(Element sku) {
		boolean available = true;

		if(sku.hasAttr("disabled")){
			available = false;
		}

		return available;
	}


	/*****************
	 * Color methods *
	 *****************/

	private boolean hasColorVariations(Document doc){
		Element cor = doc.select(".corProd").first();

		if(cor != null){
			return true;
		}

		return false;
	}


	/*******************
	 * General methods *
	 *******************/

	private String crawlInternalPid(String url) {
		String internalPid = null;

		if(url.contains("?")){
			String text = url.split("\\?")[0];

			String[] tokens = text.split("/");
			internalPid = tokens[tokens.length-1];
		}

		return internalPid;
	}

	private Float crawlPrice(Document doc, boolean available) {
		Float price = null;

		if(available){
			Element e = doc.select(".preco span").first();

			if(e != null){
				price = Float.parseFloat(e.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());
			}
		}
		return price;
	}

	private Float crawlPriceVariation(JSONObject jsonPrices) {
		Float price = null;

		if(jsonPrices.has("price")){
			price = MathUtils.parseFloatWithComma(jsonPrices.getString("price"));
		} else if(jsonPrices.has("priceVista")){
			price = MathUtils.parseFloatWithComma(jsonPrices.getString("priceVista"));
		}

		return price;
	}

	private boolean crawlAvailability(Document doc) {
		boolean available = true;
		Element e = doc.select("#itensProd #aviseme").first();

		if(e != null){
			available = false;
		}

		return available;
	}

	private String crawlPrimaryImage(Document document) {

		String primaryImage = null;

		Element primaryImageElements = document.select(".clearfix .jqzoom").first();

		if(primaryImageElements != null){
			primaryImage = primaryImageElements.attr("href");
		} 

		return primaryImage;
	}

	private String crawlMainPageName(Document document, boolean hasVariations) {
		String name = null;
		Element elementName = document.select("#nomeproduto h1").first();

		if(elementName != null) {
			name = elementName.text().replace("'","").replace("’","").trim();
		}

		if(!hasVariations){
			Element oneVariation = document.select(".caixadeOpcoesProduto input[type=radio]").first();

			if(oneVariation != null){
				name = name + " - " + oneVariation.attr("title").trim();
			}
		}

		return name;
	}

	private ArrayList<String> crawlCategories(Document document) {
		Elements elementCategories = document.select(".mobileBreadcrumb a[title]");
		ArrayList<String> categories = new ArrayList<String>();

		for(int i = 1; i < elementCategories.size(); i++) { // start with index 1 because the first item is the home page
			categories.add(elementCategories.get(i).text().trim());
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
		Elements elementFotoSecundaria = document.select(".clearfix > ul li a.zoomThumbActive");

		if (elementFotoSecundaria.size()>1) {
			for(int i = 1; i < elementFotoSecundaria.size(); i++) { //starts with index 1 because de primary image is the first image
				Element e = elementFotoSecundaria.get(i);
				String rel = e.attr("rel");

				JSONObject jsonImages;
				try {
					jsonImages = new JSONObject(rel);
				} catch (JSONException e1) {
					jsonImages = new JSONObject();
				}

				if(jsonImages.has("largeimage")){
					secondaryImagesArray.put(jsonImages.getString("largeimage").trim());
				} else if (jsonImages.has("smallimage")){
					secondaryImagesArray.put(jsonImages.getString("smallimage").trim());
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

	private Marketplace assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		return new Marketplace();
	}


	private String crawlDescription(Document document) {
		String description = "";
		Elements elementsProductDetails = document.select(".abasdescricao:not(.avaliacaoContProd)");

		for(Element e : elementsProductDetails){
			description += e.html();
		}


		return description;
	}

	private JSONObject crawlJSONPrices(String internalId, boolean available){
		JSONObject prices = new JSONObject();

		if(available){
			String url = session.getOriginalURL();
			String payload = "xajax=atualizaCompraCasada&xajaxr=1479918276420&xajaxargs[]=" + internalId;

			Map<String,String> headers = new HashMap<>();
			headers.put("Content-Type", "application/x-www-form-urlencoded");

			Document docXml = DataFetcherNO.fetchDocumentXml(DataFetcherNO.POST_REQUEST, session, url, payload, cookies);
			
			Document doc = parseXmlToHtml(docXml);			
			Element principalPrice = doc.select(".preco span:not([class])").first();
			
			if(principalPrice != null){
				prices.put("price", principalPrice.text());
			}

			Element vistaPrice = doc.select(".compraPrazo span").last();

			if(vistaPrice != null){
				prices.put("priceVista", vistaPrice.text());
			}

			Element parcelas = doc.select(".parcela").first();

			if(parcelas != null){
				String text = parcelas.ownText().toLowerCase();

				if(text.contains("x") && text.contains("$")){
					int x = text.indexOf("x");

					String installment = text.substring(0, x).replaceAll("[^0-9]", "");
					String value = text.substring(x);

					JSONObject parcels = new JSONObject();
					parcels.put("installment", installment);
					parcels.put("installmentValue", value);

					prices.put("parcels", parcels);
				}
			}
		}

		return prices;
	}

	private Prices crawlPrices(JSONObject jsonPrices, Float price){
		Prices prices = new Prices();

		if(price != null){
			Map<Integer,Float> installmentPriceMap = new HashMap<>();

			if(jsonPrices.has("priceVista")){
				Float vistaPrice = MathUtils.parseFloatWithComma(jsonPrices.getString("priceVista"));

				// 1x no cartão e boleto são o mesmo preço
				installmentPriceMap.put(1, vistaPrice);
				prices.setBankTicketPrice(vistaPrice);
			}

			if(jsonPrices.has("parcels")){
				JSONObject parcels = jsonPrices.getJSONObject("parcels");

				if(parcels.has("installment") && parcels.has("installmentValue")){
					Integer installment = Integer.parseInt(parcels.getString("installment"));
					Float value = MathUtils.parseFloatWithComma(parcels.getString("installmentValue"));

					installmentPriceMap.put(installment, value);
				}
			}

			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.SENFF.toString(), installmentPriceMap);
		}

		return prices;
	}
	
	private Document parseXmlToHtml(Document docXml){
		Document doc = new Document("");			
		
		String document = docXml.getElementsByTag("cmd").text();
		doc = Jsoup.parse(document);
		
		return doc;
	}
}
