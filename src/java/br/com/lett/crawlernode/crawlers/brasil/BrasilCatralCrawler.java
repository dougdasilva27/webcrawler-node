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

import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.Crawler;
import br.com.lett.crawlernode.kernel.task.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;

/************************************************************************************************************************************************************************************
 * Crawling notes (02/08/2016):
 * 
 * 1) For this crawler, we have one url per mutiple skus. 
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking for url.
 * 
 * 5) Is used a script in html to get name, price, availability and internalIDS. 
 * 
 * 6) Even if a product is unavailable, its price is not displayed. Only the price for payment via 'boleto' is displayed.
 * 
 * 7) When price is not displayed, in script the price is 9999876.0, then is not crawled.
 * 
 * 8) There is internalPid for skus in this ecommerce. The internalPid is a number that is the same for all
 * the variations of a given sku.
 * 
 * 9) The primary image is the first image in the secondary images selector.
 * 
 * 10) Is not crawled price "a vista" because on the purchase does not have this option.
 * 
 * Examples:
 * ex1 (available): http://www.catral.com.br/bebedouro-de-pressso-inox-ibbl-bag40/p
 * ex2 (unavailable): http://www.catral.com.br/bobina-termica-para-ecf-80mmx30m-1-via-cor-palha-caixa-30-unid-maxprint/p
 * ex3 (unavailable/available): http://www.catral.com.br/batedeira-planetaria-g-paniz-12-litros-bp12rp-monofasica-/p
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilCatralCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.catral.com.br/";

	public BrasilCatralCrawler(CrawlerSession session) {
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

			/* ***********************************
			 * crawling data of only one product *
			 *************************************/

			// Pid
			String internalPid = crawlInternalPid(doc);

			// Categories
			ArrayList<String> categories = crawlCategories(doc);
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);

			// Primary image
			String primaryImage = crawlPrimaryImage(doc);

			// Secondary images
			String secondaryImages = crawlSecondaryImages(doc);

			// Description
			String description = crawlDescription(doc);

			// Stock
			Integer stock = null;

			// Marketplace map
			Map<String, Float> marketplaceMap = crawlMarketplace(doc);

			// Marketplace
			JSONArray marketplace = assembleMarketplaceFromMap(marketplaceMap);
			
			// Skus
			JSONArray arraySkus = this.crawlSkuJsonArray(doc);

			for(int i = 0; i < arraySkus.length(); i++){
				
				JSONObject jsonSku = arraySkus.getJSONObject(i);
				
				// InternalId
				String internalId = crawlInternalId(jsonSku);
				
				// Name
				String name = crawlName(jsonSku);
				
				// Availability
				boolean available = crawlAvailability(jsonSku);
				
				// Price
				Float price = crawlPrice(jsonSku, available);
				
				// Creating the product
				Product product = new Product();
				product.setSeedId(this.session.getSeedId());
				product.setUrl(this.session.getUrl());
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
			Logging.printLogDebug(logger, "Not a product page" + this.session.getUrl());
		}
		
		return products;
	}



	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		if ( url.endsWith("/p")  || url.contains("/p?attempt=")) return true;
		return false;
	}
	
	
	/*******************
	 * General methods *
	 *******************/
	
	private String crawlInternalId(JSONObject jsonSku) {
		String internalId = null;
	
		if(jsonSku.has("sku")) internalId = Integer.toString(jsonSku.getInt("sku"));

		return internalId;
	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;
		Element inElement = document.select("#___rc-p-id").first();
		
		if(inElement != null) internalPid = inElement.attr("value");
		
		return internalPid;
	}
	
	private String crawlName(JSONObject jsonSku) {
		String name = null;
		
		if(jsonSku.has("skuname")) name = jsonSku.getString("skuname");

		return name;
	}

	private Float crawlPrice(JSONObject jsonSku, boolean available) {
		Float price = null;
				
		if (jsonSku.has("bestPriceFormated") && available) {
			price = Float.parseFloat( jsonSku.getString("bestPriceFormated").replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		} 

		return price;
	}
	
	private boolean crawlAvailability(JSONObject jsonSku) {
		
		if(jsonSku.has("available")) return jsonSku.getBoolean("available");
		
		return true;
	}

	private Map<String, Float> crawlMarketplace(Document document) {
		return new HashMap<String, Float>();
	}
	
	private JSONArray assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		return new JSONArray();
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select("#image a").first();

		if (primaryImageElement != null) {
			primaryImage = primaryImageElement.attr("href").trim();
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select("#botaoZoom");

		for (int i = 1; i < imagesElement.size(); i++) { // starts with index 1 because the first item is the primary image
			Element e = imagesElement.get(i);
			if(e.hasAttr("zoom") && !e.attr("zoom").isEmpty()){
				secondaryImagesArray.put( e.attr("zoom").trim() );
			} else {
				secondaryImagesArray.put( e.attr("rel").trim() );
			}
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(".bread-crumb ul li");

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
		Element descriptionElement = document.select(".productDescription").first();

		if (descriptionElement != null) description = description + descriptionElement.html();

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
							(
							node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1] +
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
