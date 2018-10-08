package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.prices.Prices;

/************************************************************************************************************************************************************************************
 * Crawling notes (03/01/2017):
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
 * 6) There is no internalPid for skus in this ecommerce. 
 * 
 * 7) The primary image is the first image in the secondary images selector.
 * 
 * Examples:
 * ex1 (available): https://www.abxclimatizacao.com.br/split-springer-window-9-000-btus-110v.html
 * ex2 (unavailable): https://www.abxclimatizacao.com.br/split-hi-wall-midea-vita-inverter-12-000-btus-220v-quente-frio.html
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/
public class BrasilAbxclimatizacaoCrawler extends Crawler {

	private static final String HOME_PAGE = "https://abxarcondicionado.com.br/";

	public BrasilAbxclimatizacaoCrawler(Session session) {
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

			/* ***********************************
			 * crawling data of only one product *
			 *************************************/

			// InternalId
			String internalId = crawlInternalId(doc);

			// Pid
			String internalPid = crawlInternalPid();

			// Name
			String name = crawlName(doc);

			// Price
			Float price = crawlMainPagePrice(doc);

			// Prices
			Prices prices = crawlPrices(doc, price);

			// Availability
			boolean available = crawlAvailability(doc);

			// Categories
			ArrayList<String> categories = crawlCategories();
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);

			// Primary image
			String primaryImage = crawlPrimaryImage(doc);

			// Secondary images
			String secondaryImages = crawlSecondaryImages(doc, primaryImage);

			// Description
			String description = crawlDescription(doc);

			// Stock
			Integer stock = null;

			// Marketplace
			Marketplace marketplace = assembleMarketplaceFromMap();

			String productUrl = session.getOriginalURL();
			if(internalId != null && session.getRedirectedToURL(productUrl) != null) {
				productUrl = session.getRedirectedToURL(productUrl);
			}

			// Creating the product
			Product product = new Product();
			product.setUrl(productUrl);
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

	private boolean isProductPage(Document doc) {
		if (doc.select(".product-name") != null ){
			return true;
		}
		
		return false;
	}


	/*******************
	 * General methods *
	 *******************/

	private String crawlInternalId(Document document) {
		String internalId = null;
		Element internalIdElements = document.select(".product-shop .sku").first();

		if(internalIdElements != null) {
			internalId = internalIdElements.ownText().trim();
		}

		return internalId;
	}

	private String crawlInternalPid() {
		return null;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select(".product-name h1").first();

		if (nameElement != null) {
			name = nameElement.text().toString().trim();
		}

		Element modelName = document.select("span.sku").first();

		if(modelName != null) {
			name = name + " " + modelName.ownText();
		}

		return name;
	}

	private Float crawlMainPagePrice(Document doc) {
		Float price = null;
		Pair<Integer, Float> installment = getInstallmentPrice(doc);
		
		if(installment != null) {
			Float result = installment.getFirst() * installment.getSecond();
			price = MathUtils.normalizeTwoDecimalPlaces(result);
		}

		return price;
	}

	private boolean crawlAvailability(Document document) {
		Element notifyMeElement = document.select(".availability.out-of-stock").first();

		if (notifyMeElement != null) {
			return false;
		}

		return true;
	}

	private Marketplace assembleMarketplaceFromMap() {
		return new Marketplace();
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select(".cloud-zoom").first();

		if (primaryImageElement != null) {			
			primaryImage = primaryImageElement.attr("href").trim();
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document, String primaryImage) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select(".item .cloud-zoom-gallery");

		for (int i = 0; i < imagesElement.size(); i++) { 
			String image = imagesElement.get(i).attr("href").trim();

			if(!image.replace("thumbnail", "image").equals(primaryImage)) {
				secondaryImagesArray.put( image );	
			}

		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories() {
		return new ArrayList<>();
	}

	private String getCategory(ArrayList<String> categories, int n) {
		if (n < categories.size()) {
			return categories.get(n);
		}

		return "";
	}

	private String crawlDescription(Document document) {
		String description = "";
		Element descriptionElement = document.select(".short-description").first();
		Element princElement = document.select(".product-collateral").first();

		if (descriptionElement != null){
			description = description + descriptionElement.html();
		}
		
		if (princElement != null){
			description = description + princElement.html();
		}

		return description;
	}

	private Prices crawlPrices(Document doc, Float price){
		Prices prices = new Prices();

		if(price != null){
			Map<Integer,Float> installmentPriceMap = new TreeMap<>();
			
			// bank ticket
			Element priceBoleto = doc.select(".precoboleto span").first();
			
			if(priceBoleto != null) {
				prices.setBankTicketPrice(MathUtils.parseFloatWithComma(priceBoleto.text()));
			} else {
				prices.setBankTicketPrice(price);
			}
			
			// 1x card
			installmentPriceMap.put(1, price);
			
			Pair<Integer, Float> installment = getInstallmentPrice(doc);
			
			if(installment != null) {
				installmentPriceMap.put(installment.getFirst(), installment.getSecond());
			}

			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.BNDES.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
		}

		return prices;
	}

	private Pair<Integer, Float> getInstallmentPrice(Document doc) {
		Element finalPriceParc = doc.select(".precofinalparc").first();
		
		if(finalPriceParc != null) {
			String text = finalPriceParc.ownText().toLowerCase();
			
			if(text.contains("x")) {
				int x = text.indexOf("x");
				String number = text.substring(0, x).replaceAll("[^0-9]", "").trim();
				
				if(!number.isEmpty()) {
					Integer numberInstallments = Integer.parseInt(number);
					
					Element installmentElement = finalPriceParc.select("span").first();
					
					if(installmentElement != null) {
						Float priceInstallment = MathUtils.parseFloatWithComma(installmentElement.text());
						
						return new Pair<Integer, Float>(numberInstallments, priceInstallment);
					} 
				}
			}
		}
		
		return null;
	}
}
