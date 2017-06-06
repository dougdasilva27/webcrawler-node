package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilMagazineluizaCrawlerUtils;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.Prices;
import models.Seller;
import models.Util;

/**
 * 
 * @author samirleao
 * @author gabriel (refactor) 06/06/17
 *
 */

public class BrasilMagazineluizaCrawler extends Crawler {

	private static final String HOME_PAGE = "http://www.magazineluiza.com.br/";
	private static final String SELLER_NAME = "magazine luiza";
	
	public BrasilMagazineluizaCrawler(Session session) {
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
		List<Product> products = new ArrayList<>();
		
		if(isProductPage(session.getOriginalURL(), doc)) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
			
			// InternalId for Single Product
			String internalIdSingleProduct = crawlInternalIdSingleProduct(doc);
			
			// InternalPid
			String internalPid = internalIdSingleProduct;
			
			// Product name
			String frontPageName = crawlNameFrontPage(doc);
			
			// Categories
			CategoryCollection categories = crawlCategories(doc);
			
			// Primary Image
			String primaryImage = crawlPrimaryImage(doc);
			
			// Secondary Images
			String secondaryImages = crawlSecondaryImages(doc, primaryImage);
			
			// Estoque
			Integer stock = null;
			
			// Sku info in json on html
			JSONObject skuJsonInfo = BrasilMagazineluizaCrawlerUtils.crawlFullSKUInfo(doc, "var digitalData = ");
			
			// Skus
			JSONArray skus = crawlSkusFromJson(skuJsonInfo);
			
			// Only one product in page
			boolean singleProduct = skus.length() == 1 && !BrasilMagazineluizaCrawlerUtils.hasVoltageSelector(skus);
			
			// Variations with same url and same price
			boolean voltageVariation = skus.length() > 0 && BrasilMagazineluizaCrawlerUtils.hasVoltageSelector(skus);
			
			// Products with different url , we crawl only the product with the original url
			boolean skusWithDiferentUrl = skus.length() > 1 && BrasilMagazineluizaCrawlerUtils.skusWithURL(doc);
			
			// Unnavailable Product
			boolean unnavailableProduct = skus.length() == 0;
			
			if(singleProduct) {
				JSONObject sku = skus.getJSONObject(0);
				
				// Full name
				String fullName = computeVariationName(sku, frontPageName);
				
				// Id for price
				String idForPrice = crawlIdForPrice(doc, true);
				
				// Marketplace
				Marketplace marketplace = crawlMarketPlace(doc, skuJsonInfo, idForPrice);
				
				// Availability
				boolean available = crawlAvailability(doc, marketplace);
				
				// Price
				Float price = crawlPrice(skuJsonInfo, available);
				
				// Prices
				Prices prices = crawlPrices(idForPrice, price);
				
				// Description
				String description = crawlDescription(doc);
				
				// Creating the product
				Product product = ProductBuilder.create()
						.setUrl(session.getOriginalURL())
						.setInternalId(internalIdSingleProduct)
						.setInternalPid(internalPid)
						.setName(fullName)
						.setPrice(price)
						.setPrices(prices)
						.setAvailable(available)
						.setCategory1(categories.getCategory(0))
						.setCategory2(categories.getCategory(1))
						.setCategory3(categories.getCategory(2))
						.setPrimaryImage(primaryImage)
						.setSecondaryImages(secondaryImages)
						.setDescription(description)
						.setStock(stock)
						.setMarketplace(marketplace)
						.build();

				products.add(product);
			}
			
			else if (voltageVariation) {
				for(int i = 0; i < skus.length(); i++) {
					JSONObject sku = skus.getJSONObject(i);
					
					// InternalId second part
					String internalIdSecondPart = computeInternalIdVariation(sku);
					
					// InternalId
					String internalId = internalIdSecondPart != null ? internalPid + "-" + internalIdSecondPart : internalIdSingleProduct;
					
					// Full name
					String fullName = computeVariationName(sku, frontPageName);
					
					// Marketplace
					Marketplace marketplace = crawlMarketPlace(doc, null, internalIdSecondPart);
					
					// Availability
					boolean available = crawlAvailabilityVoltageVariation(doc, marketplace, internalIdSecondPart);
					
					// Price
					Float price = crawlPrice(skuJsonInfo, available);
					
					// Prices
					Prices prices = crawlPrices(internalIdSecondPart, price);
					
					// Description
					String description = crawlDescriptionVoltageVariation(internalIdSecondPart);
					
					// Creating the product
					Product product = ProductBuilder.create()
							.setUrl(session.getOriginalURL())
							.setInternalId(internalId)
							.setInternalPid(internalPid)
							.setName(fullName)
							.setPrice(price)
							.setPrices(prices)
							.setAvailable(available)
							.setCategory1(categories.getCategory(0))
							.setCategory2(categories.getCategory(1))
							.setCategory3(categories.getCategory(2))
							.setPrimaryImage(primaryImage)
							.setSecondaryImages(secondaryImages)
							.setDescription(description)
							.setStock(stock)
							.setMarketplace(marketplace)
							.build();

					products.add(product);
				}
			}
			
			else if(skusWithDiferentUrl) {
				// Sku for this url
				String selectedValue = BrasilMagazineluizaCrawlerUtils.selectCurrentSKUValue(doc);
				
				// Sku information
				JSONObject sku = BrasilMagazineluizaCrawlerUtils.getSKUDetails(selectedValue, skuJsonInfo);
				
				// Full name
				String fullName = computeVariationName(sku, frontPageName);
				
				// Id for price
				String idForPrice = crawlIdForPrice(doc, false);
				
				// Marketplace
				Marketplace marketplace = crawlMarketPlace(doc, null, idForPrice);
				
				// Availability
				boolean available = crawlAvailabilitySkuWithDifferentUrl(skuJsonInfo);
				
				// Price
				Float price = crawlPrice(skuJsonInfo, available);
				
				// Prices
				Prices prices = crawlPrices(idForPrice, price);
				
				// Description
				String description = crawlDescription(doc);
				
				// Creating the product
				Product product = ProductBuilder.create()
						.setUrl(session.getOriginalURL())
						.setInternalId(internalIdSingleProduct)
						.setInternalPid(internalPid)
						.setName(fullName)
						.setPrice(price)
						.setPrices(prices)
						.setAvailable(available)
						.setCategory1(categories.getCategory(0))
						.setCategory2(categories.getCategory(1))
						.setCategory3(categories.getCategory(2))
						.setPrimaryImage(primaryImage)
						.setSecondaryImages(secondaryImages)
						.setDescription(description)
						.setStock(stock)
						.setMarketplace(marketplace)
						.build();

				products.add(product);
			}
			
			else if(unnavailableProduct) {
				skuJsonInfo = BrasilMagazineluizaCrawlerUtils.crawlFullSKUInfo(doc, "digitalData = ");
				
				// InternalId
				String internalId = crawlInternalIdUnnavailableProduct(skuJsonInfo);
				
				// InternalPid
				String internalPidUnnavailableProduct = internalId;
				
				// Description
				String description = crawlDescription(doc);
				
				// Categories
				CategoryCollection categoriesUnnavailableProduct = crawlCategoriesUnnavailableProduct(doc);
				
				// Creating the product
				Product product = ProductBuilder.create()
						.setUrl(session.getOriginalURL())
						.setInternalId(internalId)
						.setInternalPid(internalPidUnnavailableProduct)
						.setName(frontPageName)
						.setPrice(null)
						.setPrices(new Prices())
						.setAvailable(false)
						.setCategory1(categoriesUnnavailableProduct.getCategory(0))
						.setCategory2(categoriesUnnavailableProduct.getCategory(1))
						.setCategory3(categoriesUnnavailableProduct.getCategory(2))
						.setPrimaryImage(primaryImage)
						.setSecondaryImages(secondaryImages)
						.setDescription(description)
						.setStock(stock)
						.setMarketplace(new Marketplace())
						.build();

				products.add(product);
			}
			
		} else {
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
		}
		
		return products;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/
	private boolean isProductPage(String url, Document doc) {
		return (url.contains("/p/") || url.contains("/p1/")) &&  doc.select("h1[itemprop=name]").first() != null;
	}
	
	/**
	 * Crawl Internal ID for single product, for products with variation
	 * this id will be a first part of internalId
	 * @param doc
	 * @return
	 */
	private String crawlInternalIdSingleProduct(Document doc) {
		String internalId = null;
		Element elementInternalId = doc.select("#productId").first();
		
		if(elementInternalId != null){
			internalId = elementInternalId.val().trim();
		}
		
		return internalId;
	}
	
	/**
	 * Crawl internalId for unnavailable product
	 * @param skuInfo
	 * @return
	 */
	private String crawlInternalIdUnnavailableProduct(JSONObject skuInfo) {
		String id = null;
		
		if(skuInfo.has("idSku") && skuInfo.get("idSku") instanceof String) {
			id = skuInfo.getString("idSku");
		}
		
		return id;
	}
	
	/**
	 * Make the full internalId for variations
	 * @param sku
	 * @param internalIdFirstPart
	 * @return
	 */
	private String computeInternalIdVariation(JSONObject sku) {
		String id = null;
		
		if(sku.has("sku") && sku.get("sku") instanceof String) {
			id = sku.getString("sku");
		}
		
		return id;
	}
	
	/**
	 * Crawl name in front page
	 * @param doc
	 * @return
	 */
	private String crawlNameFrontPage(Document doc) {
		String name = null;
		Element elementName = doc.select("h1[itemprop=name]").first();
		
		if (elementName != null) {
			name = elementName.text();
		}
		
		return name;
	}
	
	/**
	 * Crawl Description
	 * @param doc
	 * @return
	 */
	private String crawlDescription(Document doc) {
		StringBuilder description = new StringBuilder();
		
		Element elementDescription = doc.select(".factsheet-main-container").first();
		Element anchorDescription = doc.select("#anchor-description").first();
		
		if (elementDescription != null) {
			description.append(elementDescription.html());
		}
		
		if (anchorDescription != null) {
			description.append(anchorDescription.html());
		}
		
		return description.toString();
	}
	
	/**
	 * Crawl description for voltage variations products
	 * Access url : http://www.magazineluiza.com.br/produto/ficha-tecnica/" + idSecondPart + "/
	 * @param idSecondPart
	 * @return
	 */
	private String crawlDescriptionVoltageVariation(String idSecondPart) {
		String descriptionURL = "http://www.magazineluiza.com.br/produto/ficha-tecnica/" + idSecondPart + "/";
		
		return DataFetcher.fetchString("GET", session, descriptionURL, null, cookies);
	}
	
	/**
	 * 
	 * @param doc
	 * @return
	 */
	private String crawlPrimaryImage(Document doc) {
		String primaryImage = null;

		Element image = doc.select(".big-picture-first-image a").first();

		if (image != null) {
			primaryImage = image.attr("href").trim();
		}
		
		if(primaryImage == null) {
			Elements imageThumbs = doc.select(".container-little-picture ul li a");
			
			for(Element e : imageThumbs) {
				if( !e.attr("rel").isEmpty() ) {
					primaryImage = parseImage(e.attr("rel"));
					break;
				}
			}
		}
		
		if(primaryImage == null) {
			Element primaryImageElement = doc.select(".img-product-out-of-stock img").first();
			
			if(primaryImageElement != null) {
				primaryImage = primaryImageElement.attr("src");
			}
		}
		
		if(primaryImage == null) {
			Element primaryImageElement = doc.select(".unavailable__product-img").first();
			
			if(primaryImageElement != null) {
				primaryImage = primaryImageElement.attr("src");
			}
		}
		
		return primaryImage;
	}

	/**
	 * 
	 * @param doc
	 * @return
	 */
	private String crawlSecondaryImages(Document doc, String primaryImage) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imageThumbs = doc.select(".container-little-picture ul li a");

		for (int i = 1; i < imageThumbs.size(); i++) { //starts with index 1, because the first image is the primary image
			Element e = imageThumbs.get(i);
			
			if( !e.attr("rel").isEmpty() ) {
				String image = parseImage(e.attr("rel"));
				
				if(!image.equalsIgnoreCase(primaryImage)) {
					secondaryImagesArray.put(image);
				}
			}
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}
	
	/**
	 * For secondary images, the url is found in a json in the rel attribute of the element
	 * @param text
	 * @return
	 */
	private String parseImage(String text) {
		int begin = text.indexOf("largeimage:") + 11;
		String img = text.substring(begin);
		img = img.replace("\'", " ").replace('}', ' ').trim();

		return img;
	}
	
	/**
	 * Crawl categories
	 * @param document
	 * @return
	 */
	private CategoryCollection crawlCategories(Document document) {
		CategoryCollection categories = new CategoryCollection();
		Elements elementCategories = document.select(".container-bread-crumb-detail.bread-none-line ul li[typeof=v:Breadcrumb] a");

		for (int i = 1; i < elementCategories.size(); i++) { // starting from index 1, because the first is the market name
			categories.add( elementCategories.get(i).text().trim() );
		}

		return categories;
	}
	
	/**
	 * Crawl categories
	 * @param document
	 * @return
	 */
	private CategoryCollection crawlCategoriesUnnavailableProduct(Document document) {
		CategoryCollection categories = new CategoryCollection();
		Elements elementCategories = document.select(".cbreadcrumb__title a");

		for (int i = 1; i < elementCategories.size(); i++) { // starting from index 1, because the first is the market name
			categories.add( elementCategories.get(i).text().trim() );
		}

		return categories;
	}
	
	/**
	 * Append variation name
	 * @param sku
	 * @return
	 */
	private String computeVariationName(JSONObject sku, String name) {
		if (sku.has("voltage") && sku.get("voltage") instanceof String) {
			return name + " - " + sku.getString("voltage");
		}
		
		else if (sku.has("color") && sku.get("color") instanceof String) {
			return name + " - " + sku.getString("color");
		}
		
		else if (sku.has("size") && sku.get("size") instanceof String) {
			return name + " - " + sku.getString("size").replace("&#34;", "");
		}
		
		return name; 
	}
	
	/**
	 * Crawl marketplace
	 * 
	 * When pass skuInfo = null, the method crawl price variation
	 * @param doc
	 * @param skuInfo
	 * @param idForPrice
	 * @return
	 */
	private Marketplace crawlMarketPlace(Document doc, JSONObject skuInfo, String idForPrice) {
		Marketplace marketplace = new Marketplace();
		Element marketplaceName = doc.select(".market-place-delivery .market-place-delivery__seller--big").first();
		
		if (marketplaceName != null) {
			String sellerName = marketplaceName.text().toLowerCase().trim();
			if (!sellerName.equals(SELLER_NAME) && (skuInfo == null || (skuInfo.has("salePrice") && skuInfo.get("salePrice") instanceof Double))) {
				Float sellerPrice;
				
				if(skuInfo != null) {
					sellerPrice = crawlPrice(skuInfo, true);
				} else {
					sellerPrice = crawlPriceVariation(doc, true);
				}
				
				JSONObject sellerJSON = new JSONObject();
				sellerJSON.put("name", sellerName);
				sellerJSON.put("price", sellerPrice);
				sellerJSON.put("prices", crawlPrices(idForPrice, sellerPrice).toJSON());
				
				try {
					Seller seller = new Seller(sellerJSON);
					marketplace.add(seller);
				} catch (Exception e) {
					Logging.printLogError(logger, session, Util.getStackTraceString(e));
				}
			}
		}
		
		return marketplace;
	}
	
	/**
	 * If singleProduct, element get the first buy option, else get the buy option checked
	 * @param doc
	 * @param singleProduct
	 * @return
	 */
	private String crawlIdForPrice(Document doc, boolean singleProduct) {
		String idForPrice = null;
		Element idForPriceElement;
		
		if(singleProduct) {
			idForPriceElement = doc.select(".buy-option").first();
		} else {
			idForPriceElement = doc.select(".buy-option[checked]").first();
		}
		
		if(idForPriceElement != null){
			idForPrice = idForPriceElement.attr("value");
		}
		
		return idForPrice;
	}
	
	/**
	 * Crawl price for voltage variation
	 * @param document
	 * @return
	 */
	private Float crawlPriceVariation(Document document, boolean available) {
		Float price = null;
		Element elementPrice = document.select("#productDiscountPrice").first();
		
		if(elementPrice != null && available) {
			price = Float.parseFloat(elementPrice.attr("value"));
		}

		return price;
	}
	
	/**
	 * Crawl price
	 * @param skuInfo
	 * @param available
	 * @return
	 */
	private Float crawlPrice(JSONObject skuInfo, boolean available) {
		Float price = null;
		
		if (available && skuInfo.has("salePrice") && skuInfo.get("salePrice") instanceof Double) {
			Double priceDouble = skuInfo.getDouble("salePrice");
			price = priceDouble.floatValue();
		}
		
		return price;
	}
	
	/**
	 * if has element in html and has no marketplace
	 * @param doc
	 * @param marketplace
	 * @return
	 */
	private boolean crawlAvailability(Document doc, Marketplace marketplace) {
		Element elementAvailable = doc.select(".container-btn-buy").first();
		
		if(elementAvailable != null && marketplace.isEmpty()) {
			return true;
		} 
		
		return false;
	}
	
	/**
	 * 
	 * @param skuInfo
	 * @return
	 */
	private boolean crawlAvailabilitySkuWithDifferentUrl(JSONObject skuInfo) {
		if(skuInfo.has("stockAvailability") && skuInfo.get("stockAvailability") instanceof Boolean) {
			return skuInfo.getBoolean("stockAvailability");
		}
		
		return false;
	}
	
	/**
	 * 
	 * @param doc
	 * @param marketplace
	 * @return
	 */
	private boolean crawlAvailabilityVoltageVariation(Document doc, Marketplace marketplace, String idSecondPart) {
		if(BrasilMagazineluizaCrawlerUtils.hasOptionSelector(idSecondPart, doc) && marketplace.isEmpty()) {
			return true;
		} 
		
		return false;
	}
	
	/**
	 * Crawl prices in api "http://www.magazineluiza.com.br/produto/"+ internalId +"/preco.json"
	 * @param internalId
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(String internalId, Float price){
		Prices prices = new Prices();
		
		if(price != null){
			String urlPrices = "http://www.magazineluiza.com.br/produto/"+ internalId +"/preco.json";
			
			Map<Integer, Float> installmentsPriceMap = new HashMap<>();
			
			JSONObject jsonPrices = new JSONObject();
			
			try{
				String json = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, urlPrices, null, cookies);
				jsonPrices = new JSONObject(json);
			} catch(Exception e){
				Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
			}
			
			if(jsonPrices.has("payload")){
				JSONObject payload = jsonPrices.getJSONObject("payload");
				
				if(payload.has("data")){
					JSONObject data = payload.getJSONObject("data");
					
					if(data.has("product")){
						JSONObject product = data.getJSONObject("product");
						
						if(product.has("bestPaymentOption")){
							JSONArray installments = product.getJSONArray("bestPaymentOption");
							
							for(int i = 0; i < installments.length(); i++){
								JSONObject installment = installments.getJSONObject(i);
								
								if(installment.has("number")){
									Integer parcela = installment.getInt("number");
									
									if(installment.has("value")){
										Float value = Float.parseFloat(installment.getString("value"));
										
										installmentsPriceMap.put(parcela, value);
									}
								}
							}
						}
					}
				}
			}
			
			//preço uma vez no cartão é igual ao do boleto.
			if(installmentsPriceMap.containsKey(1)){
				prices.setBankTicketPrice(installmentsPriceMap.get(1));
			}
			
			prices.insertCardInstallment(Card.VISA.toString(), installmentsPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentsPriceMap);
			prices.insertCardInstallment(Card.DINERS.toString(), installmentsPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentsPriceMap);
			prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentsPriceMap);
			prices.insertCardInstallment(Card.ELO.toString(), installmentsPriceMap);
			prices.insertCardInstallment(Card.AURA.toString(), installmentsPriceMap);
		}
		
		return prices;
	}
	
	/**
	 * 
	 * @param skuInfo
	 * @return
	 */
	private JSONArray crawlSkusFromJson(JSONObject skuInfo) {
		JSONArray skus = new JSONArray();
		
		if(skuInfo.has("details")) {
			skus = skuInfo.getJSONArray("details");
		}
		
		return skus;
	}
}
