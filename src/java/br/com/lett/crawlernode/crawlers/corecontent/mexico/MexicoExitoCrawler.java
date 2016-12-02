package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;

/**
 * Date: 28/11/2016
 * 
 * 1) Only one sku per page.
 * 
 * Price crawling notes:
 * 1) In time crawler was made, there no product unnavailable.
 * 2) There is no bank slip (boleto bancario) payment option.
 * 3) There is no installments for card payment. So we only have 
 * 1x payment, and to this value we use the cash price crawled from
 * the sku page. (nao existe divisao no cartao de credito).
 * 
 * @author Gabriel Dornelas
 *
 */
public class MexicoExitoCrawler extends Crawler {

	private final String HOME_PAGE = "https://www.exito.com/";

	public MexicoExitoCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}
	
	@Override
	public void handleCookiesBeforeFetch() {
		Logging.printLogDebug(logger, session, "Adding cookie...");
	
		Map<String,String> cookiesMap = DataFetcher.fetchCookies(session, HOME_PAGE, cookies, 1);
		
		for(String cookie : cookiesMap.keySet()){
			BasicClientCookie cookie2 = new BasicClientCookie(cookie, cookiesMap.get(cookie));
			cookie2.setDomain(".exito.com");
			cookie2.setPath("/");
			this.cookies.add(cookie2);
			
			System.err.println(cookie2);
		}
		
//		BasicClientCookie cookie = new BasicClientCookie("incap_ses_298_678271", "EdwbOmvWZH9dM8+rhbYiBAZiQVgAAAAA2dsmdCCWT+EDC/STMaBMhQ==");
//		cookie.setDomain(".exito.com");
//		cookie.setPath("/");
//		this.cookies.add(cookie);
//		
//		BasicClientCookie cookie2 = new BasicClientCookie("nlbi_678271", "yaJoKtj4JgbL5aqjCyiYNQAAAACu5L1FV+yStRkrgHog9/1q");
//		cookie2.setDomain(".exito.com");
//		cookie2.setPath("/");
//		this.cookies.add(cookie2);
//		
//		BasicClientCookie cookie3 = new BasicClientCookie("visid_incap_678271", "8UC6CJAATBSTxclf+DABlQZiQVgAAAAAQkIPAAAAAACAcI54AciDI6RdNLd2XWfkqGoZS8hwua8r");
//		cookie3.setDomain(".exito.com");
//		cookie3.setPath("/");
//		this.cookies.add(cookie3);
//		
//		BasicClientCookie cookie4 = new BasicClientCookie("JSESSIONID", "55A252A2494B5C6A6012B3DF312D020A.node1");
//		cookie4.setDomain("www.exito.com");
//		cookie4.setPath("/");
//		this.cookies.add(cookie4);
//		
//		BasicClientCookie cookie5 = new BasicClientCookie("tms_wsip", "1");
//		cookie5.setDomain("www.exito.com");
//		cookie5.setPath("/");
//		this.cookies.add(cookie5);
	}


	private final static String MAIN_SELLER_NAME_LOWER = "exito";
	
	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

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

			System.err.println(secondaryImages);

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
			
			try {
				BufferedWriter out = new BufferedWriter(new FileWriter("/home/gabriel/Desktop/exito.html"));
				
				out.write(doc.toString());
				out.close();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
		}

		return products;

	}

	private boolean isProductPage(Document doc) {
		if (doc.select("#pdpPage").first() != null) return true;
		return false;
	}

	private String crawlInternalId(Document document) {
		String internalId = null;

		Element internalIdElement = document.select("meta[itemprop=sku]").first();
		if (internalIdElement != null) {
			internalId = internalIdElement.attr("value");
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
		Element lojista = doc.select(".productSoldBy").first();
		
		if(lojista != null) {
			Prices prices = new Prices();
			String partnerName = lojista.text().trim().toLowerCase();
			
			Float price = null;
			Element salePriceElement = doc.select(".priceOffer").first();		

			if (salePriceElement != null) {
				price = Float.parseFloat(salePriceElement.ownText().replaceAll("\\$", "").replaceAll(",", ""));
				
				Map<Integer,Float> installmentMapPrice = new HashMap<>();
				installmentMapPrice.put(1, price);
				
				prices.insertCardInstallment(Card.AMEX.toString(), installmentMapPrice);
				prices.insertCardInstallment(Card.DINERS.toString(), installmentMapPrice);
				prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentMapPrice);
				prices.insertCardInstallment(Card.VISA.toString(), installmentMapPrice);
				prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentMapPrice);
			}
			
			marketplaces.put(partnerName, prices);
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
			primaryImage = "http://www.exito.com/" + primaryImageElement.attr("src").trim();
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document, String primaryImage) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select(".pdpThumbailsItem:not(.video) img");

		for (int i = 1; i < imagesElement.size(); i++) { // first index is the primary image
			String image = "https://www.superama.com.mx" + imagesElement.get(i).attr("src").trim().replace("xs", "lrg");
			
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
