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

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.core.task.Crawler;
import br.com.lett.crawlernode.util.Logging;

/************************************************************************************************************************************************************************************
 * Crawling notes (01/08/2016):
 * 
 * 1) For this crawler, we have one url per mutiple skus.
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking for an specific html element.
 * 
 * 5) Even if a product is unavailable, its price is not displayed if product has no variations.
 * 
 * 6) There is internalPid for skus in this ecommerce. The internalPid is a number that is the same for all
 * the variations of a given sku.
 * 
 * 7) The primary image is the first image in the secondary images selector.
 * 
 * 8) To get price of variations is accessed a api to get them.
 * 
 * 9) In products have variations, price is displayed if product is unavailable;
 * 
 * Examples:
 * ex1 (available): https://www.econtinental.com.br/fogao-electrolux-chef-super-52sb-piso-4-bocas-branco-chama-rapida-bivolt
 * ex2 (unavailable): https://www.econtinental.com.br/ar-split-cassete-springer-48000-btus-quente-e-frio-220v
 * ex3 (variations): https://www.econtinental.com.br/lavadora-de-roupas-electrolux-turbo-15kg-turbo-branca
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilEcontinentalCrawler extends Crawler {

	private final String HOME_PAGE = "https://www.econtinental.com.br/";

	public BrasilEcontinentalCrawler(CrawlerSession session) {
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
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
			
			Elements variations = doc.select("#ctrEscolheTamanho li a");

			if(variations.size() > 0){
				
				/* ***********************************
				 * crawling data of mutiple products *
				 *************************************/
				
				for(Element e : variations){
					
					// InternalID
					String internalId = e.attr("idgrade");
					
					// Name Variation
					String nameVariation = e.text();
					
					// Pid
					String internalPid = crawlInternalPid(doc);

					// Name
					String name = crawlName(doc) + " - " + nameVariation;
					
					//JSON price
					JSONObject jsonPrice = this.fetchApiPrice(internalId);
					
					// Availability
					boolean available = crawlAvailabilityVariation(e);
					
					// Price
					Float price = crawlPriceVariation(jsonPrice);

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
					product.setUrl(this.session.getOriginalURL());
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
			}
			
			else {
				
				/* ***********************************
				 * crawling data of only one product *
				 *************************************/

				// InternalId
				String internalId = crawlInternalId(doc);

				// Pid
				String internalPid = crawlInternalPid(doc);

				// Name
				String name = crawlName(doc);

				// Availability
				boolean available = crawlAvailability(doc);
						
				// Price
				Float price = crawlMainPagePrice(doc, available);
				
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
				product.setUrl(this.session.getOriginalURL());
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

	private boolean isProductPage(Document document) {
		if ( document.select(".content-product").first() != null ) return true;
		return false;
	}
	
	/*********************
	 * Variation product *
	 *********************/	
	
	private JSONObject fetchApiPrice(String internalID){
		String urlVariation = "https://www.econtinental.com.br/produto/do_escolhe_variacao?idgrade=" + internalID;
		
		Map<String,String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		headers.put("X-Requested-With", "XMLHttpRequest");
		
		JSONObject jsonPrice = new JSONObject(DataFetcher.fetchPagePOSTWithHeaders(urlVariation, session, "", null, 1, headers));
		
		return jsonPrice;
	}
	
	private Float crawlPriceVariation(JSONObject jsonPrice) {
		Float price = null;
		
		if (jsonPrice.has("valor")) {
			price = Float.parseFloat(jsonPrice.getString("valor").replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		} 

		return price;
	}
	
	private boolean crawlAvailabilityVariation(Element e) {
		
		if (e.hasAttr("maxqtdcompra") && !e.attr("maxqtdcompra").isEmpty()) {
			int stock = Integer.parseInt(e.attr("maxqtdcompra").replaceAll("[^0-9]", "").trim());
			
			if(stock > 0) {
				return true;
			}
		}
		
		return false;
	}
	
	/*******************
	 * General methods *
	 *******************/
	
	private String crawlInternalId(Document document) {
		String internalId = null;
		
		Element internalIdElement = document.select("#ctrIdGrade").first();

		if(internalIdElement != null){
			internalId = internalIdElement.attr("value");
		}
		
		return internalId;
	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;
		
		Element internalPidElement = document.select(".submensagem").first();

		if (internalPidElement != null) {
			internalPid = internalPidElement.text().toString().trim();
			
			if(internalPid.contains(":")){
				int x = internalPid.indexOf(":");
				
				internalPid = internalPid.substring(x+1).trim();
			}
		}
		
		return internalPid;
	}
	
	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("h1[itemprop=name]").first();

		if (nameElement != null) {
			name = nameElement.ownText().toString().trim();
		}

		return name;
	}

	private Float crawlMainPagePrice(Document document, boolean available) {
		Float price = null;
		Element specialPrice = document.select(".new-value-by .ctrValorMoeda").first();		
		
		if (specialPrice != null && available) {
			price = Float.parseFloat( specialPrice.text().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		} 

		return price;
	}
	
	private boolean crawlAvailability(Document document) {
		Element notifyMeElement = document.select(".produto-indisponivel").first();
		
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
		Element primaryImageElement = document.select(".ctrFotoPrincipalZoom").first();

		if (primaryImageElement != null) {
			primaryImage = primaryImageElement.attr("href").trim().replaceAll("det", "original"); // montando imagem com zoom
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select(".maisfotos-foto a");

		for (int i = 1; i < imagesElement.size(); i++) { //start with index 1 because the first image is the primary image
			secondaryImagesArray.put( imagesElement.get(i).attr("urlfoto").trim().replaceAll("det", "original") );
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select("li.f-l a span");

		for (int i = 0; i < elementCategories.size(); i++) { 
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
		Element descriptionElement = document.select(".product-description").first();
	
		if (descriptionElement != null) description = description + descriptionElement.html();

		return description;
	}
}
