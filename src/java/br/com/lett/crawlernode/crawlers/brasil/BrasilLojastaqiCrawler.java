package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.core.task.Crawler;
import br.com.lett.crawlernode.util.Logging;

/************************************************************************************************************************************************************************************
 * Crawling notes (16/08/2016):
 * 
 * 1) For this crawler, we have one url per mutiple skus.
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
 * 7) The primary image is the first image in the secondary images selector.
 * 
 * 8) To get internalId for a single product, is get another id to make selector then get internalId.
 * 
 * Examples:
 * ex1 (available): http://www.taqi.com.br/produto/ar-condicionado/ar-condicionado-split-consul-9000-btus-cbz09dbbna/999955380975/
 * ex2 (unavailable): http://www.taqi.com.br/produto/lixadeiras/lixadeira-profissional-orbital-excentrica-bosch-250-watts-gex-1251ae/100600/    (110v)
 * ex3 (variations): http://www.taqi.com.br/produto/furadeiras/furadeira-de-impacto-schulz-fi650/115774/
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilLojastaqiCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.taqi.com.br/";

	public BrasilLojastaqiCrawler(CrawlerSession session) {
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
			
			// Name
			String name = crawlName(doc);
			
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
			
			Elements variations = doc.select(".atributos div");

			if(variations.size() > 0) {
				
				/* ***********************************
				 * crawling data of mutiple products *
				 *************************************/
				
				for(Element e : variations){
					
					// InternalID
					String internalId = e.select("input").first().attr("value");

					// Name
					String nameVariation = name + " - " + e.select("label").first().text();
					
					// Availability
					boolean available = crawlAvailability(internalId, doc);
					
					// Price
					Float price = crawlPrice(internalId, doc, available);

					// Creating the product
					Product product = new Product();
					product.setUrl(session.getUrl());
					product.setInternalId(internalId);
					product.setInternalPid(internalPid);
					product.setName(nameVariation);
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
			}
			
			else {
								
				/* ***********************************
				 * crawling data of only one product *
				 *************************************/

				// InternalId
				String internalId = crawlInternalId(doc);
				
				// Availability
				boolean available = crawlAvailability(internalId, doc);
				
				// Price
				Float price = crawlPrice(internalId, doc, available);

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
				
			}
			

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
		
		Element internalIdElement = document.select("meta[itemprop=sku]").first();

		if(internalIdElement != null){
			String pid = internalIdElement.attr("content");
			Element id = document.select("input#productSkuId_" + pid).first();
			
			if(id != null){
				internalId = id.attr("value");
			}
		}
		
		return internalId;
	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;
		
		return internalPid;
	}
	
	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("h1#productName").first();

		if (nameElement != null) {
			name = nameElement.ownText().toString().trim();
		}

		return name;
	}
	
	private Float crawlPrice(String internalId, Document doc, boolean available) {
		Float price = null;
		
		if(available){
			Element ePrice = doc.select("#detailsSkuId_" + internalId + "  .valor span").first();
			if(ePrice != null){
				price = Float.parseFloat(ePrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
			}
		}
		
		return price;
	}
	
	private boolean crawlAvailability(String internalId, Document doc) {
		Element eAvailability = doc.select("#detailsSkuId_" + internalId).first();
		
		if(eAvailability.hasClass("detalhes_unavailable")){
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
		Element primaryImageElement = document.select(".gallery img").first();

		if (primaryImageElement != null) {
			if(primaryImageElement.hasAttr("data-zoom-image")){
				if(primaryImageElement.attr("data-zoom-image").startsWith("http:")){
					primaryImage = primaryImageElement.attr("data-zoom-image");
				} else {
					primaryImage = primaryImageElement.attr("src");
				}
			} else {
				primaryImage = primaryImageElement.attr("src");
			}
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select("#mycarousel li a");

		for (int i = 1; i < imagesElement.size(); i++) { //start with index 1 because the first image is the primary image
			Element e = imagesElement.get(i);
			
			String image;
			
			if(e.hasAttr("data-zoom-image")){
				if(e.attr("data-zoom-image").startsWith("http:")){
					image = e.attr("data-zoom-image");
				} else {
					image = e.attr("data-image");
				}
			} else {
				image = e.attr("data-image");
			}
			
			secondaryImagesArray.put(image);
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(".content_breadcumbs a");

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

	private String crawlDescription(Document document) {
		String description = "";
		Element descriptionElement = document.select("#descricaoproduto").first();
	
		if (descriptionElement != null) description = description + descriptionElement.html();

		return description;
	}

}
