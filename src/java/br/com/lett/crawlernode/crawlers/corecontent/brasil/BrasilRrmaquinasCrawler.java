package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;


/************************************************************************************************************************************
 * Crawling notes (22/08/2016):
 * 
 * 1) For this crawler, we have one URL for multiple skus.
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 4) The sku page identification is done simply looking for an specific html element.
 * 5) If the sku is unavailable, it's price is displayed.
 * 6) In json script in html has variations of product.
 * 7) There is internalPid for skus in this ecommerce. The internalPid is a number that is the same for all
 * the variations of a given sku.
 * 7) The primary image is the first image on the secondary images.
 * 8) To get internalId and name in case of sku variations on the same page we crawl a json object inside the html.
 * 
 * Price crawling notes:
 * 1) It wasn't observed any price change between sku variations on the same page. The only type of variation found is
 * in voltage, where we have to select a 110V or a 220V sku.
 * 
 * 
 * Examples:
 * ex1 (available): http://www.rrmaquinas.com.br/bomba-d-agua-pressurizadora-220v-1600l-hora-3-4-bpf15-9-120-ferrari.html
 * ex2 (unavailable): http://www.rrmaquinas.com.br/ar-condicionado-portatil-12000-btus-piu-quente-frio-olimpia-splendid.html
 * ex3 (with variations): http://www.rrmaquinas.com.br/furadeira-1-2-600w-gbm-13-re-bosch.html
 ************************************************************************************************************************************/

public class BrasilRrmaquinasCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.rrmaquinas.com.br/";

	public BrasilRrmaquinasCrawler(Session session) {
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

		if ( isProductPage(doc) ) {

			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			/* ***********************************
			 * crawling data of only one product *
			 *************************************/

			// Pid
			String internalPid = crawlInternalPid(doc);

			// Name
			String nameMainPage = crawlName(doc);

			// Price
			Float priceMainPage = crawlPrice(doc);
			
			// Prices
			Prices prices = crawlPrices(doc);

			// Availability
			boolean available = crawlAvailability(doc);

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

			// Marketplace map
			Map<String, Float> marketplaceMap = crawlMarketplace(doc);

			// Marketplace
			JSONArray marketplace = assembleMarketplaceFromMap(marketplaceMap);

			// Sku variations
			Element skus = doc.select(".product-options").first();

			if(skus != null){

				/* ***********************************
				 * crawling data of mutiple products *
				 *************************************/

				// Sku Json
				JSONArray jsonSku = this.crawlJsonVariations(doc);

				for (int i = 0; i < jsonSku.length(); i++) {

					JSONObject sku = jsonSku.getJSONObject(i);

					// InternalId
					String internalID = crawlInternalIdForMutipleVariations(sku);

					// Name
					String name = crawlNameForMutipleVariations(sku, nameMainPage);

					// Creating the product
					Product product = new Product();

					product.setUrl(this.session.getOriginalURL());
					product.setInternalId(internalID);
					product.setInternalPid(internalPid);
					product.setName(name);
					product.setPrice(priceMainPage);
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

				/* *********************************
				 * crawling data of single product *
				 ***********************************/

			} else {

				// InternalId
				String internalID = crawlInternalIdSingleProduct(doc);

				// Creating the product
				Product product = new Product();

				product.setUrl(this.session.getOriginalURL());
				product.setInternalId(internalID);
				product.setInternalPid(internalPid);
				product.setName(nameMainPage);
				product.setPrice(priceMainPage);
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

	private boolean isProductPage(Document document) {
		if ( document.select(".product-view").first() != null ) return true;
		return false;
	}

	/*********************
	 * Variation methods *
	 *********************/

	private String crawlInternalIdForMutipleVariations(JSONObject sku) {
		String internalId = null;

		if(sku.has("products")){
			internalId = sku.getJSONArray("products").getString(0).trim();
		}

		return internalId;
	}

	private String crawlNameForMutipleVariations(JSONObject jsonSku, String name) {
		String nameVariation = name;	

		if(jsonSku.has("label")){
			nameVariation = nameVariation + " - " + jsonSku.getString("label");
		}	

		return nameVariation;
	}

	/**********************
	 * Single Sku methods *
	 **********************/

	private String crawlInternalIdSingleProduct(Document document) {
		String internalId = null;
		Element internalIdElement = document.select(".no-display input[name=product]").first();

		if (internalIdElement != null) {
			internalId = internalIdElement.attr("value").trim();
		}

		return internalId;
	}

	/*******************
	 * General methods *
	 *******************/

	private Float crawlPrice(Document doc) {
		Float price = null;	
		Element priceElement = doc.select("#formas-pagamento-box ul li span").first();

		if(priceElement != null){
			price = Float.parseFloat(priceElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
		}	

		return price;
	}

	private Prices crawlPrices(Document document) {
		Prices prices = new Prices();

		// bank slip
		Float bankSlipPrice = crawlBankSlipPrice(document);
		if (bankSlipPrice != null) {
			prices.setBankTicketPrice(bankSlipPrice);
		}
		
		// installments
		Map<Integer, Float> installments = new TreeMap<Integer, Float>();
		Elements installmentElements = document.select("#formas-pagamento-box ul li");
		for (Element installmentElement : installmentElements) {
			Element installmentPriceElement = installmentElement.select("span.price").first();
			if (installmentPriceElement != null) {
				String installmentNumberText = installmentElement.text();
				String installmentPriceText = installmentPriceElement.text();
				
				List<String> parsedNumbers = MathCommonsMethods.parsePositiveNumbers(installmentNumberText);
				if (parsedNumbers.size() > 0) {
					Integer installmentNumber = Integer.parseInt(parsedNumbers.get(0));
					Float installmentPrice = MathCommonsMethods.parseFloat(installmentPriceText);
					
					installments.put(installmentNumber, installmentPrice);
				}
			}
		}
		
		if (installments.size() > 0) {
			prices.insertCardInstallment(Card.VISA.toString(), installments);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installments);
			prices.insertCardInstallment(Card.HIPERCARD.toString(), installments);
			prices.insertCardInstallment(Card.AMEX.toString(), installments);
			prices.insertCardInstallment(Card.DINERS.toString(), installments);
			prices.insertCardInstallment(Card.ELO.toString(), installments);
		}

		return prices;
	}
	
	private Float crawlBankSlipPrice(Document document) {
		Float bankSlipPrice = null;
		Element bankSlipPriceElement = document.select(".product-shop span.desconto span.price").first();
		if (bankSlipPriceElement != null) {
			String bankSlipPriceText = bankSlipPriceElement.text();
			if (!bankSlipPriceText.isEmpty()) {
				bankSlipPrice = MathCommonsMethods.parseFloat(bankSlipPriceElement.text());
			}
		}
		return bankSlipPrice;
	}

	private boolean crawlAvailability(Document doc) {
		Element e = doc.select(".alert-stock").first();

		if (e != null) {
			return false;
		}

		return true;
	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;

		Element internalPidElement = document.select(".product-essential p.right").first();

		if (internalPidElement != null) {
			String pid = internalPidElement.text().toString().trim();	

			int x = pid.indexOf(":");
			internalPid = pid.substring(x+1).trim();
		}

		return internalPid;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select(".product-name").first();

		if (nameElement != null) {
			name = nameElement.text().toString().trim();
		}

		return name;
	}

	private Map<String, Float> crawlMarketplace(Document document) {
		return new HashMap<String, Float>();
	}

	private JSONArray assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		return new JSONArray();
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select(".product-image a").first();

		if (primaryImageElement != null) {
			primaryImage = primaryImageElement.attr("href").trim();
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select(".more-views ul li a");

		for (int i = 1; i < imagesElement.size(); i++) { // start with indez 1 because the first image is the primary image
			Element e = imagesElement.get(i);

			secondaryImagesArray.put( e.attr("href").trim() );
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(".breadcrumbs li a");

		for (int i = 1; i < elementCategories.size(); i++) { // start with index 1 because the first item is the home page
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
		Element descriptionElement = document.select("#tab1").first();

		if (descriptionElement != null) description = description + descriptionElement.html();

		return description;
	}

	/**
	 * Crawl a json array containing sku informations, inside the html.
	 * This json array is crawled in the cases we have more than one sku variation
	 * on the same page, for example, a voltage selector.
	 * 
	 * The json of each sku inside the array is used to get internalId and name.
	 * 
	 * e.g:
	 * [
	 * 	{"id":"242","price":"0","label":"110V","products":["3000"]},
	 * 	{"id":"241","price":"0","label":"220V","products":["3001"]}
	 * ]
	 * 
	 * @param doc
	 * @return
	 */
	private JSONArray crawlJsonVariations(Document doc) {
		JSONArray jsonSku = new JSONArray();
		Elements scripts = doc.select(".product-options script");
		String id = doc.select("select.super-attribute-select").first().attr("id").replaceAll("[^0-9]", "").trim();

		String term = "Product.Config(";

		for (Element e : scripts) {
			String script = e.outerHtml().trim();

			if (script.contains(term)) {
				int x = script.indexOf(term);
				int y = script.indexOf(");", x + term.length());

				JSONObject json = new JSONObject(script.substring(x + term.length(), y).trim());

				if (json.has("attributes")) {
					json = json.getJSONObject("attributes");

					if (json.has(id)) {
						json = json.getJSONObject(id);

						if (json.has("options")) {
							jsonSku = json.getJSONArray("options");
						}
					}
				}
				break;
			}
		}

		return jsonSku;
	}

}
