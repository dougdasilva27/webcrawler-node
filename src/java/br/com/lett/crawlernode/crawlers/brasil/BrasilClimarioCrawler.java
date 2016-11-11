package br.com.lett.crawlernode.crawlers.brasil;

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

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;


/************************************************************************************************************************************************************************************
 * Crawling notes (04/10/2016):
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
 * 8) To get the internal_id is necessary to get a json , where internal_id is an attribute " sku ".
 * 
 * Examples:
 * ex1 (available): http://www.climario.com.br/ar-condicionado-de-janela-elgin-21000btuh-220-monofasico-frio-mecanico/p
 * ex2 (unavailable): http://www.climario.com.br/ar-condicionado-split-elgin-high-wall-18k-220v-frior410a/p
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilClimarioCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.climario.com.br/";

	public BrasilClimarioCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}


	@Override
	public List<Product>  extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(doc) ) {
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

				// prices
				Prices prices = crawlPrices(jsonSku, available);

				// Primary image
				String primaryImage = crawlPrimaryImage(doc);

				// Name
				String name = crawlName(doc, jsonSku);

				// Secondary images
				String secondaryImages = crawlSecondaryImages(doc);

				// Creating the product
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

	private boolean isProductPage(Document document) {
		if ( document.select(".row-product-info").first() != null ) return true;
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

		String mainPageName = null;
		Element nameElement = document.select(".productName").first();
		if (nameElement != null) {
			mainPageName = nameElement.text().trim();
		}

		String skuVariationName = null;
		if (jsonSku.has("skuname")) {
			skuVariationName = jsonSku.getString("skuname");
		}

		if (skuVariationName == null) return mainPageName;

		if (mainPageName != null) {
			name = mainPageName;

			if (name.length() > skuVariationName.length()) {
				name += skuVariationName;
			} else {
				name = skuVariationName;
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

	private Prices crawlPrices(JSONObject skuInformationsJson, boolean available) {
		Prices prices = new Prices();

		if (available) {

			// bank slip
			Float bankSlipPrice = null;
			if (skuInformationsJson.has("bestPriceFormated") && available) {
				bankSlipPrice = Float.parseFloat( skuInformationsJson.getString("bestPriceFormated").replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
				prices.insertBankTicket(bankSlipPrice);
			}

			// installments
			Document installmentsPricesDocument = fetchInstallmentPricesOptions(skuInformationsJson);
			if (installmentsPricesDocument != null) {
				Elements cardInstallmentsElements = installmentsPricesDocument.select(".tbl-payment-system"); // one for each card brand
				for (Element cardInstallmentElement : cardInstallmentsElements) {
					String cardName = getCardNameFromInstallmentLineElement(cardInstallmentElement);
					if (cardName != null) {
						prices.insertCardInstallment(cardName, getInstallmentsAndValuesFromElement(cardInstallmentElement));
					}
				}
			}
		}

		return prices;
	}

	private String getCardNameFromInstallmentLineElement(Element cardInstallmentElement) {
		String cardName = null;
		Element tableLine = cardInstallmentElement.select("tr.even").last();
		if (tableLine != null) {
			Element parcelasElement = tableLine.select(".parcelas").first();
			if (parcelasElement != null) {
				String text = parcelasElement.text().toLowerCase();
				if (text.contains(Card.AMEX.toString()) || text.contains("american express")) return Card.AMEX.toString();
				else if (text.contains(Card.VISA.toString())) return Card.VISA.toString();
				else if (text.contains(Card.DINERS.toString())) return Card.DINERS.toString();
				else if (text.contains(Card.MASTERCARD.toString())) return Card.MASTERCARD.toString();
				else if (text.contains(Card.ELO.toString())) return Card.ELO.toString();
				else if (text.contains(Card.HIPERCARD.toString())) return Card.HIPERCARD.toString();
			}
		}

		return cardName;
	}

	/**
	 * 	
	 * Nº de Parcelas	Valor de cada parcela (table header)
	 *	American Express à vista	R$ 1.205,00
	 *	American Express 2 vezes sem juros	R$ 602,50
	 *	American Express 3 vezes sem juros	R$ 401,66
	 *	American Express 4 vezes sem juros	R$ 301,25
	 *	American Express 5 vezes sem juros	R$ 241,00
	 *	American Express 6 vezes sem juros	R$ 200,83
	 *	American Express 7 vezes sem juros	R$ 172,14
	 *	American Express 8 vezes sem juros	R$ 150,62
	 *	American Express 9 vezes sem juros	R$ 133,88
	 *	American Express 10 vezes sem juros	R$ 120,50
	 *	American Express 11 vezes sem juros	R$ 109,54
	 *	American Express 12 vezes sem juros	R$ 100,41
	 *
	 * @param cardInstallmentElement
	 * @return
	 */
	private Map<Integer, Float> getInstallmentsAndValuesFromElement(Element cardInstallmentElement) {
		Map<Integer, Float> installments = new TreeMap<Integer, Float>();

		Elements installmentsTableLinesElements = cardInstallmentElement.select("tbody tr");
		for (int i = 1; i < installmentsTableLinesElements.size(); i++) { // the first element is just the table header
			Element tableLineElement = installmentsTableLinesElements.get(i);
			
            //<tr>
			//	<td class="parcelas">American Express à vista</td>
			//	<td>R$   1.205,00</td>
            //</tr>
			
			Element installmentNumberElement = tableLineElement.select("td").first();
			Element installmentPriceElement = tableLineElement.select("td").last();
			if (installmentNumberElement != null && installmentPriceElement != null) {
				Integer installmentNumber = null;
				Float installmentPrice = null;
				String installmentNumberLineText = installmentNumberElement.text();
				String installmentPriceLineText = installmentPriceElement.text();
				
				// get the installment number from the line
				List<String> numbersParsedFromLine = MathCommonsMethods.parseNumbers(installmentNumberLineText);
				if (numbersParsedFromLine.size() == 0) installmentNumber = 1;
				else {
					installmentNumber = Integer.parseInt(numbersParsedFromLine.get(0));
				}
				
				// get the installment price from the line
				installmentPrice = MathCommonsMethods.parseFloat(installmentPriceLineText);
				
				installments.put(installmentNumber, installmentPrice);
			}
		}

		return installments;
	}

	private Document fetchInstallmentPricesOptions(JSONObject skuInformationJson) {
		String skuInternalId = crawlInternalId(skuInformationJson);		
		if (skuInternalId != null) {
			String url = "http://www.climario.com.br/productotherpaymentsystems/" + skuInternalId;
			return DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, null);
		}
		return null;
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

	private String crawlPrimaryImage(Document doc) {
		String primaryImage = null;

		Element image = doc.select(".image-zoom").first();

		if (image != null) {
			primaryImage = image.attr("href");
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document doc) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements images = doc.select("#botaoZoom");

		for (int i = 1; i < images.size(); i++) {				//starts with index 1, because the first image is the primary image
			Element e = images.get(i);

			if(e.hasAttr("zoom")){
				String urlImage = e.attr("zoom");

				if(!urlImage.startsWith("http")){
					urlImage = e.attr("rel");
				}

				secondaryImagesArray.put(urlImage);
			}
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
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
		Element specElement = document.select("#caracteristicas").first();

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
							(node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1] +
									node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1].split(Pattern.quote("}]};"))[0]
									);

				}
			}        
		}

		try {
			skuJsonArray = skuJson.getJSONArray("skus");
		} catch(Exception e) {
			e.printStackTrace();
		}

		return skuJsonArray;
	}
}