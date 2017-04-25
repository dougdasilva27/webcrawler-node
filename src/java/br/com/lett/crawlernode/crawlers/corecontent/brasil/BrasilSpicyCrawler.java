package br.com.lett.crawlernode.crawlers.corecontent.brasil;


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

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;

/************************************************************************************************************************************************************************************
 * Crawling notes (26/08/2016):
 * 
 * 1) For this crawler, we have one URL for single sku, but if exists one url with mutiple skus this crawler is prepared.
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
 * 8) The primary image is the first image on the secondary images.
 * 
 * 9) The url of the primary images are changed to bigger dimensions manually.
 * 
 * Examples:
 * ex1 (available): http://www.spicy.com.br/ar-condicionado-portatil-10-000-btus-branco-nano-silent-olimpia-splendid--127v/p
 * ex2 (unavailable): http://www.spicy.com.br/ar-condicionado-split-hi-wall-lg-libero-art-cool-inverter-v-18-000-btu-s-frio-220v/p
 *
 *
 ************************************************************************************************************************************************************************************/

public class BrasilSpicyCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.spicy.com.br/";

	public BrasilSpicyCrawler(Session session) {
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

		if ( isProductPage(session.getOriginalURL()) ) {
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

			// Marketplace map
			Map<String, Float> marketplaceMap = crawlMarketplace(doc);

			// Marketplace
			JSONArray marketplace = assembleMarketplaceFromMap(marketplaceMap);
			
			// Primary image
			String primaryImage = crawlPrimaryImage(doc);
			
			// Secondary images
			String secondaryImages = crawlSecondaryImages(doc);
			
			// sku data in json
			JSONArray arraySkus = crawlSkuJsonArray(doc);			
			
			for(int i = 0; i < arraySkus.length(); i++){
				JSONObject jsonSku = arraySkus.getJSONObject(i);
				
				// Availability
				boolean available = crawlAvailability(jsonSku);
					
				// InternalId 
				String internalId = crawlInternalId(jsonSku);
				
				// Json Produc
				JSONObject jsonProduct = crawlJsonProduct(internalId);
				
				// Price
				Float price = crawlMainPagePrice(jsonSku, available);
				
				// Name
				String name = crawlName(doc, jsonSku);
				
				// Prices
				Prices prices = crawlPrices(jsonProduct, price);
				
				// Stock
				Integer stock = crawlStock(jsonProduct);
				
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

	private boolean isProductPage(String url) {
		if ( url.endsWith("/p") ) {
			return true;
		}
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
			
			if(nameVariation.length() > name.length()){
				name = nameVariation;
			} else if(!name.contains(nameVariation)){
				name = name + " " + nameVariation;
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
		if(json.has("available")) {
			return json.getBoolean("available");
		}
		
		return false;
	}

	private Map<String, Float> crawlMarketplace(Document document) {
		return new HashMap<>();
	}
	
	private JSONArray assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		return new JSONArray();
	}

	private String crawlPrimaryImage(Document doc) {
		String primaryImage = null;
		Element image = doc.select("#image a").first();
		
		if (image != null) {
			primaryImage = image.attr("href");
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document doc) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();
		Elements images = doc.select(".thumbs li a img");
		
			
		for (int i = 1; i < images.size(); i++) {//starts with index 1, because the first image is the primary image
			
			String urlImage = modifyImageURL(images.get(i).attr("src"));
			secondaryImagesArray.put(urlImage);	

		}

		
		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}
		
		return secondaryImages;
	}
	
	
	private String modifyImageURL(String url) {
		String[] tokens = url.trim().split("/");
		String dimensionImage = tokens[tokens.length-2]; //to get dimension image and the image id
		
		String[] tokens2 = dimensionImage.split("-"); //to get the image-id
		String dimensionImageFinal = tokens2[0] + "-1000-1000";
		
		String urlReturn = url.replace(dimensionImage, dimensionImageFinal); //The image size is changed
		
		return urlReturn;	
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<>();
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
		Element descriptionElement = document.select(".box_descricao").first();

		if (descriptionElement != null) {
			description = description + descriptionElement.html();
		}
	
		return description;
	}
	
	private JSONObject crawlJsonProduct(String internalId){
		JSONObject json = new JSONObject();
		
		String url = "http://www.spicy.com.br/produto/sku/" + internalId;
		JSONArray jsonProduct = DataFetcher.fetchJSONArray(DataFetcher.GET_REQUEST, session, url, null, cookies);
		
		if(jsonProduct.length() > 0){
			json = jsonProduct.getJSONObject(0);
		}
		
		return json;
	}
	
	private Integer crawlStock(JSONObject jsonProduct) {
		Integer stock = null;
		
		if (jsonProduct.has("SkuSellersInformation")) {
			JSONArray sellers = jsonProduct.getJSONArray("SkuSellersInformation");
			
			for (int i = 0; i < sellers.length(); i++) {
				JSONObject seller = sellers.getJSONObject(i);
				
				if (seller.has("Name")) {
					String name = seller.getString("Name").toLowerCase().trim();
					
					if (name.equals("spicy")) {
						if (seller.has("AvailableQuantity")) {
							stock = seller.getInt("AvailableQuantity");
						}
						break;
					}
				}
			}
		}
		
		return stock;
	}
	
	private Prices crawlPrices(JSONObject jsonProduct, Float price){
		Prices prices = new Prices();
		
		if(price != null){
			// Preço vitrine é uma vez no cartão
			Map<Integer,Float> installmentPriceMap = new HashMap<>();
			installmentPriceMap.put(1, price);
			
			if (jsonProduct.has("BestInstallmentNumber")) {
				Integer installment = jsonProduct.getInt("BestInstallmentNumber");
				
				if (jsonProduct.has("BestInstallmentValue")) {
					Double valueDouble = jsonProduct.getDouble("BestInstallmentValue");
					Float value = valueDouble.floatValue();
					
					if (installment > 0 && value > 0F) {
						installmentPriceMap.put(installment, value);
					}
				}
			}
			
			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
		}
		
		return prices;
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
							(
							node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1] +
							node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1].split(Pattern.quote("}]};"))[0]
							);

				}
			}        
		}
		
		if (skuJson != null) {
			skuJsonArray = skuJson.getJSONArray("skus");
		} else {
			skuJsonArray = new JSONArray();
		}
		
		return skuJsonArray;
	}
}
