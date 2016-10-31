package br.com.lett.crawlernode.crawlers.brasil;

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

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;


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

	public BrasilClimarioCrawler(CrawlerSession session) {
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
				Float price = crawlMainPagePrice(jsonSku, available);

				// prices
				Prices prices = crawlPrices(doc);

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

	private Float crawlMainPagePrice(JSONObject json, boolean available) {
		Float price = null;

		if (json.has("bestPriceFormated") && available) {
			price = Float.parseFloat( json.getString("bestPriceFormated").replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		}

		return price;
	}

	private Prices crawlPrices(Document doc) {
		Prices prices = new Prices();
		Float bankTicketPrice = crawlBankTicketPrice(doc);
		Map<Integer, Float> installmentPriceMap = crawlCardInstallments(doc);

		prices.insertBankTicket(bankTicketPrice);
		prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);

		return prices;
	}
	
	/**
	 * Cartão.
	 * 
	 * @param doc
	 * @return
	 */
	private Map<Integer, Float> crawlCardInstallments(Document doc) {
		Map<Integer, Float> installmentPriceMap = new HashMap<Integer, Float>();
		Elements paymentElements = doc.select(".other-payment-method ul.other-payment-method-ul li");

		// 1x
		Element firstPrice = paymentElements.first();
		if (firstPrice != null) {
			Float price = Float.parseFloat( firstPrice.text().trim().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
			if (price != null) {
				installmentPriceMap.put(1, price);
			}
		}
		
		// get each installment number from the text inside the span tag
		// e.g : <span> 4X de</span>
		for (int i = 1; i < paymentElements.size(); i++) {
			Element span = paymentElements.get(i).select("span").first();
			if (span != null) {
				List<String> numbers = CommonMethods.parseNumbers(span.text());
				if (numbers.size() > 0) {
					
					// the installment number
					Integer installment = Integer.parseInt(numbers.get(0));
					
					// the installment value
					Float price = null;
					Element priceElement = paymentElements.get(i).select("strong").first();
					if (priceElement != null) {
						price = Float.parseFloat( priceElement.text().trim().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
					}
					
					if (installment != null && price != null) {
						installmentPriceMap.put(installment, price);
					}
				}
			}
			
		}

		return installmentPriceMap;
	}

	/**
	 * Boleto.
	 * 
	 * @return
	 */
	private Float crawlBankTicketPrice(Document doc) {
		Float price = null;
		Element paymentElement = doc.select(".other-payment-method ul.other-payment-method-ul li").first();
		if (paymentElement != null) {
			price = Float.parseFloat( paymentElement.text().trim().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		}
		return price;
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