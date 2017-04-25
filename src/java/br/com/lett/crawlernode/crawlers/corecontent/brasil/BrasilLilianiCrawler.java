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

/************************************************************************************************************************************************************************************
 * Crawling notes (29/08/2016):
 * 
 * 1) For this crawler, we have one url per mutiple skus.
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking for an specific html element.
 * 
 * 5) If a product is unavailable, its price is not displayed when it has no variations,
 * because when sku have, if one of variations is available, price is displayed because price is the same for the variations.
 * 
 * 6) There is internalPid for skus in this ecommerce. The internalPid is a number that is the same for all
 * the variations of a given sku.
 * 
 * 7) The primary image is the first image in the secondary images selector.
 * 
 * 8) The url of images are changed to bigger dimensions manually.
 * 
 * Examples:
 * ex1 (available): http://www.liliani.com.br/product.aspx?idproduct=343987
 * ex2 (unavailable): http://www.liliani.com.br/product.aspx?idproduct=324446
 * ex3 (variations): http://www.liliani.com.br/product.aspx?idproduct=222262
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilLilianiCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.liliani.com.br/";

	public BrasilLilianiCrawler(Session session) {
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

		if ( isProductPage(session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
			
			// Pid
			String internalPid = crawlInternalPid(session.getOriginalURL());

			// Name
			String nameMainPage = crawlName(doc);

			// Availability
			boolean available = crawlAvailability(doc);
					
			// Price
			Float price = crawlMainPagePrice(doc);
			
			// Prices
			Prices prices = crawlPricesMain(doc, price);
			
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
			
			Elements variations = doc.select(".sku table");

			if(variations.size() > 0){
				
				/* ***********************************
				 * crawling data of mutiple products *
				 *************************************/
				
				for(Element e : variations){
					
					// InternalID
					String internalId = this.crawlInternalIdVariation(e);
					
					// Name Variation
					String nameVariation = e.select("strong").first().text().trim().replaceAll("[^0-9V]", "");

					// Name
					String name = nameMainPage + " (" + nameVariation + ")";

					// Avaiability
					boolean availableVariation = this.crawlAvailabilityVariation(e);

					// Creating the product
					Product product = new Product();
					product.setUrl(session.getOriginalURL());
					product.setInternalId(internalId);
					product.setInternalPid(internalPid);
					product.setName(name);
					product.setPrice(price);
					product.setPrices(prices);
					product.setAvailable(availableVariation);
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
			}
			
			else {
				
				/* ***********************************
				 * crawling data of only one product *
				 *************************************/

				// InternalId
				String internalId = internalPid;
				
				// Creating the product
				Product product = new Product();
				product.setUrl(session.getOriginalURL());
				product.setInternalId(internalId);
				product.setInternalPid(internalPid);
				product.setName(nameMainPage);
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
				
			}

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}
		
		return products;

	}



	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		if ( url.startsWith(HOME_PAGE + "product") ) return true;
		return false;
	}
	
	/*********************
	 * Variation product *
	 *********************/	
	
	private boolean crawlAvailabilityVariation(Element sku) {	
		Element e = sku.select(".arial_12.pading_botom_6").first();
		
		if(e != null){
			String status = e.ownText().trim().toLowerCase();
			
			if(status.equals("disponibilidade imediata")){
				return true;
			}
		}
		
		return false;
	}
	
	private String crawlInternalIdVariation(Element sku) {
		String internalId = null;
		
		Element internalIdElement = sku.select("td a").first();

		if(internalIdElement != null){
			internalId = internalIdElement.attr("href").replaceAll("[^0-9]", "").trim();
		}
		
		return internalId;
	}
	
	/*******************
	 * General methods *
	 *******************/

	private String crawlInternalPid(String url) {
		String internalPid = null;
		
		String[] tokens = url.split("=");
		internalPid = tokens[tokens.length-1];
		
		return internalPid;
	}
	
	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select(".text_prod_detalhe_prod h1").first();

		if (nameElement != null) {
			name = nameElement.text().toString().trim();
		}

		return name;
	}

	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element specialPrice = document.select("#ctl00_ContentSite_dvAvailable span strong").first();		
		
		if (specialPrice != null) {
			price = Float.parseFloat( specialPrice.text().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		} 

		return price;
	}
	
	private boolean crawlAvailability(Document document) {
		Element price = document.select("#ctl00_ContentSite_dvAvailable span strong").first();
		
		if (price != null) {
			return true;
		}
		
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
		Element primaryImageElement = document.select(".img_prod img#ctl00_ContentSite_imgProductImgBig").first();

		if (primaryImageElement != null) {
			primaryImage = modifyImageUrlToLarge(primaryImageElement.attr("src")); // montando imagem com zoom
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select("#ctl00_ContentSite_dltAdditionalImages img");

		for (int i = 1; i < imagesElement.size(); i++) { //start with index 1 because the first image is the primary image
			secondaryImagesArray.put( imagesElement.get(i).attr("src").trim().replaceAll("36", "500") );
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}
	
	private String modifyImageUrlToLarge(String image){
		String urlImage = image;
		
		String[] tokens = urlImage.split("_");
		String temp = tokens[tokens.length-1];
		
		urlImage = image.replace(temp, "500.jpg");
		
		return urlImage;
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
		Element descriptionElement = document.select(".conteudo_box_detalhe_prod").first();
	
		if (descriptionElement != null) description = description + descriptionElement.html();

		return description;
	}
	
	private Prices crawlPricesMain(Document doc, Float price){
		Prices prices = new Prices();
		
		if(price != null){
			Map<Integer,Float> installmentPriceMap = new HashMap<>();
			
			Elements installments = doc.select("#ctl00_ContentSite_dltParcels td");
			
			for(Element e : installments){
				String text = e.text().toLowerCase();
				
				if(text.contains("x")){
					int x = text.indexOf("x");
					
					Integer installment = Integer.parseInt(text.substring(0, x).replaceAll("[^0-9]", ""));
					Float value = MathCommonsMethods.parseFloat(text.substring(x+1));
					
					installmentPriceMap.put(installment, value);
				}
			}
			
			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.CREDISHOP.toString(), installmentPriceMap);
		}
		
		return prices;
	}

}
