package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
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

import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.core.task.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

/**
 * 1) Only one page per sku.
 * 2) No variations of skus in a page.
 * 3) No voltage, size or any other selector.
 * 4) InternalId is crawled from a json object embedded inside the html code.
 * this json object is located inside a <script> html tag and is called skuJson_0. 
 * 
 * @author samirleao
 *
 */
public class BrasilFnacCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.fnac.com.br/";

	public BrasilFnacCrawler(CrawlerSession session) {
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

		if ( isProductPage(this.session.getOriginalURL(), doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			JSONObject fullSkuJson = crawlSkuJsonArray(doc);

			// internal pid
			String internalPid = null;
			if (fullSkuJson.has("productId")) {
				internalPid = String.valueOf(fullSkuJson.getLong("productId"));
			}

			// categories
			ArrayList<String> categories = crawlCategories(doc);
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);

			// primary image
			String primaryImage = null;
			Elements imageElements = doc.select(".x-images #show .thumbs li a");
			if (imageElements.size() > 0) {
				String imageURL = imageElements.get(0).attr("rel");
				if (imageURL != null) {
					primaryImage = imageURL.trim();
				}
			}

			// secondary images
			String secondaryImages = null;
			JSONArray secondaryImagesArray = new JSONArray();
			if (imageElements.size() > 1) {
				for (int i = 1; i < imageElements.size(); i++) {
					String imageURL = imageElements.get(i).attr("rel");
					if (imageURL != null) {
						secondaryImagesArray.put(imageURL.trim());
					}
				}
			}
			if (secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// description
			String description = "";
			Element info = doc.select("#x-desc-info").first();
			Element specs = doc.select("#x-about").first();
			if (info != null) description += info.html();
			if (specs != null) description += specs.html();

			// stock
			Integer stock = null;

			// marketplace
			JSONArray marketplace = new JSONArray();

			JSONArray skus = fullSkuJson.getJSONArray("skus");
			
			// payment options
			Prices prices = crawlPrices(doc);

			for (int i = 0; i < skus.length(); i++) {

				JSONObject sku = skus.getJSONObject(i);

				// internal id
				String internalId = null;
				if (sku.has("sku")) {
					internalId = String.valueOf(sku.getLong("sku"));
				}				

				// name
				String name = crawlName(fullSkuJson, sku);

				// availability
				boolean available = false;
				if (sku.has("available")) {
					available = sku.getBoolean("available");
				}

				// price
				Float price = null;
				if (sku.has("bestPriceFormated") && available) {
					price = Float.parseFloat(sku.getString("bestPriceFormated").replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
				}

				Product product = new Product();
				
				product.setUrl(this.session.getOriginalURL());
				product.setInternalId(internalId);
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

	private boolean isProductPage(String url, Document document) {
		return (url.contains("/p") && document.select(".x-product-details").first() != null);
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(".x-product-details .x-breadcrumb ul li");

		for (int i = 1; i < elementCategories.size(); i++) { // start with index 1 becuase the first item is page home
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
	
	private Prices crawlPrices(Document doc) {
		Prices prices = new Prices();
		
		Map<Integer, Float> installments = new TreeMap<Integer, Float>();
		Elements installmentElements = doc.select("#x-cartao-credito .other-payment-method-ul li");
		if (installmentElements.size() > 0) {
			
			// the first installment is a different html element
			Element firstInstallmentElement = installmentElements.get(0);
			Float firstInstallmentPrice = Float.parseFloat(firstInstallmentElement.text().trim().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			installments.put(1, firstInstallmentPrice);
			
			// the first installment is also the bank ticket price (boleto bancario)
			prices.insertBankTicket(firstInstallmentPrice);
			
			// the others installments
			for (int i = 1; i < installmentElements.size(); i++) { // the first one is always the cash price (a vista)
				Element installmentElement = installmentElements.get(i);
				Element installmentNumberElement = installmentElement.select("span").first();
				Element installmentPriceElement = installmentElement.select("strong").first();
				Integer installmentNumber = null;
				Float installmentPrice = null;
				
				if (installmentNumberElement != null) {
					String installmentText = CommonMethods.parseNumbers(installmentNumberElement.text().trim()).get(0);
					if (installmentText != null && !installmentText.isEmpty()) {
						installmentNumber = Integer.parseInt(installmentText);
					}
				}
				if (installmentPriceElement != null) {
					installmentPrice = Float.parseFloat(installmentPriceElement.text().trim().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
				}
				
				if (installmentNumber != null && installmentPrice != null) {
					installments.put(installmentNumber, installmentPrice);
				}
			}
		}
		
		// set the payment options on Prices
		prices.insertCardInstallment(installments);
		
		return prices;
	}
	
	/**
	 * 
	 * @param fullSkuJson
	 * @param skuJson
	 * @return
	 */
	private String crawlName(JSONObject fullSkuJson, JSONObject skuJson) {
		String nameFullSkuJson = null;
		String nameSkuJson = null;
		
		// fullSkuJson name
		// eg: "name":"CASE LOGIC WMBP 115.01 MOCHILA PARA NOTEBOOK 15\" - PRETA"
		if (fullSkuJson.has("name")) {
			nameFullSkuJson = fullSkuJson.getString("name");
		}
		
		// skuJson name
		// eg: "skuname":"CASE LOGIC WMBP 115.01 MOCHILA PARA NOTEBOOK 15\" -"
		if (skuJson.has("skuname")) {
			nameSkuJson = skuJson.getString("skuname");
		}
		
		// to be future proof, and handle cases where the ecommerce will include more than
		// one product on the same page, the crawler must decide between these two names.
		// When we have variations the name must be different for each variation, and must have
		// the variation name appended to the origina base name. For this we must look for the biggest
		// name between the two. Until the first version of this crawler, the ecommerce has no pages with
		// more than one product to select. It's expected that with this approach, if the website starts to
		// include more than one product in the same page, the crawler will automatically handle it.
		if (nameFullSkuJson != null && nameSkuJson != null) {
			if (nameFullSkuJson.length() >= nameSkuJson.length()) return nameFullSkuJson;
		}
		return nameSkuJson;
	}


	/**
	 * Get the script having a json with the availability information.
	 * eg:
	 * 
	 * {
		"productId":692013,
		"name":"CASE LOGIC WMBP 115.01 MOCHILA PARA NOTEBOOK 15\" - PRETA",
		"salesChannel":"1",
		"available":true,
		"displayMode":"especificacao",
		"dimensions":[
		],
		"dimensionsInputType":{
		},
		"dimensionsMap":{
		},
		"skus":[
		{
		"sku":7066198,
		"skuname":"CASE LOGIC WMBP 115.01 MOCHILA PARA NOTEBOOK 15\" -",
		"dimensions":{
		},
		"available":true,
		"availablequantity":5,
		"cacheVersionUsedToCallCheckout":"596391fe885f5c0f09602ff20649b314_geral:6768E1225949CBC6E62EFD9EBBB3BF7B",
		"listPriceFormated":"R$ 169,00",
		"listPrice":16900,
		"taxFormated":"R$ 0,00",
		"taxAsInt":0,
		"bestPriceFormated":"R$ 152,10",
		"bestPrice":15210,
		"installments":3,
		"installmentsValue":5070,
		"installmentsInsterestRate":0,
		"image":"http://fnac.vteximg.com.br/arquivos/ids/384912-500-500/391-692013-0-5-case-logic-wmbp-115-01-mochila-para-notebook-15-preta.jpg",
		"sellerId":"1",
		"seller":"fnac",
		"measures":{
			"cubicweight":13.0781,
			"height":45.0000,
			"length":45.0000,
			"weight":100.0000,
			"width":31.0000
			},
		"unitMultiplier":1.0000,
		"rewardValue":0
		}
		]
		}
	 * 
	 * @return
	 */
	private JSONObject crawlSkuJsonArray(Document document) {
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

		return skuJson;
	}



}

