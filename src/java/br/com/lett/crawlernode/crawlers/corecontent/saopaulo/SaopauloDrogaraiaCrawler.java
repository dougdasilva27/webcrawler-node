package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

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

public class SaopauloDrogaraiaCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.drogaraia.com.br/";

	public SaopauloDrogaraiaCrawler(Session session) {
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

		if( isProductPage(this.session.getOriginalURL(), doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// ID interno
			Element elementInternalID = doc.select("#details .col-2 .data-table tr .data").first();
			String internalID = null;
			if(elementInternalID != null) {
				internalID = elementInternalID.text();
			}

			// Pid
			String internalPid = null;
			Element elementInternalPid = doc.select("input[name=product]").first();
			if (elementInternalPid != null) {
				internalPid = elementInternalPid.attr("value").trim();
			}

			// Disponibilidade
			boolean available = true;
			Element buyButton = doc.select(".add-to-cart button").first();

			if(buyButton == null) {
				available = false;
			}

			// Nome
			String name = null;
			Element elementName = doc.select(".limit.columns .col-1 .product-info .product-name h1").first();
			Element elementBrand = doc.select(".limit.columns .col-1 .product-info .product-attributes .marca.show-hover").first();
			Element elementQuantity = doc.select(".limit.columns .col-1 .product-info .product-attributes .quantidade.show-hover").first();
			if(elementName != null) {
				name = elementName.text().trim() + " - ";
			}
			if(name != null && elementBrand != null) {
				name += elementBrand.text().trim() + " - ";
			}
			if(name != null && elementQuantity != null) {
				name += elementQuantity.text().trim();
			}

			// Preço
			Float price = null;
			Element elementPrice = doc.select(".product-shop .regular-price").first();
			if(elementPrice == null) {
				elementPrice = doc.select(".product-shop .price-box .special-price .price").first();
			}
			if(elementPrice != null) {
				price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// Categorias
			Elements elementsCategories = doc.select(".breadcrumbs ul li:not(.home):not(.product) a");
			String category1 = ""; 
			String category2 = ""; 
			String category3 = "";
			for(Element category : elementsCategories) {
				if(category1.isEmpty()) {
					category1 = category.text();
				} 
				else if(category2.isEmpty()) {
					category2 = category.text();
				} 
				else if(category3.isEmpty()) {
					category3 = category.text();
				}
			}

			String primaryImage = crawlPrimaryImage(doc);
			String secondaryImages = crawlSecondaryImages(doc);

			// Descrição
			String description = "";
			Element elementDescription = doc.select("#details").first();
			if(elementDescription != null) description = elementDescription.html().trim();

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;
			
			// Prices
			Prices prices = crawlPrices(doc, price);

			Product product = new Product();

			product.setUrl(session.getOriginalURL());
			product.setInternalId(internalID);
			product.setInternalPid(internalPid);
			product.setName(name);
			product.setPrice(price);
			product.setPrices(prices);
			product.setCategory1(category1);
			product.setCategory2(category2);
			product.setCategory3(category3);
			product.setPrimaryImage(primaryImage);
			product.setSecondaryImages(secondaryImages);
			product.setDescription(description);
			product.setStock(stock);
			product.setMarketplace(marketplace);
			product.setAvailable(available);

			products.add(product);
			
		} else {
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
		}

		return products;
	}


	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url, Document document) {
		Element elementInternalID = document.select("#details .col-2 .data-table tr .data").first();
		return elementInternalID != null;
	}
	
	private String crawlPrimaryImage(Document doc) {
		String primaryImage = null;
		
		Element elementPrimaryImage = doc.select(".product-image-gallery img#image-main").first();
		if (elementPrimaryImage != null) {
			primaryImage = elementPrimaryImage.attr("data-zoom-image");
		}
		
		return primaryImage;
	}
	
	private String crawlSecondaryImages(Document doc) {
		String secondaryImages = null;
		JSONArray secundaryImagesArray = new JSONArray();
		Elements elementImages = doc.select(".product-image-gallery img.gallery-image");

		for (int i = 2; i < elementImages.size(); i++) {
			Element elementImage = elementImages.get(i);
			secundaryImagesArray.put(elementImage.attr("data-zoom-image"));
		}

		if(secundaryImagesArray.length() > 0) {
			secondaryImages = secundaryImagesArray.toString();
		}
		
		return secondaryImages;
	}

	/**
	 * In this market, installments not appear in product page
	 * 
	 * @param doc
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(Document doc, Float price){
		Prices prices = new Prices();

		if(price != null){
			Map<Integer,Float> installmentPriceMap = new HashMap<>();

			installmentPriceMap.put(1, price);
			prices.setBankTicketPrice(price);

			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
		}

		return prices;
	}
}