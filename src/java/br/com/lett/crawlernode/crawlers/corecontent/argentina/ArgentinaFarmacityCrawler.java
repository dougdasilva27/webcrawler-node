package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 20/09/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class ArgentinaFarmacityCrawler extends Crawler {

	private final String HOME_PAGE = "https://www.farmacity.com/";

	public ArgentinaFarmacityCrawler(Session session) {
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

			JSONObject dataLayer = crawlDatalayer(doc);
			
			String internalId = crawlInternalId(dataLayer);
			String internalPid = crawlInternalPid(dataLayer);
			String name = crawlName(doc);
			Float price = crawlPrice(doc);
			Prices prices = crawlPrices(price, doc);
			boolean available = price != null;
			CategoryCollection categories = crawlCategories(doc);
			String primaryImage = crawlPrimaryImage(doc);
			String secondaryImages = crawlSecondaryImages(doc);
			String description = crawlDescription(doc);
			Integer stock = null;
			Marketplace marketplace = crawlMarketplace();

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
		return doc.select(".main-title > a").first() != null;
	}

	private String crawlInternalId(JSONObject dataLayer) {
		String internalId = null;

		if (dataLayer.has("product")) {
			JSONObject product = dataLayer.getJSONObject("product");
			
			if(product.has("sku")) {
				internalId = product.get("sku").toString();
			}
		}

		return internalId;
	}
	
	private String crawlInternalPid(JSONObject dataLayer) {
		String internalPid = null;

		if (dataLayer.has("product")) {
			JSONObject product = dataLayer.getJSONObject("product");
			
			if(product.has("id")) {
				internalPid = product.get("id").toString();
			}
		}

		return internalPid;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select(".main-title > a").first();

		if (nameElement != null) {
			name = nameElement.text().trim();
		}

		return name;
	}

	private Float crawlPrice(Document document) {
		Float price = null;
		Element salePriceElement = document.select(".final-price .display-price").first();		

		if (salePriceElement != null) {
			price = MathCommonsMethods.parseFloat(salePriceElement.text());
		} 

		return price;
	}

	private Marketplace crawlMarketplace() {
		return new Marketplace();
	}


	private String crawlPrimaryImage(Document doc) {
		String primaryImage = null;
		Element elementPrimaryImage = doc.select(".main-pic-container > a").first();
		
		if(elementPrimaryImage != null ) {
			primaryImage = elementPrimaryImage.attr("href");
		} 
		
		return primaryImage;
	}

	/**
	 * 
	 * @param doc
	 * @return
	 */
	private String crawlSecondaryImages(Document doc) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();
		
		Elements images = doc.select(".list-gallery-thumbs > li:not(.active) > a");
		
		for(Element image : images) {
			secondaryImagesArray.put(image.attr("href"));
		}
		
		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	/**
	 * @param document
	 * @return
	 */
	private CategoryCollection crawlCategories(Document document) {
		CategoryCollection categories = new CategoryCollection();
		Elements elementCategories = document.select(".breadcrumb li[itemprop=child] a span");
		
		for (Element e : elementCategories) { 
			String cat = e.ownText().trim();
			
			if(!cat.isEmpty()) {
				categories.add( cat );
			}
		}

		return categories;
	}

	private String crawlDescription(Document doc) {
		StringBuilder description = new StringBuilder();
		
		Element elementShortdescription = doc.select("#tech-detail").first();
		
		if (elementShortdescription != null) {
			description.append(elementShortdescription.html());		
		}
		
		Element elementDescription = doc.select(".product-description-container").first();
		
		if (elementDescription != null) {
			description.append(elementDescription.html());		
		}
		
		return description.toString();
	}
	
	/**
	 * 
	 * @param doc
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(Float price, Document doc) {
		Prices prices = new Prices();
		
		if (price != null) {
			Map<Integer,Float> installmentPriceMap = new TreeMap<>();
			installmentPriceMap.put(1, price);
			
			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.CABAL.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.NARANJA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);
		}
		
		return prices;
	}

	/**
	 * Get the script having a json with the availability information
	 * @return
	 */
	private JSONObject crawlDatalayer(Document document) {
		Elements scriptTags = document.getElementsByTag("script");
		JSONObject dataLayerJson = null;
		
		for (Element tag : scriptTags){   
			
			String script = tag.html();
			
			if(script.trim().startsWith("var dataLayer = ")) {

				int x = script.indexOf("[{")+1;
				int y = script.indexOf("}]", x);
				
				String dataLayer = script.substring(x, y+1);
				
				try {
					dataLayerJson = new JSONObject(dataLayer);
				} catch (JSONException e) {
					Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
				}

				break;
			}       
		}
		
		return dataLayerJson;
	}
}
