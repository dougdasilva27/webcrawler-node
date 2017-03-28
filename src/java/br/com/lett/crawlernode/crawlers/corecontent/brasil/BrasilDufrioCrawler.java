package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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


/*****************************************************************************************************************************
 * Crawling notes (12/07/2016):
 * 
 * 1) For this crawler, we have one url per each sku.
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 4) The sku page identification is done simply looking for an specific html element.
 * 5) if the sku is unavailable, it's price is not displayed. Yet the crawler tries to crawl the price.
 * 6) There is no internalPid for skus in this ecommerce. The internalPid must be a number that is the same for all
 * the variations of a given sku.
 * 7) We have one method for each type of information for a sku (please carry on with this pattern).
 * 
 * Examples:
 * ex1 (available): https://www.dufrio.com.br/ar-condicionado-frio-split-cassete-inverter-35000-btus-220v-lg.html
 * ex2 (unavailable): https://www.dufrio.com.br/ar-condicionado-frio-split-piso-teto-36000-btus-220v-1-lg.html
 *
 ******************************************************************************************************************************/

public class BrasilDufrioCrawler extends Crawler {

	private final String HOME_PAGE_HTTP = "http://www.dufrio.com.br/";
	private final String HOME_PAGE_HTTPS = "https://www.dufrio.com.br/";

	private final String SKU_PAGE_IDENTIFICATION_SELECTOR 	= ".medias";
	private final String INTERNALID_SELECTOR 				= "input.sku";
	private final String NAME_SELECTOR 						= ".data h1";
	private final String PRICE_SELECTOR 					= ".sale-price span";
	private final String AVAILABILITY_SELECTOR 				= ".wd-buy-button div[style=\"display:none\"]";
	private final String PRIMARY_IMAGE_SELECTOR 			= "a.large-gallery";
	private final String SECONDARY_IMAGES_SELECTOR 			= "a.large-gallery";
	private final String CATEGORIES_SELECTOR 				= ".wd-browsing-breadcrumbs ul li a span";
	private final String DESCRIPTION_SELECTOR 				= ".wd-descriptions-text";
	
	public BrasilDufrioCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && ( (href.startsWith(HOME_PAGE_HTTPS)) || (href.startsWith(HOME_PAGE_HTTP)) );
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
			String internalPid = crawlInternalPid(session.getOriginalURL());

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
			JSONArray marketplace = crawlMarketplace(doc);
			
			// Prices 
			Prices prices = crawlPrices(price, doc);

			// Creating the product
			Product product = new Product();
			product.setUrl(session.getOriginalURL());
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
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}
		
		return products;
	}



	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(Document document) {
		if ( document.select(SKU_PAGE_IDENTIFICATION_SELECTOR).first() != null ) return true;
		return false;
	}


	/*******************
	 * General methods *
	 *******************/

	private String crawlInternalId(Document document) {
		String internalId = null;
		Elements internalIdElements = document.select(INTERNALID_SELECTOR);

		for(Element e : internalIdElements){
			String temp = e.attr("value").trim();

			if (temp.matches("\\d+")) {
				internalId = temp;
				break;
			}
		}


		return internalId;
	}

	private String crawlInternalPid(String url) {
		String internalPid = null;
		
		String urlForPid = null;
		if(url.contains("?")){
			urlForPid = url.split("\\?")[0];
		} else {
			urlForPid = url;
		}
		
		String[] tokens = urlForPid.split("-p");
		internalPid = tokens[tokens.length-1];
		
		return internalPid;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select(NAME_SELECTOR).first();

		if (nameElement != null) {
			name = sanitizeName(nameElement.text());
		}

		return name;
	}

	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element mainPagePriceElement = document.select(PRICE_SELECTOR).first();

		if (mainPagePriceElement != null) {
			price = Float.parseFloat( mainPagePriceElement.text().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		}

		return price;
	}

	private boolean crawlAvailability(Document document) {
		Element notifyMeElement = document.select(AVAILABILITY_SELECTOR).first();
		if (notifyMeElement != null) return false;
		return true;
	}

	private JSONArray crawlMarketplace(Document document) {
		return new JSONArray();
	}

	/**
	 * Crawl the primary image.
	 * The URL crawled must be modified. It contains the size of the image as parameters in the URL.
	 * It was observed that the biggest size is 900x900. These two values are modified using an auxiliar method
	 * from the class URLUtils. Note that the same modification must be done on the secondary images.
	 * 
	 * @param document
	 * @return the URL from the primary image
	 */
	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select(PRIMARY_IMAGE_SELECTOR).first();

		if (primaryImageElement != null) {
			primaryImage = primaryImageElement.attr("href").trim();
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select(SECONDARY_IMAGES_SELECTOR);

		for (int i = 1; i < imagesElement.size(); i++) { // starting from index 1, because the first is the primary image
			secondaryImagesArray.put(imagesElement.get(i).attr("href").trim());
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(CATEGORIES_SELECTOR);

		for (int i = 1; i < elementCategories.size() - 1; i++) { // starting from index 1, because the first is the market name
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
		Element descriptionElement = document.select(DESCRIPTION_SELECTOR).first();
		if (descriptionElement != null) description = description + descriptionElement.html();

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
			Element aVista = doc.select(".price-2 .priceContainer .savings .instant-price").first();
			Map<Integer,Float> installmentPriceMap = new HashMap<>();
			
			if(aVista != null){
				// Preço de boleto e 1 vez no cartão são iguais
				Float bankTicketPrice = MathCommonsMethods.parseFloat(aVista.text().trim());
				prices.insertBankTicket(bankTicketPrice);
				installmentPriceMap.put(1, bankTicketPrice);
			}
	
			Elements installments = doc.select(".price-2 .condition");
			
			Element parcels = installments.select(".parcels").first();
			
			if(parcels != null){				
				Integer installment = Integer.parseInt(parcels.text().replaceAll("[^0-9]", "").trim());
				
				Element parcelValue = installments.select(".parcel-value").first();
				
				if(parcelValue != null){		
					Float value = MathCommonsMethods.parseFloat(parcelValue.text());
					
					installmentPriceMap.put(installment, value);
				}
			}
			
			
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AURA.toString(), installmentPriceMap);
		}
		
		return prices;
	}

}
