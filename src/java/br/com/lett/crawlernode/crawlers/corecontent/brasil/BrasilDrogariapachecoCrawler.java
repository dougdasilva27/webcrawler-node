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
import models.Marketplace;
import models.prices.Prices;

public class BrasilDrogariapachecoCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.drogariaspacheco.com.br/";

	public BrasilDrogariapachecoCrawler(Session session) {
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

		if ( isProductPage(this.session.getOriginalURL(), doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// internalId
			Element elementID = doc.select("div.productReference").first();
			String internalID = Integer.toString(Integer.parseInt(elementID.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", ""))).trim();

			// internalPid
			String internalPid = null;

			// name
			Elements elementName = doc.select(".buy_box .productName");
			String name = elementName.text().replace("'", "").replace("’", "").trim();

			// price
			Float price = null;
			Element elementPrice = doc.select("strong.skuPrice").first();
			if (elementPrice != null) {
				price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// availability
			boolean available = true;
			Element buyButton = doc.select("a.buy-button").first();
			if (buyButton == null || buyButton.attr("style").replace(" ", "").contains("display:none")) {
				available = false;
			}

			// categories
			Elements elementCategories = doc.select(".bread-crumb li a");
			ArrayList<String> categories = new ArrayList<>();
			String category1 = "";
			String category2 = "";
			String category3 = "";

			for (int i = 1; i < elementCategories.size(); i++) {
				categories.add(elementCategories.get(i).text());
			}

			for (String category : categories) {
				if (category1.isEmpty()) {
					category1 = category;
				} else if (category2.isEmpty()) {
					category2 = category;
				} else if (category3.isEmpty()) {
					category3 = category;
				}
			}

			// images
			String primaryImage = "";
			Elements elementPrimaryImage = doc.select("img#image-main");
			primaryImage = elementPrimaryImage.first().attr("src").trim();

			if (primaryImage.contains("indisponivel.gif")) {
				primaryImage = "";
			}

			String secondaryImages = null;
			JSONArray secondaryImagesArray = new JSONArray();
			Elements elementsecondaryImages = doc.select("ul.thumbs li a");

			if (elementsecondaryImages.size() > 1) {
				for (int i = 1; i < elementsecondaryImages.size(); i++) {
					Element e = elementsecondaryImages.get(i);
					secondaryImagesArray.put(e.attr("rel"));
				}
			}
			if (secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// description
			Elements elementDescription = doc.select("div.productDescription");
			String description = elementDescription.html();

			// stock
			Integer stock = null;

			// marketplace
			Marketplace marketplace = new Marketplace();
			
			// Prices
			Prices prices = crawlPrices(price);

			Product product = new Product();
			product.setUrl(this.session.getOriginalURL());
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
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}
		
		return products;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url, Document document) {
		Element body = document.select("body").first();
		Element elementID = document.select("div.productReference").first();
		return (body.hasClass("produto") && elementID != null);
	}
	
	/**
	 * In this market has no informations of installments
	 * on product page
	 * @param doc
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(Float price){
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
		}
		
		return prices;
	}
}