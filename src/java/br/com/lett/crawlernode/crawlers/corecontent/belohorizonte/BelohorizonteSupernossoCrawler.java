package br.com.lett.crawlernode.crawlers.corecontent.belohorizonte;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import models.Prices;

public class BelohorizonteSupernossoCrawler extends Crawler {
	
	public BelohorizonteSupernossoCrawler(Session session) {
		super(session);
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
			
			Product product = crawlUsingEndpoint();
			//Product product = crawlUsingWebDriver(doc);
			
			products.add(product);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
		}
		
		return products;
	}
	
	private Product crawlUsingWebDriver(Document document) {
		Product product = new Product();
		
		String internalId = crawlInternalId(document);
		String internalPid = null;
		String name = crawlName(document);
		boolean available = crawlAvailability(document);
		Float price = crawlPrice(document);
		Prices prices = crawlPrices(document);
		String primaryImage = crawlPrimaryImage(document);
		String secondaryImages = crawlSecondaryImages(document);
		CategoryCollection categories = crawlCategories(document);
		String description = crawlDescription(document);
		Integer stock = null;
		JSONArray marketplace = crawlMarketplace(document);
		
		product.setUrl(session.getOriginalURL());
		product.setInternalId(internalId);
		product.setInternalPid(internalPid);
		product.setName(name);
		product.setAvailable(available);
		product.setPrice(price);
		product.setPrices(prices);
		product.setCategory1(categories.getCategory(0));
		product.setCategory2(categories.getCategory(1));
		product.setCategory3(categories.getCategory(2));
		product.setPrimaryImage(primaryImage);
		product.setSecondaryImages(secondaryImages);
		product.setMarketplace(marketplace);
		product.setStock(stock);
		product.setDescription(description);
		
		return product;
	}

	private boolean isProductPage(String url) {
		return url.startsWith("https://www.supernossoemcasa.com.br/e-commerce/p/");
	}
	
	private Product crawlUsingEndpoint() {
		Product product = new Product();
		
		// get the sku id from the URL
		String[] tokens = session.getOriginalURL().split("\\/");
		String skuId = null;
		if (tokens.length >= 5) skuId = tokens[5];
		
		if (skuId != null) {
			
			// endpoint request
			String requestURL = "https://www.supernossoemcasa.com.br/e-commerce/api/products/" + skuId;
						
			JSONObject endpointResponse = DataFetcher.fetchJSONObject(DataFetcher.GET_REQUEST, session, requestURL, null, null);
						
			String internalId = null;
			if (endpointResponse.has("sku")) {
				internalId = endpointResponse.getString("sku");
			}
			
			String internalPid = null;
			
			String name = null;
			if (endpointResponse.has("name")) {
				name = endpointResponse.getString("name");
			}
			
			boolean available = false;
			if (endpointResponse.has("stockQuantity")) {
				if (endpointResponse.getInt("stockQuantity") > 0) available = true;
			}
			
			Float price = null;
			if (endpointResponse.has("price")) {
				price = new Float(endpointResponse.getDouble("price"));
			}
			
			Prices prices = crawlPricesUsingAPI(price);
			
			CategoryCollection categories = crawlCategoriesUsingAPI(endpointResponse);
			
			String primaryImage = null;
			if (endpointResponse.has("mainImageUrl")) {
				primaryImage = endpointResponse.getString("mainImageUrl");
			}
			
			String secondaryImages = null;
			if (endpointResponse.has("additionalImagesUrl")) {
				JSONArray secondaryImagesArray = endpointResponse.getJSONArray("additionalImagesUrl");
				if (secondaryImagesArray.length() > 0) {
					secondaryImages = secondaryImagesArray.toString();
				} 
			}
			
			String description = "";
			
			Integer stock = null;
			if (endpointResponse.has("stockQuantity")) {
				stock = endpointResponse.getInt("stockQuantity");
			}
			
			JSONArray marketplace = new JSONArray();
			
			product.setUrl(this.session.getOriginalURL());
			product.setInternalId(internalId);
			product.setInternalPid(internalPid);
			product.setName(name);
			product.setAvailable(available);
			product.setPrice(price);
			product.setPrices(prices);
			product.setCategory1(categories.getCategory(0));
			product.setCategory2(categories.getCategory(1));
			product.setCategory3(categories.getCategory(2));
			product.setPrimaryImage(primaryImage);
			product.setSecondaryImages(secondaryImages);
			product.setDescription(description);
			product.setStock(stock);
			product.setMarketplace(marketplace);
			
			
		} else {
			Logging.printLogDebug(logger, session, "Error parsing sku id from URL.");
		}
		
		return product;
	}
	
	private String crawlInternalId(Document document) {
		String internalId = null;
		Element internalIdElement = document.select("div.snc-product-code span[itemprop=sku]").first();
		if (internalIdElement != null) {
			internalId = internalIdElement.text().trim();
		}
		return internalId;
	}
	
	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("div.snc-product-info h1.snc-product-name").first();
		if (nameElement != null) {
			name = nameElement.text().trim();
		}
		return name;
	}
	
	private boolean crawlAvailability(Document document) {
		boolean available = true;
		Element buyButtonElement = document.select("div.snc-product-actions-btn.ng-scope").first();
		if (buyButtonElement == null) {
			available = false;
		}
		return available;
	}
	
	private Float crawlPrice(Document document) {
		Float price = null;
		
		Element priceElement = document.select("meta[itemprop=price]").first();
		if (priceElement != null) {
			price = Float.parseFloat(priceElement.attr("content").trim());
		}
		
		return price;
	}
	
	/**
	 * There is no bankSlip price.
	 * 
	 * For installments, we will have only one installment for each
	 * card brand, and it will be equals to the price crawled on the sku
	 * main page.
	 * 
	 * @param doc
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(Document document) {
		Prices prices = new Prices();
		
		Float price = crawlPrice(document);
		
		if(price != null){
			Map<Integer,Float> installmentPriceMap = new TreeMap<Integer, Float>();
			installmentPriceMap.put(1, price);
	
			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
		}
		
		return prices;
	}
	
	private Prices crawlPricesUsingAPI(Float price) {
		Prices prices = new Prices();
		
		if(price != null){
			Map<Integer,Float> installmentPriceMap = new TreeMap<Integer, Float>();
			installmentPriceMap.put(1, price);
	
			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
		}
		
		return prices;
	}
	
	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select("div.snc-product-image.zoom-img-block img").first();
		if (primaryImageElement != null) {
			primaryImage = primaryImageElement.attr("data-zoom-image").trim();
		}
		return primaryImage;
	}
	
	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select("#zoom-gallery li a");

		for (int i = 1; i < imagesElement.size(); i++) { // the first is the primary image
			String image = imagesElement.get(i).attr("data-zoom-image").trim();
			secondaryImagesArray.put( image );
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}
	
	private CategoryCollection crawlCategories(Document document) {
		CategoryCollection categories = new CategoryCollection();

		Elements elementCategories = document.select("ol.snc-breadcrumb.breadcrumb li a span");
		for (int i = 0; i < elementCategories.size(); i++) { 
			categories.add( elementCategories.get(i).text().trim() );
		}

		return categories;
	}
	
	private String crawlDescription(Document document) {
		StringBuilder description = new StringBuilder();
		
		Element productInfoElement = document.select("section.snc-product-info").first();
		if (productInfoElement != null) {
			description.append(productInfoElement.html());
		}
		
		return description.toString();
	}
	
	private JSONArray crawlMarketplace(Document document) {
		return new JSONArray();
	}
	
	
	/**
	 * The main category can be found inside the endpointResponse.
	 * 
	 * To get subcategories first we must request for the mainCategory information, using
	 * an endpoint with the following format:
	 * "https://www.supernossoemcasa.com.br/e-commerce/api/category/" + categoryId
	 * 
	 * With the response of the above request, we get the parentId of this category, and
	 * perform another request for the parentCategory, so we can get it's name.
	 * 
	 * @param endpointResponse
	 * @return
	 */
	private CategoryCollection crawlCategoriesUsingAPI(JSONObject endpointResponse) {
		CategoryCollection categories = new CategoryCollection();

		String mainCategoryId = endpointResponse.getString("mainCategoryId");
		String mainCategoryName = endpointResponse.getString("mainCategoryName");
		
		if (endpointResponse.has("categories")) {
			JSONArray categoriesIds = endpointResponse.getJSONArray("categories");
						
			for (int i = 0; i < categoriesIds.length(); i++) {
				String categoryId = categoriesIds.getString(i);
				
				if (mainCategoryId.equals(categoryId)) {
					String categoryRequestURL = "https://www.supernossoemcasa.com.br/e-commerce/api/category/" + categoryId;
										
					JSONObject categoryRequestResponse = DataFetcher.fetchJSONObject(DataFetcher.GET_REQUEST, session, categoryRequestURL, null, null);
					
					if (categoryRequestResponse.has("parentId")) {
						String parentId = categoryRequestResponse.getString("parentId");
						String parentCategoryRequestURL = "https://www.supernossoemcasa.com.br/e-commerce/api/category/" + parentId;
												
						JSONObject parentCategoryRequestResponse = DataFetcher.fetchJSONObject(DataFetcher.GET_REQUEST, session, parentCategoryRequestURL, null, null);
												
						if (parentCategoryRequestResponse.has("name")) {
							categories.add(parentCategoryRequestResponse.getString("name"));
						}
					}
				}
			}
		}
		
		categories.add(mainCategoryName);

		return categories;
	}
}