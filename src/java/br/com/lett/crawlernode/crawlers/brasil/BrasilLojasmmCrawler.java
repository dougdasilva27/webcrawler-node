package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;

/************************************************************************************************************************************************************************************
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
 * 5) If the sku is unavailable, it's price is displayed, but in a case when all variations are unnavailable, price it's not displayed.
 * 
 * 6) To get availability information, is crawl a script javascript in html.
 * 
 * 7) There is internalPid for skus in this ecommerce. The internalPid is a number that is the same for all
 * the variations of a given sku.
 * 
 * 8) If all skus in page are unnavailable, selector for skus is diferent.
 * 
 * Examples:
 * ex1 (available): https://www.lojasmm.com/suporte-gps-universal-multilaser-cp188----53175.html
 * ex2 (unavailable): https://www.lojasmm.com/cabo-micro-usb-celular-samsung-motorola---54248.html
 * ex3 (variations): https://www.lojasmm.com/smartphone-samsung-galaxy-j1-mini-duos-dual-chip-55106.html
 *
 *
 ************************************************************************************************************************************************************************************/

public class BrasilLojasmmCrawler extends Crawler {
	
	private final String HOME_PAGE = "https://www.lojasmm.com/";

	public BrasilLojasmmCrawler(CrawlerSession session) {
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

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			/* ***********************************
			 * crawling data of only one product *
			 *************************************/

			// Pid
			String internalPid = crawlInternalPid(doc);

			// Name
			String nameMainPage = crawlName(doc);

			// Categories
			ArrayList<String> categories = crawlCategories(doc);
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);

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

			// Sku variations
			Elements skus = doc.select(".ciq div div[id]");

			// Availability all products (caso específico que todos produtos estão indisponíveis)
			boolean unnavailableForAll = false;
			
			if(skus.size() < 1){
				skus = doc.select(".ciq option[class]");
				unnavailableForAll = true;
			}
			
			// Price
			Float price = crawlPrice(doc, unnavailableForAll);

			for(Element sku : skus){
				// InternalId
				String internalID = crawlInternalId(sku);

				// Name
				String name = crawlNameFinal(nameMainPage, sku);
				
				// Primary image
				String primaryImage = crawlPrimaryImage(sku, unnavailableForAll, doc);

				// Availability
				boolean available = crawlAvailability(doc, internalID, unnavailableForAll);

				// Creating the product
				Product product = new Product();
				product.setUrl(session.getOriginalURL());
				product.setInternalId(internalID);
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
		if ( document.select("span[itemprop=productID]").first() != null ) return true;
		
		return false;
	}


	/*******************
	 * General methods *
	 *******************/

	private String crawlInternalPid(Document document) {
		String internalPid = null;

		Element internalPidElement = document.select("span[itemprop=productID]").first();

		if (internalPidElement != null) {
			internalPid = internalPidElement.text().toString().trim();			
		}

		return internalPid;
	}
	
	private String crawlInternalId(Element sku) {
		String internalId = null;

		internalId = sku.attr("id").trim();
		
		if(internalId.isEmpty()){
			internalId = sku.attr("value").trim();
		}

		return internalId;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("span[itemprop=name]").first();

		if (nameElement != null) {
			name = nameElement.text().toString().trim();
		}

		return name;
	}
	
	private String crawlNameFinal(String name, Element sku) {
		String nameVariation = name;	
		Element e = sku.select("a span").first();
		
		if(e != null){
			String variation = e.text().trim();
			
			nameVariation = name + " " + variation;
			
		} else {
			String variation = sku.text().trim();
			
			if(variation.toLowerCase().contains("esgotado")){
				String[] tokens = variation.split("-");
				
				variation = variation.replace(tokens[tokens.length-1], "").trim().replaceAll("-", "");
			}
			
			nameVariation = name + " " + variation;
		}
		
		return nameVariation;
	}
	
	private Float crawlPrice(Document doc, boolean unnavailableForAll) {
		Float price = null;	
		
		if(!unnavailableForAll){
			Element priceElement = doc.select("span[itemprop=price]:not([name])").first();
		
			if(priceElement != null){
				price = Float.parseFloat( priceElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
			}
		}
		
		return price;
	}

	private boolean crawlAvailability(Document doc, String internalId, boolean unnavailableForAll){
		Elements es = doc.select(".conteudo script:not([src])");
		
		if(!unnavailableForAll){
			
			for(Element e : es){
				String script = e.outerHtml();
				
				if(script.contains("function Alerta()")){
					
					script = script.replaceAll("\"", "").replaceAll("'", "").toLowerCase();
					String ifScrpit = "if(a == "+ internalId +")";
	
					int x = script.indexOf(ifScrpit);
					int y = script.indexOf("}", x + ifScrpit.length());
					
					String element = script.substring(x + ifScrpit.length(), y);
					
					if(element.contains("outofstock")){
						return false;
					} else {
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	private Map<String, Float> crawlMarketplace(Document document) {
		return new HashMap<String, Float>();
	}

	private JSONArray assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		return new JSONArray();
	}

	private String crawlPrimaryImage(Element sku, boolean unnavailableForAll, Document doc) {
		String primaryImage = null;
		Element primaryImageElement;
		
		if(unnavailableForAll){
			primaryImageElement = doc.select("#FotoProdutoM5 div a").first();
		} else {
			primaryImageElement = sku.select("a").first();
		}
		
		if (primaryImageElement != null) {
			String image = primaryImageElement.attr("href").trim();
			
			if(!image.startsWith("https:")){
				image = "https:" + image;
			}
					
			primaryImage = image;
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select(".itemzoom:not([style])");

		for (Element e : imagesElement) { 
			String image = e.attr("href");
			
			if(!image.startsWith("https:")){
				image = "https:" + image;
			}
			
			secondaryImagesArray.put( image.trim() );
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select("#brd-crumbs ul li a:not([href=index.php])");

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
		Element descriptionElement = document.select(".InfoProd").first();

		if (descriptionElement != null) description = description + descriptionElement.html();

		return description;
	}

}
