package br.com.lett.crawlernode.crawlers.corecontent.fortaleza;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

/************************************************************************************************************************************************************************************
 * Crawling notes (15/07/2016):
 * 
 * 1) For this crawler, we have one url per each sku. There is no page is more than one sku in it.
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking for an specific html element.
 * 
 * 5) Even if a product is unavailable, its price is displayed.
 * 
 * 6) There is no internalPid for skus in this ecommerce. The internalPid must be a number that is the same for all
 * the variations of a given sku.
 * 
 * 7) InternalId is in url, because in webranking is the unique id found.
 * 
 * 8) We have one method for each type of information for a sku (please carry on with this pattern).
 * 
 * 9) There is no secondary image for this market
 * 
 * Examples:
 * ex1 (available): http://loja.paguemenos.com.br/alimento-infantil-nestle-iogurte-original-120-g-387195.aspx/p
 * ex2 (unavailable): http://loja.paguemenos.com.br/alimento-infantil-nestle-frutas-tropicais-120g-4922.aspx/p
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class FortalezaPaguemenosCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://loja.paguemenos.com.br/";

	public FortalezaPaguemenosCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	@Override
	public void handleCookiesBeforeFetch() {
		Logging.printLogDebug(logger, session, "Adding cookie...");
		
		BasicClientCookie cookie = new BasicClientCookie("StoreCodePagueMenos", "52");
		cookie.setDomain("loja.paguemenos.com.br");
		cookie.setPath("/");
		this.cookies.add(cookie);
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

			// Prices 
			Prices prices = crawlPrices(doc, price);
			
			// Creating the product
			Product product = new Product();
			
			product.setUrl(this.session.getOriginalURL());
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

		} else {
			Logging.printLogDebug(logger, session, "Not a product page: " + this.session.getOriginalURL());
		}
		
		return products;
	}

	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(Document doc) {
		if ( doc.select("#info-product").first() != null ) return true;
		return false;
	}


	/*******************
	 * General methods *
	 *******************/

	private String crawlInternalId(Document document) {
		String internalId = null;
		Element internalIdElement = document.select("#liCodigoInterno span[itemprop=identifier]").first();

		if (internalIdElement != null) {
			internalId = internalIdElement.text().trim();			
		}

		return internalId;
	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;

		return internalPid;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("h1.name.fn").first();

		if (nameElement != null) {
			name = nameElement.text().toString().trim();
		}

		return name;
	}

	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element mainPagePriceElement = document.select("#lblPrecoPor strong").first();

		if (mainPagePriceElement != null) {
			price = Float.parseFloat( mainPagePriceElement.text().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		}

		return price;
	}

	private boolean crawlAvailability(Document document) {
		Float price = crawlMainPagePrice(document);
		if (price != null) return true;
		return false;
	}

	private Map<String, Float> crawlMarketplace(Document document) {
		return new HashMap<String, Float>();
	}

	private JSONArray assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		return new JSONArray();
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select(".collum.images #hplAmpliar").first();

		if (primaryImageElement != null) {
			primaryImage = primaryImageElement.attr("href").trim();

			if(primaryImage.equals("../#")){ //no image for product
				return null;
			}
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select("#breadcrumbs span a[href] span");

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
		Element descriptionElement = document.select("#panCaracteristica").first();

		if (descriptionElement != null) description = description + descriptionElement.html();

		return description;
	}

	/**
	 * In this market has no bank slip payment method
	 * @param doc
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(Document doc, Float price){
		Prices prices = new Prices();
		
		if(price != null){
			Map<Integer,Float> installmentPriceMap = new HashMap<>();
			installmentPriceMap.put(1, price);
			
			Element installments = doc.select("#lblParcelamento").first();
			
			if(installments != null){
				Element installmentElement = installments.select("#lblParcelamento1 > strong").first();
				
				if(installmentElement != null) {
					Integer installment = Integer.parseInt(installmentElement.text().replaceAll("[^0-9]", ""));
					
					Element valueElement = installments.select("#lblParcelamento2 > strong").first();
					
					if(valueElement != null) {
						Float value = MathCommonsMethods.parseFloat(valueElement.text());
						
						installmentPriceMap.put(installment, value);
					}
				}
			}
			
			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
		}
		
		return prices;
	}
}
