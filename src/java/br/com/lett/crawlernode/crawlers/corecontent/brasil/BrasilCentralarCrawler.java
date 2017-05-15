package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;
import models.Marketplace;
import models.Prices;


/************************************************************************************************************************************************************************************
 * Crawling notes (11/07/2016):
 * 
 * 1) For this crawler, we have one url per each sku.
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking for an specific html element.
 * 
 * 5) if the sku is unavailable, it's price is not displayed. Yet the crawler tries to crawl the price.
 * 
 * 6) There is no internalPid for skus in this ecommerce. The internalPid must be a number that is the same for all
 * the variations of a given sku.
 * 
 * 7) There was no example of sku with secondary images. All the images that appears below the main image, are
 * schematics of the sku, showing measures, and they are not image files.
 * 
 * 8) We have one method for each type of information for a sku (please carry on with this pattern).
 * 
 * Examples:
 * ex1 (available): http://www.centralar.com.br/ar-condicionado-split-9000-btus-frio-220v-midea-liva-inverter-42vfca09m5.html
 * ex2 (unavailable): http://www.centralar.com.br/ar-condicionado-janela-7500-btu-s-frio-110v-consul-manual-cci07dbna.html
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilCentralarCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.centralar.com.br/";
	
	public BrasilCentralarCrawler(Session session) {
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
		List<Product> products = new ArrayList<>();

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

			// Marketplace
			Marketplace marketplace = crawlMarketplace(doc);
			
			// Prices 
			Prices prices = crawlPrices(price, doc);

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
			Logging.printLogDebug(logger, "Not a product page" + this.session.getOriginalURL());
		}
		
		return products;
	}



	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(Document document) {
		if ( document.select(".dados1").first() != null ) {
			return true;
		}
		return false;
	}
	
	
	/*******************
	 * General methods *
	 *******************/
	
	private String crawlInternalId(Document document) {
		String internalId = null;
		Element boxElement = document.select(".dados1 .box").first();
		
		if (boxElement != null) {
			Elements boxChildElements = boxElement.children();
			
			for (Element boxChild : boxChildElements) {
				String childText = StringUtils.stripAccents(boxChild.text().toLowerCase());
				
				if (childText.contains("codigo do produto")) {
					String[] tokens = childText.split(":");
					
					if (tokens.length > 1) {
						internalId = tokens[1].trim();
					}
				}
			}
		}

		return internalId;
	}

	private String crawlInternalPid(Document document) {
		return null;
	}
	
	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select(".titulo h1[property=name]").first();

		if (nameElement != null) {
			name = sanitizeName(nameElement.text());
		}

		return name;
	}

	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element mainPagePriceElement = document.select(".float.preco1").first();

		if (mainPagePriceElement != null) {
			price = Float.parseFloat( mainPagePriceElement.text().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		}

		return price;
	}
	
	private boolean crawlAvailability(Document document) {
		Element notifyMeElement = document.select(".Aviseme").first();
		if (notifyMeElement != null) return false;
		return true;
	}

	private Marketplace crawlMarketplace(Document document) {
		return new Marketplace();
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select(".foto-principal a").first();

		if (primaryImageElement != null) {
			primaryImage = primaryImageElement.attr("href").trim();
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(".breadcrumbs div[itemprop=breadcrumb] span a");

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
		Element specElement = document.select(".bx-caracteristicas-tecnicas").first();
		Element dimensionsElement = document.select(".float.medidas").first();
		
		if (specElement != null) description = description + specElement.html();
		if (dimensionsElement != null) description = description + dimensionsElement.html();

		return description;
	}
	
	/**************************
	 * Specific manipulations *
	 **************************/
	
	private String sanitizeName(String name) {
		return name.replace("'","").replace("’","").trim();
	}
	
	private Prices crawlPrices(Float price, Document doc){
		Prices prices = new Prices();
		
		if(price != null){
			Element aVista = doc.select(".preco4 .preco1").first();
			
			if(aVista != null){
				Float bankTicketPrice = MathCommonsMethods.parseFloat(aVista.text().trim());
				prices.setBankTicketPrice(bankTicketPrice);
			}
			
			Map<Integer,Float> installmentPriceMap = new HashMap<>();
			Elements installments = doc.select(".parcelasAbertas span");
			
			for(Element e : installments) {
				String text = e.text().toLowerCase();
				int x = text.indexOf("x");
				
				Integer installment = Integer.parseInt(text.substring(0,x).replaceAll("[^0-9]", "").trim());
				Float value = MathCommonsMethods.parseFloat(text.substring(x+1));
				
				installmentPriceMap.put(installment, value);
			}
			
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
		}
		
		return prices;
	}

}
