package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

/***************************************************************************************************************************************
 * Crawling notes (23/08/2016):
 * 
 * 1) For this crawler, we have one URL for multiple skus.
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 4) The sku page identification is done simply looking for an specific html element.
 * 5) If the sku is unavailable, it's price is not displayed.
 * 6) The price of sku, found in json script, is wrong when the same is unavailable, then it is not crawled.
 * 7) There is internalPid for skus in this ecommerce. The internalPid is a number that is the same for all
 * the variations of a given sku.
 * 7) The primary image is the first image on the secondary images.
 * 8) Variations of skus are not crawled if the variation is unavailable because it is not displayed for the user,
 * except if there is a html element voltage, because variations of voltage are displayed for the user even though unavailable.
 * 9) The url of the primary images are changed to bigger dimensions manually.
 * 
 * Price crawling notes:
 * 1) For the bank slip price we use a global discount rate of 5%, as we observed that this value was the default discount
 * on the ecommerce. This discount rate is applyed on the lowest installment price (menor preço a prazo). After we apply the discount
 * on the base price, we use round the result to the uppermost decimal value, because we observed that thats the rounding policy used
 * in this ecommerce.
 * 
 * 2) For the card payment options we fetch a page containing all the payment options. This page also includes the bank slip price,
 * but this one is inconsistent to that displayed on the sku main page, and that's why we doesn't crawl it.
 * 
 * Examples:
 * ex1 (available): http://www.eletronicasantana.com.br/ar-condicionado-portatil-9000-btus-quente-frio--acp09qf-%E2%80%93-agratto/p
 * ex2 (unavailable): http://www.eletronicasantana.com.br/forno-de-micro-ondas-32-l-900-w-branco-st652wruk---panasonic-08736/p
 *
 *
 **************************************************************************************************************************************/

public class BrasilEletronicasantanaCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.eletronicasantana.com.br/";

	private final Float BANK_SLIP_DISCOUNT_RATE = 0.05f;

	public BrasilEletronicasantanaCrawler(Session session) {
		super(session);
	}

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

			// Categories
			ArrayList<String> categories = crawlCategories(doc);
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);

			// Description
			String description = crawlDescription(doc);

			// Stock
			Integer stock = null;

			// Marketplace map
			Map<String, Float> marketplaceMap = crawlMarketplace(doc);

			// Marketplace
			JSONArray marketplace = assembleMarketplaceFromMap(marketplaceMap);

			// Primary image
			String primaryImage = crawlPrimaryImage(doc);

			// Secondary images
			String secondaryImages = crawlSecondaryImages(doc);

			// sku data in json
			JSONArray arraySkus = crawlSkuJsonArray(doc);			

			for(int i = 0; i < arraySkus.length(); i++){
				JSONObject jsonSku = arraySkus.getJSONObject(i);

				// Availability
				boolean available = crawlAvailability(jsonSku);

				// InternalId 
				String internalId = crawlInternalId(jsonSku);

				// Price
				Float price = crawlPrice(jsonSku, available);

				// Prices
				Prices prices = crawlPrices(jsonSku);

				// Name
				String name = crawlName(doc, jsonSku);

				Product product = new Product();
				product.setUrl(session.getOriginalURL());
				product.setInternalId(internalId);
				product.setInternalPid(internalPid);
				product.setName(name);
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

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return products;
	}

	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		if ( url.endsWith("/p") ) return true;
		return false;
	}

	/*******************
	 * General methods *
	 *******************/

	private String crawlInternalId(JSONObject json) {
		String internalId = null;

		if (json.has("sku")) {
			internalId = Integer.toString((json.getInt("sku"))).trim();			
		}

		return internalId;
	}	


	private String crawlInternalPid(Document document) {
		String internalPid = null;
		Element internalPidElement = document.select("#___rc-p-id").first();

		if (internalPidElement != null) {
			internalPid = internalPidElement.attr("value").toString().trim();
		}

		return internalPid;
	}

	private String crawlName(Document document, JSONObject jsonSku) {
		String name = null;
		Element nameElement = document.select(".productName").first();

		String nameVariation = jsonSku.getString("skuname");

		if (nameElement != null) {
			name = nameElement.text().toString().trim();

			if(nameVariation.length() > name.length()){
				name = nameVariation;
			} else if(!name.contains(nameVariation)){
				name = name + " " + nameVariation;
			}
		}

		return name;
	}

	private Float crawlPrice(JSONObject json, boolean available) {
		Float price = null;
		if (json.has("bestPriceFormated") && available) {
			price = MathCommonsMethods.parseFloat(json.getString("bestPriceFormated"));
		}
		return price;
	}

	/**
	 * To crawl both card payment options we fetch a page
	 * containing all the installments for all cards.
	 * The URL of this page is in this format: "http://www.supermuffato.com.br/productotherpaymentsystems/internalId"
	 * 
	 * When the sku is unavailable we doesn't crawl the prices, because they are not displayed for the user.
	 * 
	 * @param skuInformationJson
	 * @return
	 */
	private Prices crawlPrices(JSONObject skuInformationJson) {
		Prices prices = new Prices();
		
		// bank slip
		prices.setBankTicketPrice(crawlBankSlipPrice(skuInformationJson));

		// check for availability
		boolean skuIsAvailable = false;
		if(skuInformationJson.has("available")) {
			skuIsAvailable = skuInformationJson.getBoolean("available");
		}

		if (skuIsAvailable && skuInformationJson.has("sku")) {

			// fetch the page containing price payment options
			String skuId = Integer.toString((skuInformationJson.getInt("sku"))).trim();
			String paymentOptionsURL = "http://www.eletronicasantana.com.br/productotherpaymentsystems/" + skuId;
			Document paymentOptionsDocument = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, paymentOptionsURL, null, null);

			// installments 
			Elements tableElements = paymentOptionsDocument.select("#divCredito .tbl-payment-system");
			for (Element tableElement : tableElements) {
				Card card = crawlCardFromTableElement(tableElement);
				Map<Integer, Float> installments = crawlInstallmentsFromTable(tableElement);

				prices.insertCardInstallment(card.toString(), installments);
			}

		}

		return prices;
	}

	/**
	 * For the bank slip price we use a global discount rate of 5%, as we observed that this value was the default discount
	 * on the ecommerce. This discount rate is applyed on the lowest installment price (menor preço a prazo). After we apply the discount
	 * on the base price, we use round the result to the uppermost decimal value, because we observed that thats the rounding policy used
	 * in this ecommerce.
	 * 
	 * @param skuInformationJson
	 * @return
	 */
	private Float crawlBankSlipPrice(JSONObject skuInformationJson) {
		Float bankSlipPrice = null;

		// check for availability
		boolean skuIsAvailable = false;
		if(skuInformationJson.has("available")) {
			skuIsAvailable = skuInformationJson.getBoolean("available");
		}
		
		// bank slip
		if (skuIsAvailable && skuInformationJson.has("bestPriceFormated")) {
			Float basePrice = MathCommonsMethods.parseFloat(skuInformationJson.getString("bestPriceFormated"));
			bankSlipPrice = MathCommonsMethods.normalizeTwoDecimalPlacesUp(basePrice - (BANK_SLIP_DISCOUNT_RATE * basePrice));
		}
		
		return bankSlipPrice;
	}

	/**
	 * Crawl all the installments numbers and values from a table element.
	 * It's the same form as the example on crawlCardFromTableElement method.
	 *  
	 * @param skuInformationJson
	 * @return
	 */
	private Map<Integer, Float> crawlInstallmentsFromTable(Element tableElement) {
		Map<Integer, Float> installments = new TreeMap<Integer, Float>();
		Elements lines = tableElement.select("tr");
		for (int i = 1; i < lines.size(); i++) { // first line is the table header
			Element installmentTextElement = lines.get(i).select("td.parcelas").first();
			Element installmentPriceTextElement = lines.get(i).select("td").last();

			if (installmentTextElement != null && installmentPriceTextElement != null) {
				List<String> parsedNumbers = MathCommonsMethods.parseNumbers(installmentTextElement.text());
				if (parsedNumbers.size() == 0) { // à vista
					installments.put(1, MathCommonsMethods.parseFloat(installmentPriceTextElement.text()));
				} else {
					installments.put(Integer.parseInt(parsedNumbers.get(0)), MathCommonsMethods.parseFloat(installmentPriceTextElement.text()));
				}
			}
		}

		return installments;
	}

	/**
	 * Crawl the card brand from a table html element.
	 * 
	 * e.g:
	 * 
	 *	Nº de Parcelas	Valor de cada parcela
	 *	Visa à vista	R$ 479,90
	 *	Visa 2 vezes sem juros	R$ 239,95
	 *	Visa 3 vezes sem juros	R$ 159,96
	 *	Visa 4 vezes sem juros	R$ 119,97
	 *	Visa 5 vezes sem juros	R$ 95,98
	 *	Visa 6 vezes sem juros	R$ 79,98
	 *	Visa 7 vezes sem juros	R$ 68,55
	 *	Visa 8 vezes sem juros	R$ 59,98
	 *	Visa 9 vezes sem juros	R$ 53,32
	 *	Visa 10 vezes sem juros	R$ 47,99
	 *	Visa 11 vezes com juros	R$ 46,26
	 *	Visa 12 vezes com juros	R$ 42,61
	 *
	 * @param table
	 * @return
	 */
	private Card crawlCardFromTableElement(Element table) {
		Elements lines = table.select("tr");
		for (int i = 1; i < lines.size(); i++) { // the first is the table header
			Element installmentTextElement = lines.get(i).select("td.parcelas").first();
			if (installmentTextElement != null) {
				String installmentText = installmentTextElement.text().toLowerCase();
				if (installmentText.contains(Card.VISA.toString())) return Card.VISA;
				if (installmentText.contains(Card.AMEX.toString()) || installmentText.contains("american express")) return Card.AMEX;
				if (installmentText.contains(Card.DINERS.toString())) return Card.DINERS;
				if (installmentText.contains(Card.MASTERCARD.toString())) return Card.MASTERCARD;
				if (installmentText.contains(Card.HIPERCARD.toString())) return Card.HIPERCARD;
				if (installmentText.contains(Card.ELO.toString())) return Card.ELO;
				if (installmentText.contains(Card.AURA.toString())) return Card.AURA;
				if (installmentText.contains(Card.DISCOVER.toString())) return Card.DISCOVER;
			}
		}
		return Card.UNKNOWN_CARD;
	}

	private boolean crawlAvailability(JSONObject json) {
		if(json.has("available")) {
			return json.getBoolean("available");
		}
		return false;
	}

	private Map<String, Float> crawlMarketplace(Document document) {
		return new HashMap<String, Float>();
	}

	private JSONArray assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		return new JSONArray();
	}

	private String crawlPrimaryImage(Document doc) {
		String primaryImage = null;
		Element image = doc.select("#image a").first();

		if (image != null) {
			primaryImage = image.attr("href");
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document doc) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();
		Elements images = doc.select(".thumbs li a img");

		for (int i = 1; i < images.size(); i++) {//starts with index 1, because the first image is the primary image

			String urlImage = modifyImageURL(images.get(i).attr("src"));
			secondaryImagesArray.put(urlImage);	

		}


		if (secondaryImagesArray.length() > 0) {
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

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(".bread-crumb > ul li a");

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

	private String crawlDescription(Document document) {
		String description = "";
		Element descriptionElement = document.select("#desc_product").first();
		Element specElement = document.select("#technical_data").first();
		Element embalagemElement = document.select("#dimensions").first();

		if (descriptionElement != null) description = description + descriptionElement.html();
		if (specElement != null) description = description + specElement.html();
		if (embalagemElement != null) description = description + embalagemElement.html();

		return description;
	}

	/**
	 * Search for a particular script html element that contains a 
	 * JSONArray with all the information we need about the skus on the page.
	 * This method will parse only the JSONArray inside this script, and ignores
	 * the remaining information inside the script tag.
	 * 
	 * e.g:
	 * var skuJson_0 = 
	 * {"productId":9000346,"name":"kit Conversor Digital SC-1001 Pix +
	 *  Antena Externa Cromus CR2100","salesChannel":"1",
	 *  "available":true,"displayMode":"especificacao","dimensions":[],"dimensionsInputType":{},
	 *  "dimensionsMap":{},"skus":
	 *  [
	 *  	{"sku":9000352,
	 *  	"skuname":"kit Conversor Digital SC-1001 Pix + Antena Externa Cromus CR2100","dimensions":{},
	 * 		"available":true,
	 * 		"availablequantity":99999,
	 * 		"cacheVersionUsedToCallCheckout":"b945c45336540f7aa63e5810464143a1_geral:D99E18D9FD8A98F5A3C7FC0A5A871ED1",
	 *  	"listPriceFormated":"R$ 189,90",
	 *  	"listPrice":18990,
	 *  	"taxFormated":"R$ 0,00",
	 *  	"taxAsInt":0,
	 *  	"bestPriceFormated":"R$ 169,90",
	 *  	"bestPrice":16990,"installments":6,"installmentsValue":2831,
	 *  	"installmentsInsterestRate":0,
	 *  	"image":"http://eletronicasantana.vteximg.com.br/arquivos/ids/49938-479-479/Kit-Receptor-digital-pix---Antena-Cromus.jpg",
	 *  	"sellerId":"1","seller":"Eletronica Santana",
	 *  	"measures":{"cubicweight":1.4080,"height":22.0000,"length":12.0000,"weight":1700.0000,"width":32.0000},
	 *  	"unitMultiplier":1.0000,"rewardValue":0}
	 *  ]
	 *  };CATALOG_SDK.setProductWithVariationsCache(skuJson_0.productId, skuJson_0); var skuJson = skuJson_0;
	 * 
	 * @return a parsed JSONArray with all skus informations.
	 */
	private JSONArray crawlSkuJsonArray(Document document) {
		Elements scriptTags = document.getElementsByTag("script");
		JSONObject skuJson = new JSONObject();
		JSONArray skuJsonArray = new JSONArray();

		for (Element tag : scriptTags) {                
			for (DataNode node : tag.dataNodes()) {
				if(tag.html().trim().startsWith("var skuJson_0 = ")) {
					String nodeWholeData = node.getWholeData();
					String jsonString = 
							nodeWholeData.split(Pattern.quote("var skuJson_0 = "))[1] + 
							nodeWholeData.split(Pattern.quote("var skuJson_0 = "))[1].split(Pattern.quote("}]};"))[0];
					skuJson = new JSONObject(jsonString);
				}
			}        
		}

		if(skuJson.has("skus")) {
			skuJsonArray = skuJson.getJSONArray("skus");
		}

		return skuJsonArray;
	}
}
