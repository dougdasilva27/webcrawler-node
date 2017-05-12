package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
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
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

/************************************************************************************************************************************************************************************
 * Crawling notes (23/08/2016):
 * 
 * 1) For this crawler, we have one URL for multiple skus.
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking for an specific html element.
 * 
 * 5) If the sku is unavailable, it's price is not displayed.
 * 
 * 6) The price of sku, found in json script, is wrong when the same is unavailable, then it is not crawled.
 * 
 * 7) There is internalPid for skus in this ecommerce. The internalPid is a number that is the same for all
 * the variations of a given sku.
 * 
 * 7) The primary image is the first image on the secondary images.
 * 
 * 8) Variations of skus are not crawled if the variation is unavailable because it is not displayed for the user,
 * except if there is a html element voltage, because variations of voltage are displayed for the user even though unavailable.
 * 
 * 9) The url of the primary images are changed to bigger dimensions manually.
 * 
 * 10) The secondary images are get in api "http://www.schumann.com.br/produto/sku/" + internalId
 * 
 * Examples:
 * ex1 (available): http://www.schumann.com.br/cafeteira-expresso-tres-coracoes-s04-modo-vermelha-multibebidas/p
 * ex2 (unavailable): http://www.schumann.com.br/ar-condicionado-springer-split-hi-wall-12000-btus-quente-e-frio/p
 *
 *
 ************************************************************************************************************************************************************************************/

public class BrasilSchumannCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.schumann.com.br/";

	public BrasilSchumannCrawler(Session session) {
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
				Prices prices = crawlPrices(doc, jsonSku);
				
				// Primary image
				String primaryImage = crawlPrimaryImage(jsonSku);
				
				// Name
				String name = crawlName(doc, jsonSku);
				
				// Secondary images
				String secondaryImages = crawlSecondaryImages(internalId, arraySkus, doc);
				
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
			
			if(name.equals(nameVariation)){
				if(jsonSku.has("dimensions")){
					JSONObject dimensions = jsonSku.getJSONObject("dimensions");
					
					if(dimensions.has("Cor")) nameVariation = dimensions.getString("Cor");
					if(dimensions.has("Tipo")) nameVariation = dimensions.getString("Tipo");
					if(dimensions.has("Voltagem")) nameVariation = dimensions.getString("Voltagem");
				}
			}
			
			if(!name.contains(nameVariation)){
				name = name + " - " + nameVariation;
			}
		}

		return name;
	}

	private Float crawlPrice(JSONObject json, boolean available) {
		Float price = null;
		
		if (json.has("bestPriceFormated") && available) {
			price = Float.parseFloat( json.getString("bestPriceFormated").replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		}

		return price;
	}
	
	/**
	 * 
	 * @param skuInformationJson
	 * @return
	 */
	private Prices crawlPrices(Document document, JSONObject skuInformationJson) {
		Prices prices = new Prices();

		// bank slip
		Float bankSlipPrice = crawlBankSlipPrice(document, skuInformationJson);
		if (bankSlipPrice != null) {
			prices.setBankTicketPrice(bankSlipPrice);
		}

		// installments
		if (skuInformationJson.has("sku")) {

			// fetch the page with payment options
			String skuId = Integer.toString(skuInformationJson.getInt("sku"));
			String paymentOptionsURL = "http://www.schumann.com.br/productotherpaymentsystems/" + skuId;
			Document paymentOptionsDocument = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, paymentOptionsURL, null, null);

			// get all cards brands
			List<String> cardBrands = new ArrayList<String>();
			Elements cardsBrandsElements = paymentOptionsDocument.select(".div-card-flag #ddlCartao option");
			for (Element cardBrandElement : cardsBrandsElements) {
				String cardBrandText = cardBrandElement.text().toLowerCase();
				if (cardBrandText.contains("american express") || cardBrandText.contains(Card.AMEX.toString())) cardBrands.add(Card.AMEX.toString());
				else if (cardBrandText.contains(Card.VISA.toString())) cardBrands.add(Card.VISA.toString());
				else if (cardBrandText.contains(Card.DINERS.toString())) cardBrands.add(Card.DINERS.toString());
				else if (cardBrandText.contains(Card.MASTERCARD.toString())) cardBrands.add(Card.MASTERCARD.toString());
				else if (cardBrandText.contains(Card.HIPERCARD.toString())) cardBrands.add(Card.HIPERCARD.toString());
				else if (cardBrandText.contains(Card.ELO.toString())) cardBrands.add(Card.ELO.toString());
			}

			// get each table payment option in the same sequence as we got the cards brands (the html logic was this way)
			Elements paymentElements = paymentOptionsDocument.select("#divCredito .tbl-payment-system tbody");

			for (int i = 0; i < cardBrands.size(); i++) {
				if (paymentElements.size() > i) {
					Element paymentElement = paymentElements.get(i);
					Map<Integer, Float> installments = crawlInstallmentsFromTableElement(paymentElement);
					if (installments.size() > 0) prices.insertCardInstallment(cardBrands.get(i), installments);
				}
			}
		}	
		
		return prices;
	}
	
	/**
	 * Extract all installments from a table html element.
	 * 
	 * e.g:
	 * 	Nº de Parcelas	Valor de cada parcela
	 *	American Express à vista	R$ 1.799,00
	 *	American Express 2 vezes sem juros	R$ 899,50
	 *	American Express 3 vezes sem juros	R$ 599,66
	 *	American Express 4 vezes sem juros	R$ 449,75
	 *	American Express 5 vezes sem juros	R$ 359,80
	 *	American Express 6 vezes sem juros	R$ 299,83
	 *	American Express 7 vezes sem juros	R$ 257,00
	 *	American Express 8 vezes sem juros	R$ 224,87
	 *	American Express 9 vezes sem juros	R$ 199,88
	 *	American Express 10 vezes sem juros	R$ 179,90
	 *	American Express 11 vezes com juros	R$ 173,41
	 *	American Express 12 vezes com juros	R$ 159,73
	 *
	 * @param tableElement
	 * @return
	 */
	private Map<Integer, Float> crawlInstallmentsFromTableElement(Element tableElement) {
		Map<Integer, Float> installments = new TreeMap<Integer, Float>();

		Elements tableLinesElements = tableElement.select("tr");
		for (int j = 1; j < tableLinesElements.size(); j++) { // the first one is just the table header
			Element tableLineElement = tableLinesElements.get(j);
			Element installmentNumberElement = tableLineElement.select("td.parcelas").first();
			Element installmentPriceElement = tableLineElement.select("td").last();

			if (installmentNumberElement != null && installmentPriceElement != null) {
				String installmentNumberText = installmentNumberElement.text().toLowerCase();
				String installPriceText = installmentPriceElement.text();

				List<String> parsedNumbers = MathCommonsMethods.parseNumbers(installmentNumberText);
				if (parsedNumbers.size() == 0) { // à vista
					installments.put(1, MathCommonsMethods.parseFloat(installPriceText));
				} else {
					installments.put(Integer.parseInt(parsedNumbers.get(0)), MathCommonsMethods.parseFloat(installPriceText));
				}
			}
		}

		return installments;
	}
	
	/**
	 * Computes the bank slip price by applying a discount on the base price.
	 * The base price is the same that is crawled on crawlPrice method.
	 * 
	 * @param document
	 * @param jsonSku
	 * @return
	 */
	private Float crawlBankSlipPrice(Document document, JSONObject jsonSku) {
		Float bankSlipPrice = null;

		// check availability
		boolean available = false;
		if(jsonSku.has("available")) {
			available = jsonSku.getBoolean("available");
		}

		if (available) {
			if (jsonSku.has("bestPriceFormated") && available) {
				Float basePrice = MathCommonsMethods.parseFloat(jsonSku.getString("bestPriceFormated"));
				Float discountPercentage = crawlDiscountPercentage(document);

				// apply the discount on base price
				if (discountPercentage != null) {
					bankSlipPrice = MathCommonsMethods.normalizeTwoDecimalPlaces(basePrice - (discountPercentage * basePrice));
				}
			}
		}

		return bankSlipPrice;
	}

	/**
	 * Look for the discount html element and parses the discount percentage
	 * from the element name. 
	 * In this ecommerce we have elements in this form 
	 * <p class="flag boleto-10--off">Boleto 10% Off</p> where the 10 in the name 
	 * of the class indicates the percentual value we must apply on the base value.
	 * 
	 * @return
	 */
	private Float crawlDiscountPercentage(Document document) {
		Float discountPercentage = null;
		Element discountElement = document.select(".product-info-container p[class^=flag desconto-boleto]").first();
		if (discountElement != null) {
			List<String> parsedNumbers = MathCommonsMethods.parsePositiveNumbers(discountElement.attr("class"));
			if (parsedNumbers.size() > 0) {
				try {
					Integer discount = Integer.parseInt(parsedNumbers.get(0));
					Float discountFloat = new Float(discount);
					discountPercentage = MathCommonsMethods.normalizeTwoDecimalPlaces(discountFloat / 100);
				} catch (NumberFormatException e) {
					Logging.printLogError(logger, session, "Error parsing integer from String in CrawlDiscountPercentage method.");
				}
			}
		}
		return discountPercentage;
	}
	
	private boolean crawlAvailability(JSONObject json) {

		if(json.has("available")) return json.getBoolean("available");
		
		return false;
	}

	private Map<String, Float> crawlMarketplace(Document document) {
		return new HashMap<String, Float>();
	}
	
	private JSONArray assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		return new JSONArray();
	}

	private String crawlPrimaryImage(JSONObject json) {
		String primaryImage = null;

		if (json.has("image")) {
			String urlImage = json.getString("image");
			primaryImage = modifyImageURL(urlImage);
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(String internalId, JSONArray arraySkus, Document doc) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();
	
		if(arraySkus.length() > 1){
			String url = HOME_PAGE + "produto/sku/" + internalId;
			String stringJsonImages = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, null); //GET request to get secondary images
			
			JSONObject jsonObjectImages;
			try {
				jsonObjectImages = new JSONArray(stringJsonImages).getJSONObject(0);
			} catch (JSONException e) {
				jsonObjectImages = new JSONObject();
				e.printStackTrace();
			}
			
			if (jsonObjectImages.has("Images")) {
				JSONArray jsonArrayImages = jsonObjectImages.getJSONArray("Images");
				
				for (int i = 1; i < jsonArrayImages.length(); i++) {				//starts with index 1, because the first image is the primary image
					JSONArray arrayImage = jsonArrayImages.getJSONArray(i);
					JSONObject jsonImage = arrayImage.getJSONObject(0);
					
					if(jsonImage.has("Path")){
						String urlImage = modifyImageURL(jsonImage.getString("Path"));
						secondaryImagesArray.put(urlImage);
					}
					
				}
			}
			
			/**
			* Product of single variations images
			*/
			
		} else {
			Elements images =  doc.select(".thumbs li a img");
			
			for(int i = 1; i < images.size(); i++){ // start with index 1 because the first image is the primary image
				Element e = images.get(i);
				
				secondaryImagesArray.put(modifyImageURL(e.attr("src")));
			}
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
		Elements elementCategories = document.select(".bread-crumb ul li a");

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
		Element descriptionElement = document.select("#descricao").first();
		Element specElement = document.select("#especificacao").first();

		if (descriptionElement != null) description = description + descriptionElement.html();
		if (specElement != null) description = description + specElement.html();

		return description;
	}
	
	/**
	 * Get the script having a json with the availability information
	 * @return
	 */
	private JSONArray crawlSkuJsonArray(Document document) {
		Elements scriptTags = document.getElementsByTag("script");
		JSONObject skuJson = null;
		JSONArray skuJsonArray = null;
		
		for (Element tag : scriptTags){                
			for (DataNode node : tag.dataNodes()) {
				if(tag.html().trim().startsWith("var skuJson_0 = ")) {
					skuJson = new JSONObject
							(
							node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1] +
							node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1].split(Pattern.quote("}]};"))[0]
							);

				}
			}        
		}
		if (skuJson != null && skuJson.has("skus")) {
			skuJsonArray = skuJson.getJSONArray("skus");
		} else {
			skuJsonArray = new JSONArray();
		}		
		
		return skuJsonArray;
	}
	
}
