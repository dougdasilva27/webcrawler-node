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
import models.Marketplace;
import models.prices.Prices;

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
	
	private final String HOME_PAGE = "https://www.maniavirtual.com.br/";

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
		List<Product> products = new ArrayList<>();

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			String internalId = crawlInternalId(doc);
			String internalPid = internalId;
			String name = crawlName(doc);
			Float price = crawlMainPagePrice(doc);
			Prices prices = crawlPrices(doc, price);
			boolean available = price != null;
			
			ArrayList<String> categories = crawlCategories(doc);
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);

			String primaryImage = crawlPrimaryImage(doc);
			String secondaryImages = crawlSecondaryImages(doc, primaryImage);
			String description = crawlDescription(doc);
			Integer stock = null;

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
			product.setMarketplace(new Marketplace());

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
		return doc.select(".product-essential").first() != null;
	}
	
	
	/*******************
	 * General methods *
	 *******************/
	
	private String crawlInternalId(Document document) {
		String internalId = null;
		Element internalIdElement = document.select(".sku .value").first();

		if (internalIdElement != null) {
			internalId = internalIdElement.ownText().trim();			
		}

		return internalId;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select(".product-name h1").first();

		if (nameElement != null) {
			name = nameElement.text().trim();
		}

		return name;
	}

	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element specialPrice = document.select(".product-price span").first();		
		
		if (specialPrice != null) {
			String priceString = specialPrice.attr("content").trim();
			price = priceString.isEmpty() ? null : Float.parseFloat( priceString );
		} 

		return price;
	}
	
	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select(".gallery .picture a").first();

		if (primaryImageElement != null) {
			primaryImage = primaryImageElement.attr("href").trim();
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document, String primaryImage) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select(".picture-thumbs-item a");

		for (int i = 0; i < imagesElement.size(); i++) { 
			String image = imagesElement.get(i).attr("data-full-image-url").trim();
			
			if(!image.equals(primaryImage)){
				secondaryImagesArray.put(image);
			}
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<>();
		Elements elementCategories = document.select(".breadcrumb li span a");

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
		Element descriptionElements = document.select("#quickTab-description").first();
		
		if(descriptionElements != null) {
			description += descriptionElements.html();
		}
		
		return description;
	}

	private Prices crawlPrices(Document doc, Float price){
		Prices prices = new Prices();
		
		if(price != null){
			Element vistaPrice = doc.select(".overview div strong[style=font-size: 16px; color: #139100]").first();
			
			if(vistaPrice != null){
				Float bankTicketPrice = MathCommonsMethods.parseFloat(vistaPrice.ownText());
				prices.setBankTicketPrice(bankTicketPrice);
			}
			
			Map<Integer,Float> installmentPriceMap = new HashMap<>();
			Elements installments = doc.select("#Parcelas span");
			
			for(Element e : installments){
				String text = e.ownText().toLowerCase();
				
				if(text.contains("de")) {
					int x = text.indexOf("de") + 2;
					
					String installment = text.substring(0, x).replaceAll("[^0-9]", "");
					Float value = MathCommonsMethods.parseFloat(text.substring(x));
					
					if(!installment.isEmpty() && value != null) {
						installmentPriceMap.put(Integer.parseInt(installment), value);
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
