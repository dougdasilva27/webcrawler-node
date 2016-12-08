package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.support.ui.Select;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;

/**
 * Date: 08/12/2016
 * 
 * 1) In time crawler was made, there no product unnavailable.
 * 2) There is no bank slip (boleto bancario) payment option.
 * 3) There is no installments for card payment. So we only have 
 * 1x payment, and to this value we use the cash price crawled from
 * the sku page. (nao existe divisao no cartao de credito).
 * 
 * 4) In this crawler is requires use webdriver
 * 5) Bogota was city choose
 * 6) Is required some cookies that are only getted via webdriver
 * 
 * @author Gabriel Dornelas
 *
 */
public class ColombiaCarullaCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.carulla.com/";

	public ColombiaCarullaCrawler(Session session) {
		super(session);
		//this.config.setFetcher(Fetcher.WEBDRIVER);
	}

	@Override
	public boolean shouldVisit() {
		String href = session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	@Override
	public void handleCookiesBeforeFetch() {
		this.webdriver = DynamicDataFetcher.fetchPageWebdriver(HOME_PAGE, session);
		
		new Select( this.webdriver.driver.findElement(By.id("ddlSelectCity"))).selectByVisibleText("Bogotá");
	    this.webdriver.driver.findElement(By.cssSelector("option[value=\"BG\"]")).click();
	    this.webdriver.driver.findElement(By.linkText("Continuar")).click();
	    
	    Set<Cookie> cookiesSelenium = this.webdriver.driver.manage().getCookies();
	    
	    for(Cookie c : cookiesSelenium){
	    	if(!c.getName().startsWith("x-")){
		    	BasicClientCookie cookie = new BasicClientCookie(c.getName(), c.getValue());
				cookie.setDomain(c.getDomain());
				cookie.setPath(c.getPath());
				this.cookies.add(cookie);
	    	}
	    }
	}
	
	private final static String MAIN_SELLER_NAME_LOWER = "carulla";
	
	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
			
//			try {
//				this.webdriver.takeScreenshotFromCurrentLoadedPage("/home/gabriel/Desktop/teste.png");
//				new Select( this.webdriver.driver.findElement(By.id("ddlSelectCity"))).selectByVisibleText("Bogotá");
//			    this.webdriver.driver.findElement(By.cssSelector("option[value=\"BG\"]")).click();
//			    this.webdriver.driver.findElement(By.linkText("Continuar")).click();
//			    
//			    
//			    this.webdriver.takeScreenshotFromCurrentLoadedPage("/home/gabriel/Desktop/teste.png");
//			    // get the new html and parse
//				String html = this.webdriver.getCurrentPageSource();
//				doc = Jsoup.parse(html);
//			} catch(Exception e){
//			}
			
			String internalId = crawlInternalId(doc);
			String internalPid = crawlInternalPid(doc);
			String name = crawlName(doc);
			
			Map<String,Prices> marketplaceMap  = crawlMarketplace(doc);
			JSONArray marketplace = assembleMarketplaceFromMap(marketplaceMap);
			
			Float price = crawlPrice(marketplaceMap);
			Prices prices = crawlPrices(marketplaceMap);
			boolean available = crawlAvailability(marketplaceMap);
			
			
			CategoryCollection categories = crawlCategories(doc);
			String primaryImage = crawlPrimaryImage(doc);
			String secondaryImages = crawlSecondaryImages(doc, primaryImage);
			String description = crawlDescription(doc);
			Integer stock = null;

			// Creating the product
			Product product = ProductBuilder.create()
					.setUrl(session.getOriginalURL())
					.setInternalId(internalId)
					.setInternalPid(internalPid)
					.setName(name)
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

		} else {			
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
		}

		return products;

	}

	private boolean isProductPage(String url) {
		if (url.startsWith(HOME_PAGE+"product")) return true;
		return false;
	}
	
	private String crawlInternalId(Document document) {
		String internalId = null;

		Element internalIdElement = document.select(".btn.btn-warning").first();
		if (internalIdElement != null) {
			internalId = internalIdElement.attr("sku");
		}

		return internalId;
	}

	/**
	 * There is no internalPid.
	 * 
	 * @param document
	 * @return
	 */
	private String crawlInternalPid(Document document) {
		String internalPid = null;

		Element internalIdElement = document.select(".btn.btn-warning").first();
		if (internalIdElement != null) {
			internalPid = internalIdElement.attr("prd");
		}
		
		return internalPid;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select(".pdpInfoProductName").first();

		if (nameElement != null) {
			name = nameElement.text().trim();
		}

		return name;
	}

	private Float crawlPrice(Map<String, Prices> marketplaces) {
		Float price = null;

		for (String seller : marketplaces.keySet()) {
			if (seller.equals(MAIN_SELLER_NAME_LOWER)) {
				if(marketplaces.get(MAIN_SELLER_NAME_LOWER).getRawCardPaymentOptions(Card.AMEX.toString()).has("1")){
					Double priceDouble = marketplaces.get(MAIN_SELLER_NAME_LOWER).getRawCardPaymentOptions(Card.AMEX.toString()).getDouble("1");
					price = priceDouble.floatValue(); 
				}
				
				break;
			}
		}		
		return price;
	}

	private boolean crawlAvailability(Map<String, Prices> marketplaces) {
		boolean available = false;

		for (String seller : marketplaces.keySet()) {
			if (seller.equals(MAIN_SELLER_NAME_LOWER)) {
				available = true;
			}
		}

		return available;
	}

	private Map<String,Prices> crawlMarketplace(Document doc) {
		Map<String,Prices> marketplaces = new HashMap<>();
		Element lojista = doc.select(".product-seller").first();
		
		if(lojista != null) {
			Prices prices = new Prices();
			String partnerName = (lojista.text().split(":")[1].toLowerCase().replaceAll("[^A-Za-z]+", "")).trim(); // has tab in partnerName
			
			Float price = null;
			Element salePriceElement = doc.select(".otherMedia > span ").first();
			
			if(salePriceElement == null){
				salePriceElement = doc.select(".priceOffer").first();
			} 
			
			if(salePriceElement == null){
				salePriceElement = doc.select("h4.price").first();
			} 
			
			if (salePriceElement != null) {
				String textPrice = salePriceElement.ownText().replaceAll("\\$", "").replaceAll(",", "").trim();
				
				if(!textPrice.isEmpty()){
					price = Float.parseFloat(textPrice);
					
					Map<Integer,Float> installmentMapPrice = new HashMap<>();
					installmentMapPrice.put(1, price);
					
					prices.insertCardInstallment(Card.AMEX.toString(), installmentMapPrice);
					prices.insertCardInstallment(Card.DINERS.toString(), installmentMapPrice);
					prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentMapPrice);
					prices.insertCardInstallment(Card.VISA.toString(), installmentMapPrice);
					
					//Shop card
					Element priceShopElement = doc.select(".priceOffer").first();
					if(priceShopElement != null){
						Float priceShop = Float.parseFloat(priceShopElement.ownText().replaceAll("\\$", "").replaceAll(",", ""));
						
						Map<Integer,Float> installmentMapPriceShop = new HashMap<>();
						installmentMapPriceShop.put(1, priceShop);
						
						prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentMapPriceShop);
					} else {
						prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentMapPrice);
					}
					
					marketplaces.put(partnerName, prices);
				}
			}
		}	

		return marketplaces;
	}
	
	private JSONArray assembleMarketplaceFromMap(Map<String, Prices> marketplaceMap) {
		JSONArray marketplace = new JSONArray();

		for(String sellerName : marketplaceMap.keySet()) {
			if ( !sellerName.equals(MAIN_SELLER_NAME_LOWER) ) {
				JSONObject seller = new JSONObject();
				seller.put("name", sellerName);
				
				if(marketplaceMap.get(sellerName).getRawCardPaymentOptions(Card.VISA.toString()).has("1")){
					// Pegando o preço de uma vez no cartão
					Double price = marketplaceMap.get(sellerName).getRawCardPaymentOptions(Card.VISA.toString()).getDouble("1");
					Float priceFloat = price.floatValue();				
					
					seller.put("price", priceFloat); // preço de boleto é o mesmo de preço uma vez.
				}
				
				seller.put("prices", marketplaceMap.get(sellerName).getPricesJson());

				marketplace.put(seller);
			}
		}
		
		return marketplace;
	}


	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select("#largeImageCanvas").first();

		if (primaryImageElement != null) {			
			primaryImage = "http://www.carulla.com/" + primaryImageElement.attr("src").trim();
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document, String primaryImage) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select(".pdpThumbailsItem:not(.video) img");

		for (int i = 1; i < imagesElement.size(); i++) { // first index is the primary image
			String image = "http://www.carulla.com" + imagesElement.get(i).attr("src").trim().replace("xs", "lrg");
			
			if(!image.equals(primaryImage)){
				secondaryImagesArray.put( image );	
			}
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private CategoryCollection crawlCategories(Document document) {
		CategoryCollection categories = new CategoryCollection();

		Elements elementCategories = document.select(".breadcrumb a span");
		for (int i = 0; i < elementCategories.size(); i++) { 
			categories.add( elementCategories.get(i).text().trim() );
		}

		return categories;
	}

	private String crawlDescription(Document document) {
		StringBuilder description = new StringBuilder();
		
		Element descriptionElement = document.select("#pdpCaracteristicas").first();
		Element ingredientElement = document.select("#pdpEspecificaciones").first();
	
		if(descriptionElement != null) description.append(descriptionElement.html());
		if(ingredientElement != null) description.append(ingredientElement.html());
		
		return description.toString();
	}

	/**
	 * There is no bankSlip price.
	 * In this market has no installments informations
	 * 
	 * @param doc
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(Map<String,Prices> marketplaces) {
		Prices prices = new Prices();
		

		for (String seller : marketplaces.keySet()) {
			if (seller.equals(MAIN_SELLER_NAME_LOWER)) {
				prices = marketplaces.get(seller);
				break;
			}
		}
		
		return prices;
	}

}
