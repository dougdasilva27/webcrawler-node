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
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

/*********************************************************************************************************************
 * Crawling notes (19/08/2016):
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
 * 
 * Examples:
 * ex1 (available): http://www.lebes.com.br/gaveteiro-madesa-tutti-colors-34256p1a-rosa-se-568992/p
 * ex2 (unavailable): http://www.lebes.com.br/receptor-analogico-century-nanobox-sem-antena-557510/p
 *
 *
 *******************************************************************************************************************/

public class BrasilLebesCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.lebes.com.br/";

	public BrasilLebesCrawler(CrawlerSession session) {
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

		if ( isProductPage(doc, session.getOriginalURL()) ) {

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

			// Primary image
			String primaryImage = crawlPrimaryImage(doc);

			// Secondary images
			String secondaryImages = crawlSecondaryImages(doc);

			// Stock
			Integer stock = null;

			// Marketplace map
			Map<String, Float> marketplaceMap = crawlMarketplace(doc);

			// Marketplace
			JSONArray marketplace = assembleMarketplaceFromMap(marketplaceMap);

			// sku data in json
			JSONArray arraySkus = crawlSkuJsonArray(doc);			

			for (int i = 0; i < arraySkus.length(); i++) {
				JSONObject jsonSku = arraySkus.getJSONObject(i);

				// Availability
				boolean available = crawlAvailability(jsonSku);

				// InternalId 
				String internalId = crawlInternalId(jsonSku);

				// Price
				Float price = crawlMainPagePrice(jsonSku, available);

				// Name
				String name = crawlName(doc, jsonSku);

				// Creating the product
				Product product = new Product();
				product.setUrl(session.getOriginalURL());
				product.setInternalId(internalId);
				product.setInternalPid(internalPid);
				product.setName(name);
				product.setPrice(price);
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

	private boolean isProductPage(Document document, String url) {
		if ( document.select(".productName").first() != null && (url.endsWith("/p") || url.contains("/p?attempt="))) return true;
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

			if(name.length() > nameVariation.length()){
				name += " " + nameVariation;
			} else {
				name = nameVariation;
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

		Element image = doc.select("#botaoZoom").first();

		if (image != null) {
			String urlImage = image.attr("zoom");

			if(!urlImage.startsWith("http")){
				urlImage = image.attr("rel");
			}
			primaryImage = urlImage;
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
	 * Get the json array containing information about the skus in this page.
	 * 
	 * @return
	 */
	private JSONArray crawlSkuJsonArray(Document document) {
		JSONObject skuJson = crawlSKUJson(document);
		JSONArray skuJsonArray = null;

		if (skuJson != null) {
			try {
				skuJsonArray = skuJson.getJSONArray("skus");
			} catch(Exception e) {
				skuJsonArray = new JSONArray();
				Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
			}
		}

		return skuJsonArray;
	}

	/**
	 * Get the json containing all skus info.
	 * In case we can't find the json on the already loaded html from the
	 * sku page, we try to fetch this json from an API.
	 * 
	 * @param document
	 * @return
	 */
	private JSONObject crawlSKUJson(Document document) {
		JSONObject skuJson = null;
		Elements scriptTags = document.getElementsByTag("script");
		
		// first we will try to get the json object in the html
		for (Element tag : scriptTags){                
			for (DataNode node : tag.dataNodes()) {
				if(tag.html().trim().startsWith("var skuJson_0 = ")) {
					skuJson = new JSONObject
							( node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1] +
									node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1].split(Pattern.quote("}]};"))[0] );
				}
			}        
		}

		// if we couldn't find the json on the html, we will use the API
		if (skuJson == null) {
			Element elementId = document.select("#___rc-p-id").first();
			if (elementId != null) {
				String id = elementId.attr("value").trim();
				String apiURL = "http://www.lebes.com.br/api/catalog_system/pub/products/variations/" + id;
				skuJson = DataFetcher.fetchJSONObject(DataFetcher.GET_REQUEST, session, apiURL, null, null);
			}
		}
		
		return skuJson;
	}
}
