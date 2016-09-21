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

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.core.task.Crawler;
import br.com.lett.crawlernode.util.Logging;

/************************************************************************************************************************************************************************************
 * Crawling notes (17/08/2016):
 * 
 * 1) For this crawler, we have one url per each sku. There is no page is more than one sku in it.
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking for an specific html element.
 * 
 * 5) Even if a product is unavailable, its price is not displayed.
 * 
 * 6) There is no internalPid for skus in this ecommerce. The internalPid must be a number that is the same for all
 * the variations of a given sku.
 * 
 * 7) The primary image is not in the secondary images selector.
 * 
 * 8) If product is unnavailable, description is not displayed.
 * 
 * Examples:
 * ex1 (available): http://www.balaroti.com.br/produto/Cesto-multiuso-branco-com-filete-/184
 * ex2 (unavailable): http://www.balaroti.com.br/produto/Ar-condicionado-Split-60000-BTU-frio-piso-teto-CI-CE-60F-380V-/32443
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilBalarotiCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.balaroti.com.br/";

	public BrasilBalarotiCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(session.getUrl()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			/* ***********************************
			 * crawling data of only one product *
			 *************************************/

			// InternalId
			String internalId = crawlInternalId(doc);

			// Pid
			String internalPid = crawlInternalPid(doc);

			// Name
			String name = crawlName(doc);

			// Price
			Float price = crawlMainPagePrice(doc);
			
			// Availability
			boolean available = crawlAvailability(doc);

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

			// Creating the product
			Product product = new Product();
			product.setUrl(session.getUrl());
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


		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}
		
		return products;
	}



	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		if ( url.startsWith(HOME_PAGE + "produto/") ) return true;
		return false;
	}
	
	
	/*******************
	 * General methods *
	 *******************/
	
	private String crawlInternalId(Document document) {
		String internalId = null;
		Element internalIdElement = document.select("#idItVenda").first();

		if (internalIdElement != null) {
			internalId = internalIdElement.attr("value").toString().trim();			
		} else {
			internalIdElement = document.select("#itemVenda").first();
			internalId = internalIdElement.attr("value").toString().trim();
		}

		return internalId;
	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;

		return internalPid;
	}
	
	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select(".nomeProduto").first();

		if (nameElement != null) {
			name = nameElement.ownText().toString().trim();
		} else {
			nameElement = document.select(".caixaConjunto > h2").first();
			name = nameElement.ownText().toString().trim();
		}

		return name;
	}

	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element specialPrice = document.select("#preco span").first();		
		
		if (specialPrice != null) {
			price = Float.parseFloat( specialPrice.text().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		} 

		return price;
	}
	
	private boolean crawlAvailability(Document document) {
		Element notifyMeElement = document.select("#indisponivel").first();
		
		if (notifyMeElement != null) {
			return false;
		}
		
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
		Element primaryImageElement = document.select(".clearfix ul li a#imgPrinc_0").first();

		if (primaryImageElement != null) {
			primaryImage = primaryImageElement.attr("href").trim();
			
			if(!validateImage(primaryImage)){
				Element e = primaryImageElement.select("img").first();
				
				if(e != null){
					primaryImage = e.attr("src");
				}
			}
		}
		
		if(!validateImage(primaryImage)){
			primaryImage = null;
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select(".clearfix ul div li a");

		for (int i = 1; i < imagesElement.size(); i++) { // start with index 1, because the first image is the primary image
			Element e = imagesElement.get(i);
			String rel = e.attr("rel");
			
			JSONObject jsonImages;
			try{
				jsonImages = new JSONObject(rel);
			} catch(Exception ex){
				jsonImages = new JSONObject();
			}
			String image = null;
			
			if(jsonImages.length() > 0){
				if(jsonImages.has("largeimage")){
					image = jsonImages.getString("largeimage").trim();
				}
				
				if(!validateImage(image)){
					if(jsonImages.has("smallimage")){
						image = jsonImages.getString("smallimage");
					}
				} 
			}
			
			if(!validateImage(image)){
				Element x = e.select("img").first();
				if(x != null){
					image = x.attr("src");
				}
			}
			
			if(validateImage(image)){
				secondaryImagesArray.put(image);
			}
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private boolean validateImage(String image){
		if(image == null || image.isEmpty() || !image.startsWith("http:")){
			return false;
		} 
		
		return true;
	}
	
	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(".breadCrumb a[href]");

		for (int i = 1; i < elementCategories.size(); i++) { // starting from index 1, because the first is the market name
			categories.add( elementCategories.get(i).attr("title").trim() );
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
		Elements descriptionElement = document.select(".abaContent");
		
		for(Element e : descriptionElement){
			Element x = e.select(".titulopergunta").first();
			
			if(x != null){
				description = description + e.html();
			}
		}
		
		return description;
	}

}
