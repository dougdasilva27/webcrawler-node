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
import br.com.lett.crawlernode.util.MathCommonsMethods;
import models.Prices;

/************************************************************************************************************************************************************************************
 * Crawling notes (25/08/2016):
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
 * 6) There is internalPid for skus in this ecommerce. 
 * 
 * 7) The primary image is not in the secondary images selector.
 * 
 * 8) If image is unnavailable, url not ends with .jpg or any extension.
 * 
 * Examples:
 * ex1 (available): http://www.maniavirtual.com.br/produto/9002675/cafeteira-single-caf-colors-caf111-vermelho-110v-cadence
 * ex2 (unavailable): http://www.maniavirtual.com.br/produto/9013969/placa-de-video-amd-radeon-r9-380-4gb-pci-e-xfx-double-dissipation-r9-380p-4df5
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilManiavirtualCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.maniavirtual.com.br/";

	public BrasilManiavirtualCrawler(Session session) {
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

			// InternalId
			String internalId = crawlInternalId(doc);

			// Pid
			String internalPid = crawlInternalPid(doc);

			// Name
			String name = crawlName(doc);

			// Price
			Float price = crawlMainPagePrice(doc);
			
			// Prices
			Prices prices = crawlPrices(doc, price);
			
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
			String secondaryImages = crawlSecondaryImages(doc, primaryImage);

			// Description
			String description = crawlDescription(doc);

			// Stock
			Integer stock = null;

			// Marketplace map
			Map<String, Float> marketplaceMap = crawlMarketplace(doc);

			// Marketplace
			JSONArray marketplace = assembleMarketplaceFromMap(marketplaceMap);

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

	private boolean isProductPage(String url) {
		if ( url.startsWith(HOME_PAGE + "produto/") ) return true;
		return false;
	}
	
	
	/*******************
	 * General methods *
	 *******************/
	
	private String crawlInternalId(Document document) {
		String internalId = null;
		Element internalIdElement = document.select("#codigoProduto").first();

		if (internalIdElement != null) {
			internalId = internalIdElement.attr("value").toString().trim();			
		}

		return internalId;
	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;

		Element pidElement = document.select("#codigoProduto").first();

		if (pidElement != null) {
			String[] tokens = pidElement.attr("value").split("-");
			
			internalPid = tokens[0].trim();
		}
		
		return internalPid;
	}
	
	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select(".produto-titulo").first();

		if (nameElement != null) {
			name = nameElement.text().toString().trim();
		}

		return name;
	}

	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element specialPrice = document.select(".descricao-preco > em").last();		
		
		if (specialPrice != null) {
			price = Float.parseFloat( specialPrice.ownText().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		} 

		return price;
	}
	
	private boolean crawlAvailability(Document document) {
		Element notifyMeElement = document.select(".Aviseme").first();
		
		if (notifyMeElement != null) {
			return false;
		}
		
		return true;
	}

	private Map<String, Float> crawlMarketplace(Document document) {
		return new HashMap<String, Float>();
	}
	
	private JSONArray assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		return new JSONArray();
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select(".produto-imagem a").first();

		if (primaryImageElement != null) {
			String image = primaryImageElement.attr("href").trim();
			
			if(verifyAvailabilityOfImage(image)){
				primaryImage = image;
			}
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document, String primaryImage) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select(".produto-thumb-slider li a");

		for (int i = 0; i < imagesElement.size(); i++) { 
			String image = imagesElement.get(i).attr("href").trim();
			
			if(!image.equals(primaryImage) && verifyAvailabilityOfImage(image)){
				secondaryImagesArray.put(image);
			}
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private boolean verifyAvailabilityOfImage(String image){
		
		if(image.endsWith(".jpg") || image.endsWith(".png") || image.endsWith(".jpeg")){
			return true;
		}
		
		return false;
		
	}
	
	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select("#breadcrumb li a");

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
		Elements descriptionElements = document.select(".abaDescricao");
		
		for(Element e : descriptionElements){
			description += e.html();
		}
		
		return description;
	}

	private Prices crawlPrices(Document doc, Float price){
		Prices prices = new Prices();
		
		if(price != null){
			Element vistaPrice = doc.select(".skuBestPrice").first();
			
			if(vistaPrice != null){
				Float bankTicketPrice = MathCommonsMethods.parseFloat(vistaPrice.ownText());
				prices.setBankTicketPrice(bankTicketPrice);
			}
			
			Map<Integer,Float> installmentPriceMap = new HashMap<>();
			Elements installments = doc.select(".produto-container-parcelamento > ul li");
			
			for(Element e : installments){
				Element installmentElement = e.select("span").first();
				
				if(installmentElement != null){
					Integer installment = Integer.parseInt(installmentElement.text().replaceAll("[^0-9]", ""));
					
					Element installmentValue = e.select("strong").first();
					
					if(installmentValue != null){
						Float value = MathCommonsMethods.parseFloat(installmentValue.text());
						
						installmentPriceMap.put(installment, value);
					}
				}
			}
			
			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
		}
		
		return prices;
	}
}
