package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.CrawlerSession;
import br.com.lett.crawlernode.kernel.fetcher.DataFetcher;
import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.Crawler;
import br.com.lett.crawlernode.util.Logging;


/************************************************************************************************************************************************************************************
 * Crawling notes (06/07/2016):
 * 
 * 1) For this crawler, we have one url per each sku. At the time this crawler was done, no example of multiple skus
 * in an URL was found. Although there was some indication that this case could occur (eg. analysing the sku json), no
 * concrete example was found. The crawler always tries to get all the data only from the sku URL, without using any endpoint. 
 * We choose to use endpoint, only if the information isn't available anywhere else.
 * 
 * 2) The images are crawled from the sku json, fetched from an endpoint of shopfato.
 * 
 * 3) There is stock information for skus in this ecommerce only in the json from the endpoint.
 * 
 * 4) There is no marketplace in this ecommerce by the time this crawler was made. There are some indications that could exist
 * some other seller than the shopfato, but no concrete example was found. Still we try to get the seller name in the sku page, that
 * is 'shopfato' on all observed cases.  
 * 
 * 6) The sku page identification is done simply looking the URL format.
 * 
 * 7) When a product is unavailable, its price is not shown. But the crawler doesn't consider this
 * as a global rule. It tries to crawl the price the same way in both cases.
 * 
 * 8) There is no internalPid for skus in this ecommerce. The internalPid must be a number that is the same for all
 * the variations of a given sku.
 * 
 * 9) In the json from the endpoint we have the stock from the seller. There is a field with sellers informations, but
 * we didn't saw any example with more than one seller (different from shopfato), for an sku.
 * 
 * 10) We have one method for each type of information for a sku (please carry on with this pattern).
 * 
 * Examples:
 * ...
 * 
 * Optimizations notes:
 * ...
 *
 ************************************************************************************************************************************************************************************/

public class BrasilShopfatoCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.shopfato.com.br/";
	private final String MAIN_SELLER_NAME_LOWER_CASE = "shopfato";

	public BrasilShopfatoCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getUrl()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());		

			/* ***************
			 * crawling sku  *
			 *****************/

			// InternalId
			String internalId = crawlInternalId(doc);

			// Pid
			String internalPid = crawlInternalPid(doc);

			// Name
			String name = crawlName(doc);

			// Availability
			boolean available = crawlAvailability(doc);

			// Price
			Float price = crawlMainPagePrice(doc);

			// Categories
			ArrayList<String> categories = crawlCategories(doc);
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);

			// Get sku information json from the endpoint
			JSONArray skuJson = crawlSkuJsonFromAPI(doc);

			// Primary image
			String primaryImage = crawlPrimaryImage(skuJson);

			// Secondary images
			String secondaryImages = crawlSecondaryImages(skuJson);

			// Marketplace
			Map<String, Float> marketplaceMap = crawlMarketplace(doc);
			JSONArray marketplace = assembleMarketplaceFromMap(marketplaceMap);

			// Description
			String description = crawlDescription(doc);

			// Stock
			Integer stock = crawlStock(skuJson);
			
			// Checking sellers informations to make sure there isn't some seller other than shopfato for this sku
			checkSkuSellersInfo(skuJson);

			// Creating the product
			Product product = new Product();
			product.setSeedId(this.session.getSeedId());
			product.setUrl(this.session.getUrl());
			product.setInternalId(internalId);
			product.setInternalPid(internalPid);
			product.setName(name);
			product.setAvailable(available);
			product.setPrice(price);
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
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}
		
		return products;
	}


	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		if (url.startsWith(HOME_PAGE) && url.endsWith("/p")) return true;
		return false;
	}


	/*******************************
	 * Single product page methods *
	 *******************************/

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select(".prd-name .fn.productName").first();

		if (nameElement != null) {
			name = nameElement.text().toString().trim();
		}

		return name;
	}

	private boolean hasNotifyMe(Document document) {
		Element notifymeElement = document.select(".notifyme.sku-notifyme").first();

		if (notifymeElement != null) return true;
		return false;
	}

	private boolean crawlAvailability(Document document) {
		if ( hasNotifyMe(document) ) return false;
		return true;
	}

	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element mainPagePriceElement = document.select(".prd-price-holder .plugin-preco .valor-por.price-best-price .skuBestPrice").first();
		if (mainPagePriceElement == null) {
			mainPagePriceElement = document.select(".prd-price-holder .plugin-preco .skuBestPrice").first();
		}

		if (mainPagePriceElement != null) {
			price = Float.parseFloat( mainPagePriceElement.text().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		}

		return price;
	}

	private Map<String, Float> crawlMarketplace(Document document) {
		Map<String, Float> marketplace = new HashMap<String, Float>();
		Element sellerElement = document.select(".prd-small-info-item.prd-small-info-provider .seller-description .seller-name a").first();

		if (sellerElement != null) {
			String sellerName = sellerElement.text().toString().trim().toLowerCase();
			Float sellerPrice = this.crawlMainPagePrice(document);

			marketplace.put(sellerName, sellerPrice);
		}


		return marketplace;
	}


	/*******************
	 * General methods *
	 *******************/

	private String crawlInternalPid(Document document) {
		String internalPid = null;
		return internalPid;
	}

	private String crawlInternalId(Document document) {
		String internalId = null;
		Element internalIdElement = document.select(".skuReference").first();

		if (internalIdElement != null) {
			internalId = internalIdElement.text().trim();
		}

		return internalId;
	}

	private String crawlDescription(Document document) {
		String description = "";
		Element skuDescriptionElement = document.select(".prd-accordion-description-holder").first();
		Element skuSpecElement = document.select(".prd-accordion-especification-holder").first();

		if (skuDescriptionElement != null) description = description + skuDescriptionElement.html();
		if (skuSpecElement != null) description = description + skuSpecElement.html();
		
		return description;
	}

	private JSONArray assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		JSONArray marketplace = new JSONArray();

		for(String sellerName : marketplaceMap.keySet()) {
			if ( !sellerName.equals(MAIN_SELLER_NAME_LOWER_CASE) ) {
				JSONObject seller = new JSONObject();
				seller.put("name", sellerName);
				seller.put("price", marketplaceMap.get(sellerName));

				marketplace.put(seller);
			}
		}

		return marketplace;
	}

	private String crawlPrimaryImage(JSONArray skuJson) {
		String primaryImage = null;

		JSONObject sku = skuJson.getJSONObject(0);
		if (sku.has("Images")) {
			JSONArray images = sku.getJSONArray("Images");
			for (int i = 0; i < images.length(); i++) {
				JSONArray innerImagesArray = images.getJSONArray(i);
				if ( this.isArrayOfMainImages(innerImagesArray) ) {
					primaryImage = this.getLargestImage(innerImagesArray);
				}
			}
		}

		return primaryImage;
	}
	
	private String crawlSecondaryImages(JSONArray skuJson) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();
		
		JSONObject sku = skuJson.getJSONObject(0);
		if (sku.has("Images")) {
			JSONArray images = sku.getJSONArray("Images");
			for (int i = 0; i < images.length(); i++) {
				JSONArray innerImagesArray = images.getJSONArray(i);
				if ( !this.isArrayOfMainImages(innerImagesArray) ) {
					secondaryImagesArray.put( this.getLargestImage(innerImagesArray) );
				}
			}
		}
		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private boolean isArrayOfMainImages(JSONArray innerImagesArray) { // Identify the inner array of images that are the primary image
		JSONObject innerImage = (JSONObject) innerImagesArray.get(0);
		if (innerImage.has("IsMain")) {
			if (innerImage.getBoolean("IsMain")) return true;
		}
		return false;
	}
	
	private String getLargestImage(JSONArray innerImagesArray) {
		for (int i = 0; i < innerImagesArray.length(); i++) {
			JSONObject innerImage = (JSONObject) innerImagesArray.get(i);
			if (innerImage.has("Path")) {
				String imagePath = innerImage.getString("Path");
				if (imagePath.contains("-1000-1000/")) return imagePath;
			}
		}
		
		return null;
	}

	private JSONArray crawlSkuJsonFromAPI(Document document) {
		String internalId = this.crawlInternalId(document);
		JSONArray skuData = null;
		if (internalId != null) {
			String apiURL = HOME_PAGE + "produto/sku/" + internalId;
			skuData = DataFetcher.fetchJSONArray(DataFetcher.GET_REQUEST, session, apiURL, null, null);
		}

		return skuData;
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
	
	private void checkSkuSellersInfo(JSONArray skuJson) { // some day, if a new seller appears, this will fire a warn
		JSONObject sku = skuJson.getJSONObject(0);
		if ( sku.has("SkuSellersInformation") ) {
			JSONArray sellersInformation = sku.getJSONArray("SkuSellersInformation");
			if ( sellersInformation.length() > 1 ) Logging.printLogWarn(logger, session, "Warning: there is more than one seller in this sku!");
		}
	}
	
	private Integer crawlStock(JSONArray skuJson) {
		JSONObject sku = skuJson.getJSONObject(0);
		if ( sku.has("SkuSellersInformation") ) {
			JSONArray sellersInformation = sku.getJSONArray("SkuSellersInformation");
			for (int i = 0; i < sellersInformation.length(); i++) {
				JSONObject seller = sellersInformation.getJSONObject(i);
				if ( this.isMainSeller(seller) ) {
					if ( seller.has("AvailableQuantity") ) return seller.getInt("AvailableQuantity");
				}
			}
			if ( sellersInformation.length() > 1 ) Logging.printLogWarn(logger, session, "Warning: there is more than one seller in this sku!");
		}
		
		return null;
	}
	
	private boolean isMainSeller(JSONObject seller) {
		if (seller.has("IsDefaultSeller")) {
			if ( seller.getBoolean("IsDefaultSeller") ) return true;
		}
		return false;
	}

}
