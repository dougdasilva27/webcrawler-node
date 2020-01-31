package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

/************************************************************************************************************************************************************************************
 * Crawling notes (16/08/2016):
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
 * 8) There is no categories information in this ecommerce.
 * 
 * 9) In some products, this crawler crawl images that are not displayed. (bug site)
 * 
 * Examples:
 * ex1 (available): http://www.camicado.com.br/cafeteira-eletrica-pixie-clips-branco-e-koral-127v-nespresso/p/000000000000032728
 * ex2 (unavailable): http://www.camicado.com.br/jogo-de-colcha-queen-lumiere-branca-bordado-100-algodao-egipcio-200-fios-matelasse-3-pecas-andreza-enxovais/p/000000000000030984
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilCamicadoCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.camicado.com.br/";

	public BrasilCamicadoCrawler(Session session) {
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
		List<Product> products = new ArrayList<>();

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

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
			Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap);

			// Prices
			Prices prices = crawlPrices(doc);
			
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
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
		}
		
		return products;
	}



	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(Document document) {
		return document.select(".headerProduct").first() != null;
	}
	
	
	/*******************
	 * General methods *
	 *******************/
	
	private String crawlInternalId(Document document) {
		String internalId = null;
		Element internalIdElement = document.select("input.idProduct").first();

		if (internalIdElement != null) {
			internalId = internalIdElement.attr("value").trim();
		}

		return internalId;
	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;

		return internalPid;
	}
	
	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("#dsNameProduct").first();

		if (nameElement != null) {
			name = nameElement.text().trim();
		}

		return name;
	}

	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element specialPrice = document.select(".price strong").first();		
		
		if (specialPrice != null) {
			price = Float.parseFloat(specialPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
		} 

		return price;
	}
	
	private Prices crawlPrices(Document document) {
		Prices prices = new Prices();
		Float bankTicketPrice = null;
		Map<Integer, Float> installments = new TreeMap<Integer, Float>();
		
		// bank ticket
		Element bankTicketPriceElement = document.select(".box-price div.price strong").first();
		if (bankTicketPriceElement != null) {
			bankTicketPrice = MathUtils.parseFloatWithComma(bankTicketPriceElement.text().trim());
		}
		
		// card payment options
		// the payment options are the same across all card brands
		Elements installmentsElements = document.select(".paymentForm div.content-parcel ul.content-parcels-item");
		for (Element installmentElement : installmentsElements) {
			Integer installmentNumber = null;
			Float installmentPrice = null;
			
			//<ul>
			//	<li>à vista</li>
			//	<li>R$ 349,90</li>
			//	<li>sem juros</li>
			//	<li>R$ 349,90</li>
			//</ul>
			
			//<ul>
			//	<li>2x</li>
			//	<li>R$ 174,95</li>
			//	<li>sem juros</li>
			//<li>R$ 349,90</li>
			
			Element installmentNumberElement = installmentElement.select("li").first(); // <li>2x</li> or <li>à vista</li>
			Element installmentPriceElement = installmentElement.select("li").get(1); // <li>R$ 349,90</li>
			List<String> parsedNumbers = MathUtils.parseNumbers(installmentNumberElement.text());
			
			if ( parsedNumbers.size() == 0 ) { // <li>à vista</li>
				installmentNumber = 1;
			} else { // <li>2x</li>
				installmentNumber = Integer.parseInt(parsedNumbers.get(0));
			}
			installmentPrice = MathUtils.parseFloatWithComma(installmentPriceElement.text());
			
			installments.put(installmentNumber, installmentPrice);
		}
		
		// insert prices on Prices object
		prices.setBankTicketPrice(bankTicketPrice);
		
		prices.insertCardInstallment(Card.VISA.toString(), installments);
		prices.insertCardInstallment(Card.MASTERCARD.toString(), installments);
		prices.insertCardInstallment(Card.DINERS.toString(), installments);
		prices.insertCardInstallment(Card.AMEX.toString(), installments);
		prices.insertCardInstallment(Card.ELO.toString(), installments);
		
		return prices;
	}
	
	private boolean crawlAvailability(Document document) {
		Element notifyMeElement = document.select(".reminder-label").first();

		return notifyMeElement == null;
	}

	private Map<String, Float> crawlMarketplace(Document document) {
		return new HashMap<>();
	}
	
	private Marketplace assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		return new Marketplace();
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select(".active-image a").first();

		if (primaryImageElement != null) {
			if(primaryImageElement.hasAttr("data-zoom")){
				if(primaryImageElement.attr("data-zoom").contains("camicado.com.br")){
					primaryImage = primaryImageElement.attr("data-zoom");
				}
			}
			
			if(primaryImage == null){
				Element e = primaryImageElement.select("img").first();
				
				if(e != null){
					primaryImage = e.attr("src");
				}
			}
		}

		if(!primaryImage.startsWith("http:")){
			primaryImage = "http:" + primaryImage;
		}
		
		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select(".aditional-images li a");

		for (int i = 1; i < imagesElement.size(); i++) { 
			Element e = imagesElement.get(i);
			String image = null;
			
			if(e.hasAttr("data-zoom")){
				if(e.attr("data-zoom").contains("camicado.com.br")){
					image = e.attr("data-zoom");
				}
			}
			
			if(image == null){
				Element x = e.select("img").first();
				
				if(x != null){
					image = e.attr("src");
				}
			}
			
			if(!image.startsWith("http:")){
				image = "http:" + image;
			}
			
			if(image != null){
				secondaryImagesArray.put(image);
			}
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();

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
