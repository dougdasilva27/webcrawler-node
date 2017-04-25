package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;


/*********************************************************************************************************************************************
 * Crawling notes (12/07/2016):
 * 
 * 1) For this crawler, we have one url per each sku
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made
 * 3) There is no marketplace in this ecommerce by the time this crawler was made
 * 4) The sku page identification is done simply looking for an specific html element
 * 5) if the sku is unavailable, it's price is not displayed.
 * 6) There is no internalPid for skus in this ecommerce. The internalPid must be a number that is the same for all
 * the variations of a given sku
 * 7) For the availability we crawl a script in the html. A script that has a variable named skuJson_0. It's been a common
 * script, that contains a jsonObject with certain informations about the sku. It's used only when the information needed
 * is too complicated to be crawled by normal means, or inexistent in other place. Although this json has other informations
 * about the sku, only the availability is crawled this way in this website.
 * 8) All the images are .png
 * 9) We have one method for each type of information for a sku (please carry on with this pattern).
 * 
 * Examples:
 * ex1 (available): http://www.multiar.com.br/condicionador-de-ar-split-consul-frio-9-000-btu-h-2001105/p
 * ex2 (unavailable): http://www.multiar.com.br/ar-condicionado-split-hi-wall-inverter-daikin-advance-18000-btus-frio-220v-ftk18p5vl-rk18p5vl/p
 *
 **********************************************************************************************************************************************/

public class BrasilMultiarCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.multiar.com.br/";

	private final String INTERNALID_SELECTOR 								= "#___rc-p-id";
	private final String INTERNALID_SELECTOR_ATTRIBUTE						= "value";

	private final String NAME_SELECTOR 										= ".fn.productName";
	private final String PRICE_SELECTOR 									= ".plugin-preco .preco-a-vista .skuPrice";

	private final String PRIMARY_IMAGE_SELECTOR 							= "#image a";
	private final String PRIMARY_IMAGE_SELECTOR_ATTRIBUTE 					= "href";

	private final String SECONDARY_IMAGES_SELECTOR 							= ".thumbs li a";
	private final String SECONDARY_IMAGES_SELECTOR_ATTRIBUTE				= "zoom";

	private final String CATEGORIES_SELECTOR 								= ".bread-crumb ul li a";
	
	private final String DESCRIPTION_SELECTOR 								= "#descricao";
	private final String SPECS_SELECTOR										= "#especificacoes";

	public BrasilMultiarCrawler(Session session) {
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

		if ( isProductPage(this.session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			/* ***********************************
			 * crawling data of only one product *
			 *************************************/
			
			// crawl the skuJson
			JSONObject skuJson = crawlSkuJson(doc);

			// InternalId
			String internalId = crawlInternalId(doc);

			// Pid
			String internalPid = crawlInternalPid(doc);

			// Name
			String name = crawlName(doc);
			
			// Price
			Float price = crawlMainPagePrice(doc);

			// Availability
			boolean available = crawlAvailability(skuJson);

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

			// Stock
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = crawlMarketplace(doc);

			// Prices
			Prices prices = crawlPrices(price, doc, skuJson);
			
			// Creating the product
			Product product = new Product();
			product.setUrl(this.session.getOriginalURL());
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

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}
		
		return products;
	}



	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		if ( url.startsWith("http://www.multiar.com.br/") && url.endsWith("/p") ) return true;
		return false;
	}


	/*******************
	 * General methods *
	 *******************/

	private String crawlInternalId(Document document) {
		String internalId = null;
		Element internalIdElement = document.select(INTERNALID_SELECTOR).first();

		if (internalIdElement != null) {
			internalId = internalIdElement.attr(INTERNALID_SELECTOR_ATTRIBUTE).trim();
		}

		return internalId;
	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;
		return internalPid;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select(NAME_SELECTOR).first();

		if (nameElement != null) {
			name = sanitizeName(nameElement.text());
		}

		Element modelName = document.select(".productReference").first();

		if(modelName != null) {
			name = name + " " + modelName.text();
		}

		return name;
	}

	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element mainPagePriceElement = document.select(PRICE_SELECTOR).first();
		
		if (mainPagePriceElement != null) {
			price = Float.parseFloat( mainPagePriceElement.text().trim().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		}

		return price;
	}

	private boolean crawlAvailability(JSONObject skuJson) {
		if (skuJson != null && skuJson.has("skus")) {
			JSONArray skus = skuJson.getJSONArray("skus");
			if (skus.length() > 0) {
				JSONObject sku = skus.getJSONObject(0);
				if (sku.has("available")) {
					return sku.getBoolean("available");
				}
			}
		}
		return false;
	}

	private JSONArray crawlMarketplace(Document document) {
		return new JSONArray();
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select(PRIMARY_IMAGE_SELECTOR).first();

		if (primaryImageElement != null) {
			primaryImage = primaryImageElement.attr(PRIMARY_IMAGE_SELECTOR_ATTRIBUTE);
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select(SECONDARY_IMAGES_SELECTOR);
	
		for (int i = 1; i < imagesElement.size(); i++) { // starting from index 1, because the first is the primary image
			secondaryImagesArray.put(imagesElement.get(i).attr(SECONDARY_IMAGES_SELECTOR_ATTRIBUTE));
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(CATEGORIES_SELECTOR);

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
		Element descriptionElement = document.select(DESCRIPTION_SELECTOR).first();
		Element specsElement = document.select(SPECS_SELECTOR).first();
		if (descriptionElement != null) description = description + descriptionElement.html();
		if (specsElement != null) description = description + specsElement.html();

		return description;
	}

	
	/**
	 * To crawl this prices is accessed a api
	 * Is removed all accents for crawl price 1x like this:
	 * Visa à vista	R$ 1.790,00
	 * 
	 * @param internalId
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(Float price, Document doc, JSONObject skuJson){
		Prices prices = new Prices();

		if(price != null){
			// Pegando o id para acessar a página de preços
			String id = null;
			if (skuJson != null && skuJson.has("skus")) {
				JSONArray skus = skuJson.getJSONArray("skus");
				if (skus.length() > 0) {
					JSONObject sku = skus.getJSONObject(0);
					
					if(sku.has("sku")){
						id = Integer.toString(sku.getInt("sku"));
					}
				}
			}
			
			if(id != null){
				String url = "http://www.multiar.com.br/productotherpaymentsystems/" + id;
	
				Document docPrices = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, cookies);
					
				Float bankTicketPrice = calculateBankTicketPrice(doc, price);
				prices.insertBankTicket(bankTicketPrice);
				
				Elements cardsElements = docPrices.select("#ddlCartao option");
	
				for(Element e : cardsElements){
					String text = e.text().toLowerCase();
	
					if (text.contains("visa")) {
						Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"), price, doc);
						prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
						
					} else if (text.contains("mastercard")) {
						Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"), price, doc);
						prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
						
					} else if (text.contains("diners")) {
						Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"), price, doc);
						prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
						
					} else if (text.contains("american") || text.contains("amex")) {
						Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"), price, doc);
						prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);	
						
					} else if (text.contains("hipercard")) {
						Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"), price, doc);
						prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);	
						
					} else if (text.contains("credicard") ) {
						Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"), price, doc);
						prices.insertCardInstallment(Card.CREDICARD.toString(), installmentPriceMap);
						
					} else if (text.contains("elo") ) {
						Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"), price, doc);
						prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
						
					} else if (text.contains("aura") ) {
						Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"), price, doc);
						prices.insertCardInstallment(Card.AURA.toString(), installmentPriceMap);
						
					} else if (text.contains("discover") ) {
						Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"), price, doc);
						prices.insertCardInstallment(Card.DISCOVER.toString(), installmentPriceMap);
						
					}
				} 
			}

		}

		return prices;
	}

	private Map<Integer,Float> getInstallmentsForCard(Document doc, String idCard, Float price, Document docMain){
		Map<Integer,Float> mapInstallments = new HashMap<>();

		Elements installmentsCard = doc.select(".tbl-payment-system#tbl" + idCard + " tr");
		for(Element i : installmentsCard){
			Element installmentElement = i.select("td.parcelas").first();

			if(installmentElement != null){
				String textInstallment = removeAccents(installmentElement.text().toLowerCase());
				Integer installment = null;

				if(textInstallment.contains("vista")){
					installment = 1;
					Float value = calculate1xCard(docMain, price);

					mapInstallments.put(installment, value);
				} else {
					installment = Integer.parseInt(textInstallment.replaceAll("[^0-9]", "").trim());
					
					Element valueElement = i.select("td:not(.parcelas)").first();

					if(valueElement != null){
						Float value = Float.parseFloat(valueElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());

						mapInstallments.put(installment, value);
					}
				}
			}
		}

		return mapInstallments;
	}

	// O preço no boleto não aparece com javascript desligado, mas aparece a porcentagem de desconto
	// Assim é calculado o preço no boleto de acordo com o preço principal.
	private Float calculateBankTicketPrice(Document doc, Float price){
		Float bankTicketPrice = null;
		Elements discounts = doc.select(".list-inline li > p[class]");
		
		for(Element e : discounts){
			String text = e.text().toLowerCase();
			
			if(text.contains("boleto")){
				Integer discount = Integer.parseInt(text.replaceAll("[^0-9]", "").trim());
				Float result = (float) (price - (price * (discount.floatValue()/100.0)));
				
				bankTicketPrice = MathCommonsMethods.normalizeTwoDecimalPlaces(result);
				break;
			}
		}
		
		return bankTicketPrice;
	}
	
	// O preço de 1x no cartão não aparece com javascript desligado na pagina principal, mas aparece a porcentagem de desconto
	// Assim é calculado o preço no boleto de acordo com o preço principal.
	// O preço correspondente na página acessada de preços, está errado, com isso necessita ser calculado.
	private Float calculate1xCard(Document doc, Float price){
		Float oneXCard = null;
		Elements discounts = doc.select(".list-inline li > p[class]");
		
		for(Element e : discounts){
			String text = removeAccents(e.text().toLowerCase());
			
			if(text.contains("credito")){
				Integer discount = Integer.parseInt(text.replaceAll("[^0-9]", "").trim());
				Float result = (float) (price - (price * (discount.floatValue()/100.0)));
				
				oneXCard = MathCommonsMethods.normalizeTwoDecimalPlaces(result);
				break;
			}
		}
		
		return oneXCard;
	}
	
	private String removeAccents(String str) {
		str = Normalizer.normalize(str, Normalizer.Form.NFD);
		str = str.replaceAll("[^\\p{ASCII}]", "");
		return str;
	}
	
	/**************************
	 * Specific manipulations *
	 **************************/

	private String sanitizeName(String name) {
		return name.replace("'","").replace("’","").trim();
	}
	
	/**
	 * Get the script having a json with the availability information
	 * @return
	 */
	private JSONObject crawlSkuJson(Document document) {
		Elements scriptTags = document.getElementsByTag("script");
		JSONObject skuJson = null;
		
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
		
		return skuJson;
	}

}
