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
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/************************************************************************************************************************************************************************************
 * Crawling notes (20/07/2016):
 * 
 * 1) For this crawler, we have URLs with multiple variations of a sku.
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking the URL format.
 * 
 * 5) Even if a product is unavailable, its price is displayed.
 * 
 * 6) There is no internalPid for skus in this ecommerce. The internalPid must be a number that is the same for all
 * the variations of a given sku.
 * 
 * 
 * Examples:
 * ex1 (available): http://www.lojadomecanico.com.br/produto/34988/21/159/mini-compressor-de-ar-air-plus-digital-de-12v-schulz-9201163-0
 * ex2 (unavailable): http://www.lojadomecanico.com.br/produto/96544/3/163/tampa-de-reservatorio-arrefecimento-do-onix-para-sa1000-planatc-10201195867 
 * 
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilLojadomecanicoCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.lojadomecanico.com.br/";

	public BrasilLojadomecanicoCrawler(Session session) {
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

		if ( isProductPage(this.session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
			
			// InternalId
			String internalId = crawlInternalId(doc);
			
			// Pid
			String internalPid = internalId;
			
			// Stock
			Integer stock = null;

			// Marketplace map
			Map<String, Float> marketplaceMap = crawlMarketplace(doc);

			// Marketplace
			Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap);
			
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
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}
		
		return products;
	}



	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		if ( url.startsWith(HOME_PAGE + "produto/") || url.startsWith(HOME_PAGE + "/produto/")) return true;
		return false;
	}
	
	/*******************
	 * General methods *
	 *******************/
	
	private String crawlInternalId(Document document) {
		String internalId = null;
		Element internalIdElement = document.select("meta[itemprop=sku]").first();

		if (internalIdElement != null) {
			internalId = internalIdElement.attr("content").toString().trim();			
		}

		return internalId;
	}
	
	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("h1[itemprop=name]").first();

		if (nameElement != null) {
			name = nameElement.ownText().toString().trim();
		}

		return name;
	}

	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element mainPagePriceElement = document.select("td.produto > div[style] > div[style=font-size:19px;font-weight:bold;color:#0074c5;]").first();

		if (mainPagePriceElement != null) {
			price = Float.parseFloat( mainPagePriceElement.text().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		}

		return price;
	}
	
	private boolean crawlAvailability(Document document) {
		Element notifyMeElement = document.select("#comprar_maior_1000").first();
		
		if (notifyMeElement != null) {
			return true;
		}
		
		return false;
	}

	private Map<String, Float> crawlMarketplace(Document document) {
		return new HashMap<String, Float>();
	}
	
	private Marketplace assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		return new Marketplace();
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select(".clearfix > img").first();

		if (primaryImageElement != null) {
			String image = primaryImageElement.attr("data-zoom-image").trim();
			if (image.endsWith(".JPG")) {
				image = image.replace(".JPG", ".jpg");
			}
			primaryImage = image.toLowerCase();
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select("#gal1 > a");

		for (int i = 1; i < imagesElement.size(); i++) { // starting from index 1, because the first is the primary image
			secondaryImagesArray.put( imagesElement.get(i).attr("data-zoom-image").trim().toLowerCase() );
		}
		
		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(".Menu_Navegacao a");

		for (int i = 1; i < elementCategories.size(); i++) { // starting from index 1, because the first is the market name
			categories.add( elementCategories.get(i).text().replace("+", "").trim() );
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
		Element descriptionElement = document.select(".descricao p").first();

		if (descriptionElement != null) description = description + descriptionElement.html();

		return description;
	}
	
	private Prices crawlPrices(Float price, Document doc){
		Prices prices = new Prices();
		
		if(price != null){
			Element aVista = doc.select(".preco0 span[itemprop=price]").first();
			
			if(aVista != null){
				Float bankTicketPrice = MathUtils.parseFloatWithComma(aVista.text().trim());
				prices.setBankTicketPrice(bankTicketPrice);
			}
			
			Map<Integer,Float> installmentPriceMap = new HashMap<>();
			installmentPriceMap.put(1, price);
						
			Element parcels = doc.select(".produto > div[style=\"\"] > div[style=\"font-size:13px;color:#666;\"]").first();
			
			if(parcels != null){
				String text = parcels.ownText().toLowerCase();
				int x = text.indexOf("x");
				
				Integer installment = Integer.parseInt(text.substring(0,x).replaceAll("[^0-9]", "").trim());
				Float value = MathUtils.parseFloatWithComma(text.substring(x+1));
					
				installmentPriceMap.put(installment, value);
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
