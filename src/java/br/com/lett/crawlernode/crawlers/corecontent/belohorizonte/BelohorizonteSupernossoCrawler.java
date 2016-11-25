package br.com.lett.crawlernode.crawlers.corecontent.belohorizonte;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;

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
			
			//Product product = crawlUsingEndpoint();
			Product product = crawlUsingWebDriver(doc);
			
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
		String description = crawlDescription(document);
		
		product.setUrl(session.getOriginalURL());
		product.setInternalId(internalId);
		product.setInternalPid(internalPid);
		product.setName(name);
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
			
			
			product.setUrl(this.session.getOriginalURL());
			product.setInternalId(internalId);
			product.setName(name);
			product.setPrice(price);
			product.setCategory1(categories.getCategory(0));
			product.setCategory2(categories.getCategory(1));
			product.setCategory3(categories.getCategory(2));
			product.setPrimaryImage(primaryImage);
			product.setSecondaryImages(secondaryImages);
			product.setDescription(description);
			product.setStock(stock);
//			product.setMarketplace(marketplace);
			product.setAvailable(available);
			
			
			
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
	
	private String crawlDescription(Document document) {
		StringBuilder description = new StringBuilder();
		
		Element productInfoElement = document.select("section.snc-product-info").first();
		if (productInfoElement != null) {
			description.append(productInfoElement.html());
		}
		
		return description.toString();
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