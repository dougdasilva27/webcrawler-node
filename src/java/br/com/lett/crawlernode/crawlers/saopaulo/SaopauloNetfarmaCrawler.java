package br.com.lett.crawlernode.crawlers.saopaulo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;

/************************************************************************************************************************************************************************************
 * Crawling notes (27/10/2016):
 * 
 * 1) For this crawler, we have one url per each sku.
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is marketplace information in this ecommerce. 
 *  
 * 4) InternalId, Pid and Price is crawl in json like this:
 * 
 * {
    "page": "product",
    "sku": "C00497LRE00",
    "price": 19.90,
    "pid": "21397"
 * }
 * 
 * 
 * 5) The sku page identification is done simply looking the html element.
 * 
 * 6) Even if a product is unavailable, its price is not displayed, then price is null.
 * 
 * Examples:
 * ex1 (available): https://www.netfarma.com.br/mascara-para-cilios-maybelline-the-colossal-volum-express-preto-a-prova-dagua-1un.-21397
 * ex2 (unavailable): https://www.netfarma.com.br/formula-infantil-nan-supreme-2-lata-400g
 * 
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/


public class SaopauloNetfarmaCrawler extends Crawler {
	
	private final String HOME_PAGE = "https://www.netfarma.com.br/";

	public SaopauloNetfarmaCrawler(CrawlerSession session) {
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

			// Json Product
			JSONObject jsonProduct = crawlJSONProduct(doc);
			
			// ID interno
			String internalID = crawlInternalId(jsonProduct);

			// Pid
			String internalPid = crawlInternalPid(jsonProduct);

			// Nome
			String name = crawlName(doc);

			// Disponibilidade
			boolean available = true;
			Element elementOutOfStock = doc.select(".product-details__unavailable").first();
			if(elementOutOfStock != null) {
				available = false;
			}
			
			// Preço
			Float price = crawlPrice(jsonProduct, available);

			// Categorias
			String category1 = null; 
			String category2 = null; 
			String category3 = null;

			// primary image
			String primaryImage = crawlPrimaryImage(doc);

			// secondary images
			String secondaryImages = crawlSecondaryImages(doc);

			// description
			String description = crawlDescription(doc);
			
			// stock
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;
			
			// Prices
			Prices prices = crawlPrices(price);

			Product product = new Product();
			
			product.setUrl(session.getOriginalURL());
			product.setInternalId(internalID);
			product.setInternalPid(internalPid);
			product.setName(name);
			product.setPrice(price);
			product.setPrices(prices);
			product.setCategory1(category1);
			product.setCategory2(category2);
			product.setCategory3(category3);
			product.setPrimaryImage(primaryImage);
			product.setSecondaryImages(secondaryImages);
			product.setDescription(description);
			product.setStock(stock);
			product.setMarketplace(marketplace);
			product.setAvailable(available);

			products.add(product);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
		}
		
		return products;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(Document doc) {
		return (doc.select(".product-details__code").first() != null);
	}
	
	private String crawlInternalPid(JSONObject jsonProduct){
		String internalPid = null;
		
		if(jsonProduct.has("sku")){
			internalPid = jsonProduct.getString("sku").trim();
		}
		
		return internalPid;
	}
	
	private String crawlInternalId(JSONObject jsonProduct){
		String internalId = null;
		
		if(jsonProduct.has("pid")){
			internalId = jsonProduct.getString("pid").trim();
		}
		
		return internalId;
	}
	
	private String crawlName(Document document) {
		String name = null;
		
		// get base name
		Element elementName = document.select(".product-details__title").first();
		if (elementName != null) {
			name = elementName.text().trim();
		}
		
		// get 'gramatura' attribute
		Element gramaturaElement = document.select(".product-details__measurement").first();
		if (gramaturaElement != null) {
			if (name != null) name = name + " " + gramaturaElement.text().trim();
		}
		
		return name;
	}
	
	private Float crawlPrice(JSONObject jsonProduct, boolean available) {
		Float price = null;
		
		if(available){
			if(jsonProduct.has("price")){
				Double priceDouble = jsonProduct.getDouble("price");
				price = priceDouble.floatValue();
			}
		}
		
		return price;
	}
	
	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element elementPrimaryImage = document.select(".product-image img[data-zoom-image]").first();
		if (elementPrimaryImage != null) {
			primaryImage = elementPrimaryImage.attr("src").trim();
		}
		return primaryImage;
	}
	
	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();
		
		Elements element_fotosecundaria = document.select("#product-gallery > a");
		if(element_fotosecundaria.size()>1){
			for(int i=1; i<element_fotosecundaria.size();i++){
				Element e = element_fotosecundaria.get(i);
				if(e.attr("src").contains("/imagens/icon_video.png")){

				}else{
					secondaryImagesArray.put(e.attr("href"));
				}
			}

		}
		if(secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}
		
		return secondaryImages;
	}
	
	private JSONObject crawlJSONProduct(Document doc){
		JSONObject jsonProduct = new JSONObject();
		Elements scripts = doc.select("script:not([src])");
		
		for(Element e : scripts){
			String script = e.outerHtml();
			
			if(script.contains("chaordic_meta")){
				int x = script.indexOf("meta =")+6;
				int y = script.indexOf(";", x);
				
				String json = script.substring(x, y);
				jsonProduct = new JSONObject(json);
			}
		}
		
		return jsonProduct;
	}
	
	private String crawlDescription(Document document) {
		String description = "";
		Element elementWarning = document.select(".product-description .nano-content span").first();
		if (elementWarning != null) {
			description = description + elementWarning.text();
		}
		Element elementProductDetails = document.select(".product-description .nano-content div").last();
		if (elementProductDetails != null) {
			description = description + elementProductDetails.text();
		}

		return description;
	}
	
	private Prices crawlPrices(Float price){
		Prices prices = new Prices();
		
		if(price != null){
			Map<Integer,Float> installmentPriceMap = new HashMap<>();
			
			installmentPriceMap.put(1, price);
			prices.insertBankTicket(price);
			
			prices.insertCardInstallment(Prices.MASTERCARD, installmentPriceMap);
			prices.insertCardInstallment(Prices.DINERS, installmentPriceMap);
			prices.insertCardInstallment(Prices.VISA, installmentPriceMap);
			prices.insertCardInstallment(Prices.ELO, installmentPriceMap);
			prices.insertCardInstallment(Prices.AMEX, installmentPriceMap);
		}
				
		
		return prices;
	}
}
