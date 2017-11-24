
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
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;
import models.Marketplace;
import models.prices.Prices;

/************************************************************************************************************************************************************************************
 * Crawling notes (19/07/2016):
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
 * 6) The price of sku, found in json script, is wrong when the same is unavailable, then it is not
 * crawled.
 * 
 * 7) There is internalPid for skus in this ecommerce. The internalPid is a number that is the same
 * for all the variations of a given sku.
 * 
 * 7) The primary image is the first image on the secondary images.
 * 
 * 8) Variations of skus are not crawled if the variation is unavailable because it is not displayed
 * for the user, except if there is a html element voltage, because variations of voltage are
 * displayed for the user even though unavailable.
 * 
 * 9) The url of the primary images are changed to bigger dimensions manually.
 * 
 * 
 * Examples: ex1 (available):
 * http://loja.consul.com.br/freezer-horizontal-consul-519-litros-2-tampas-branco-chb53cb/p ex2
 * (unavailable):
 * http://loja.consul.com.br/condicionador-de-ar-consul-quente-frio-18000-btuh-cbu22db/p
 *
 *
 ************************************************************************************************************************************************************************************/

public class BrasilConsulCrawler extends Crawler {

	private final String HOME_PAGE = "http://loja.consul.com.br/";

	public BrasilConsulCrawler(Session session) {
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
		List<Product> products = new ArrayList<>();

		if (isProductPage(session.getOriginalURL())) {
			Logging.printLogDebug(logger, session,
					"Product page identified: " + this.session.getOriginalURL());

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
			Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap);

			// Secondary images
			String secondaryImages = crawlSecondaryImages(doc);

			// sku data in json
			JSONArray arraySkus = crawlSkuJsonArray(doc);

			for (int i = 0; i < arraySkus.length(); i++) {

				JSONObject jsonSku = arraySkus.getJSONObject(i);

				// Name
				String name = crawlName(doc, jsonSku);

				// Availability
				boolean available = crawlAvailability(jsonSku);

				// InternalId
				String internalId = crawlInternalId(jsonSku);

				// Price
				Float price = crawlPrice(jsonSku, available);

				// Prices
				Prices prices = crawlPrices(doc, price, jsonSku);

				// Primary image
				String primaryImage = crawlPrimaryImage(jsonSku);

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
		if (url.endsWith("/p") || url.contains("/p?attempt="))
			return true;
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

			if (!name.contains(nameVariation)) {
				name = name + " - " + nameVariation;
			}
		}

		return name;
	}

	private Float crawlPrice(JSONObject json, boolean available) {
		Float price = null;

		if (json.has("bestPriceFormated") && available) {
			price = Float.parseFloat(json.getString("bestPriceFormated").replaceAll("[^0-9,]+", "")
					.replaceAll("\\.", "").replaceAll(",", "."));
		}

		return price;
	}

	/**
	 * For the card payment options we must fetch a page whose URL is assembled using the sku id found
	 * inside the json object passed as parameter. This new page has all payment options, for all card
	 * brands, and it also contains the bank slip price, but this one is not the same as displayed to
	 * the user on the sku main page. The bank slip price on the sku main page is computed applying a
	 * discount using the best price as basis.
	 * 
	 * We are not considering that all payment options are the same for all card brands, as in the
	 * page containing the payment options we have one table for each card brands. On the examples
	 * used during this crawler development, all the tables were the same, but we want to make robust
	 * informations crawling ;)
	 * 
	 * @param document
	 * @param skuInformationJson
	 * @return
	 */
	private Prices crawlPrices(Document document, Float price, JSONObject skuInformationJson) {
		Prices prices = new Prices();

		// bank slip
		Float bankSlipPrice = crawlBankSlipPrice(document, skuInformationJson);
		if (bankSlipPrice != null) {
			if (bankSlipPrice.equals(price)) {
				prices.setBankTicketPrice(bankSlipPrice - (bankSlipPrice * 0.05));
			} else {
				prices.setBankTicketPrice(bankSlipPrice);
			}
		}

		// installments
		if (skuInformationJson.has("sku")) {

			// fetch the page with payment options
			String skuId = Integer.toString((skuInformationJson.getInt("sku"))).trim();
			String paymentOptionsURL = "http://loja.consul.com.br/productotherpaymentsystems/" + skuId;
			Document paymentOptionsDocument = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session,
					paymentOptionsURL, null, null);

			// get all cards brands
			List<String> cardBrands = new ArrayList<String>();
			Elements cardsBrandsElements =
					paymentOptionsDocument.select(".div-card-flag #ddlCartao option");
			for (Element cardBrandElement : cardsBrandsElements) {
				String cardBrandText = cardBrandElement.text().toLowerCase();
				if (cardBrandText.contains("american express")
						|| cardBrandText.contains(Card.AMEX.toString()))
					cardBrands.add(Card.AMEX.toString());
				else if (cardBrandText.contains(Card.VISA.toString()))
					cardBrands.add(Card.VISA.toString());
				else if (cardBrandText.contains(Card.DINERS.toString()))
					cardBrands.add(Card.DINERS.toString());
				else if (cardBrandText.contains(Card.MASTERCARD.toString()))
					cardBrands.add(Card.MASTERCARD.toString());
				else if (cardBrandText.contains(Card.HIPERCARD.toString()))
					cardBrands.add(Card.HIPERCARD.toString());
			}

			// get each table payment option in the same sequence as we got the cards brands (the html
			// logic was this way)
			Elements paymentElements =
					paymentOptionsDocument.select("#divCredito .tbl-payment-system tbody");

			for (int i = 0; i < cardBrands.size(); i++) {
				if (paymentElements.size() > i) {
					Element paymentElement = paymentElements.get(i);
					Map<Integer, Float> installments = crawlInstallmentsFromTableElement(paymentElement);
					prices.insertCardInstallment(cardBrands.get(i), installments);
				}
			}
		}


		return prices;
	}

	/**
	 * The bank slip price must be calculated, using the sku best price as basis. De: R$ 2.799,00 Por:
	 * R$ 2.789,00 (this is the sku best price)
	 * 
	 * For the discount value we must look for a particular html element, indicating the percentag
	 * evalue to apply as discount. It was observed that all the discounts in bank slip are 5%. The
	 * html element is ".flag.-cns--desconto-5--boleto". If this element is found, then we apply the
	 * 5% discount on the best price, otherwise we consider the bestPrice as being the bank slip
	 * price.
	 * 
	 * @param document
	 * @param skuInformationJson
	 * @return
	 */
	private Float crawlBankSlipPrice(Document document, JSONObject skuInformationJson) {
		Float bankSlipPrice = null;

		// check availability
		boolean skuIsAvailable = false;
		if (skuInformationJson.has("available")) {
			skuIsAvailable = skuInformationJson.getBoolean("available");
		}

		if (skuIsAvailable) {
			if (skuInformationJson.has("bestPriceFormated")) {
				String bestPriceString = skuInformationJson.getString("bestPriceFormated");
				Float bestPrice = MathCommonsMethods.parseFloat(bestPriceString);

				// get discount to apply on calculation
				Element discountElement = document.select(".flag.-cns--desconto-5--boleto").first();
				if (discountElement != null) {
					bankSlipPrice =
							MathCommonsMethods.normalizeTwoDecimalPlaces(bestPrice - (0.05f * bestPrice));
				} else {
					bankSlipPrice = bestPrice;
				}
			}
		}

		return bankSlipPrice;
	}

	/**
	 * 
	 * 
	 * 
	 * e.g: Nº de Parcelas Valor de cada parcela American Express à vista R$ 1.799,00 American Express
	 * 2 vezes sem juros R$ 899,50 American Express 3 vezes sem juros R$ 599,66 American Express 4
	 * vezes sem juros R$ 449,75 American Express 5 vezes sem juros R$ 359,80 American Express 6 vezes
	 * sem juros R$ 299,83 American Express 7 vezes sem juros R$ 257,00 American Express 8 vezes sem
	 * juros R$ 224,87 American Express 9 vezes sem juros R$ 199,88 American Express 10 vezes sem
	 * juros R$ 179,90 American Express 11 vezes com juros R$ 173,41 American Express 12 vezes com
	 * juros R$ 159,73
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
					installments.put(Integer.parseInt(parsedNumbers.get(0)),
							MathCommonsMethods.parseFloat(installPriceText));
				}
			}
		}

		return installments;
	}

	private boolean crawlAvailability(JSONObject json) {

		if (json.has("available"))
			return json.getBoolean("available");

		return false;
	}

	private Map<String, Float> crawlMarketplace(Document document) {
		return new HashMap<String, Float>();
	}

	private Marketplace assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		return new Marketplace();
	}

	private String crawlPrimaryImage(JSONObject json) {
		String primaryImage = null;

		if (json.has("image")) {
			String urlImage = json.getString("image");
			primaryImage = modifyImageURL(urlImage);
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document doc) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements images = doc.select(".thumbs li a img");

		for (int i = 1; i < images.size(); i++) { // start with index 1 because the first image is the
																							// primary image
			secondaryImagesArray.put(modifyImageURL(images.get(i).attr("src")));
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}


	private String modifyImageURL(String url) {
		String[] tokens = url.trim().split("/");
		String dimensionImage = tokens[tokens.length - 2]; // to get dimension image and the image id

		String[] tokens2 = dimensionImage.split("-"); // to get the image-id
		String dimensionImageFinal = tokens2[0] + "-1000-1000";

		String urlReturn = url.replace(dimensionImage, dimensionImageFinal); // The image size is
																																					// changed

		return urlReturn;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(".prod-info .breadcrumb .bread-crumb > ul li a");

		for (int i = 1; i < elementCategories.size(); i++) { // starting from index 1, because the first
																													// is the market name
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

	private String crawlDescription(Document document) {
		String description = "";
		Element descriptionElement = document.select(".productDescription").first();
		Element specElement = document.select("#caracteristicas").first();

		if (descriptionElement != null) {
			description = description + descriptionElement.html();
		}
		if (specElement != null) {
			description = description + specElement.html();
		}

		return description;
	}

	/**
	 * Get the script having a json with the availability information
	 * 
	 * @return
	 */
	private JSONArray crawlSkuJsonArray(Document document) {
		Elements scriptTags = document.getElementsByTag("script");
		JSONObject skuJson = new JSONObject();
		JSONArray skuJsonArray = new JSONArray();

		for (Element tag : scriptTags) {
			for (DataNode node : tag.dataNodes()) {
				if (tag.html().trim().startsWith("var skuJson_0 = ")) {

					skuJson = new JSONObject(node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1]
							+ node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1]
									.split(Pattern.quote("}]};"))[0]);

				}
			}
		}

		if (skuJson.has("skus")) {
			try {
				skuJsonArray = skuJson.getJSONArray("skus");
			} catch (Exception e) {
				Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
			}
		}

		return skuJsonArray;
	}
}
