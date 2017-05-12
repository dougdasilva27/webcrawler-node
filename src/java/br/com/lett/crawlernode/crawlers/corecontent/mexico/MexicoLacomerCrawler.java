package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import models.Prices;

/**
 * Date: 28/11/2016
 * 
 * 1) Only one sku per page.
 * 
 * Price crawling notes:
 * 1) In time crawler was made, there no product unnavailable.
 * 2) There is no bank slip (boleto bancario) payment option.
 * 3) There is no installments for card payment. So we only have 
 * 1x payment, and to this value we use the cash price crawled from
 * the sku page. (nao existe divisao no cartao de credito).
 * 4) In this market has two others possibles markets, City Market = 305 and Fresko = 14
 * 5) In page of product, has all physicals stores when it is available.
 * 
 * Url example: http://www.lacomer.com.mx/lacomer/doHome.action?succId=14&pasId=63&artEan=7501055901401&ver=detallearticulo&opcion=detarticulo
 * 
 * pasId -> Lacomer
 * succId -> Tienda Lomas Anahuac (Mondelez choose)
 * 
 * @author Gabriel Dornelas
 *
 */
public class MexicoLacomerCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.lacomer.com.mx/";

	public MexicoLacomerCrawler(Session session) {
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
			String internalPid = crawlInternalPid(doc);
			String name = crawlName(doc);
			Float price = crawlPrice(doc);
			Prices prices = crawlPrices(price);
			boolean available = crawlAvailability(price);
			CategoryCollection categories = crawlCategories(doc);
			String primaryImage = crawlPrimaryImage(doc);
			String secondaryImages = crawlSecondaryImages(doc, primaryImage);
			String description = crawlDescription(doc);
			Integer stock = null;
			JSONArray marketplace = crawlMarketplace(doc);

			// Creating the product
			Product product = ProductBuilder.create()
					.setUrl(session.getOriginalURL())
					.setInternalId(internalId)
					.setInternalPid(internalPid)
					.setName(name)
					.setPrice(price)
					.setPrices(prices)
					.setAvailable(available)
					.setCategory1(categories.getCategory(0))
					.setCategory2(categories.getCategory(1))
					.setCategory3(categories.getCategory(2))
					.setPrimaryImage(primaryImage)
					.setSecondaryImages(secondaryImages)
					.setDescription(description)
					.setStock(stock)
					.setMarketplace(marketplace)
					.build();

			products.add(product);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return products;

	}

	private boolean isProductPage(Document doc) {
		if (doc.select(".product-title").first() != null) {
			return true;
		}
		return false;
	}

	private String crawlInternalId(Document document) {
		String internalId = null;

		Element internalIdElement = document.select("input[name=artEan]").first();
		if (internalIdElement != null) {
			internalId = internalIdElement.attr("value").trim();
		}

		return internalId;
	}

	/**
	 * There is no internalPid.
	 * 
	 * @param document
	 * @return
	 */
	private String crawlInternalPid(Document document) {
		String internalPid = null;

		return internalPid;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("h1.product-title").first();

		if (nameElement != null) {
			name = nameElement.text().trim();
		}

		return name;
	}

	private Float crawlPrice(Document document) {
		Float price = null;

		String priceText = null;
		Element salePriceElement = document.select(".price-sales").last();		

		if (salePriceElement != null) {
			priceText = salePriceElement.ownText().trim().toLowerCase();
			
			if(priceText.contains("$")) {
				int x = priceText.indexOf("$") + 1;
				int y = priceText.indexOf(" ", x);
				
				price = Float.parseFloat(priceText.substring(x, y));
			}
		}
		
		if(price == 0f){
			price = null;
		}

		return price;
	}

	private boolean crawlAvailability(Float price) {
		boolean available = false;

		if (price != null) {
			available = true;
		}

		return available;
	}

	private JSONArray crawlMarketplace(Document document) {
		return new JSONArray();
	}


	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select(".main-image img").first();

		if (primaryImageElement != null) {
			String image = primaryImageElement.attr("src").trim();
			
			if(!image.contains("empty")){
				primaryImage = image;
			}
		}
		

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document, String primaryImage) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private CategoryCollection crawlCategories(Document document) {
		CategoryCollection categories = new CategoryCollection();

		Elements elementCategories = document.select(".breadcrumb li a");
		for (int i = 1; i < elementCategories.size(); i++) { // first item is the home page
			categories.add( elementCategories.get(i).text().trim() );
		}

		return categories;
	}

	private String crawlDescription(Document document) {
		StringBuilder description = new StringBuilder();
		Element descriptionElement = document.select(".product-tab").first();

		if(descriptionElement != null) {
			description.append(descriptionElement.html());
		}
		
		return description.toString();
	}

	/**
	 * There is no bankSlip price.
	 * 
	 * There is no card payment options, other than cash price.
	 * So for installments, we will have only one installment for each
	 * card brand, and it will be equals to the price crawled on the sku
	 * main page.
	 * 
	 * @param doc
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(Float price) {
		Prices prices = new Prices();
		
		if(price != null){
			Map<Integer,Float> installmentPriceMap = new TreeMap<>();
			installmentPriceMap.put(1, price);
	
			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
		}
		
		return prices;
	}

}
