package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
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
 * Date: 08/08/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilDrogariapovaoCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.drogariaspovao.com.br";

	public BrasilDrogariapovaoCrawler(Session session) {
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

		if ( isProductPage(session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			String internalId = crawlInternalId(doc, session.getOriginalURL());
			String internalPid = null;
			String name = crawlName(doc);
			Float price = crawlPrice(doc);
			Prices prices = crawlPrices(price, doc);
			boolean available = crawlAvailability(price);
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

	private boolean isProductPage(String url) {
		if (url.contains("prod_key=")) {
			return true;
		}
		return false;
	}

	private String crawlInternalId(Document doc, String url) {
		String internalId = null;

		Elements scripts = doc.select("script");
		
		for(Element e : scripts) {
			String script = e.outerHtml();
			
			if(script.contains("dataLayer.push")) {				
				int x = script.indexOf('(') + 1;
				int y = script.indexOf(");", x)+1;
				
				try {
					JSONObject datalayer = new JSONObject(script.substring(x, y));
					
					if(datalayer.has("google_tag_params")) {
						JSONObject google = datalayer.getJSONObject("google_tag_params");
						
						if(google.has("ecomm_prodid")) {
							internalId = google.getString("ecomm_prodid").trim();
						}
					}
				} catch (Exception e1) {
					Logging.printLogError(logger, session, CommonMethods.getStackTrace(e1));
				}
				
				break;
			}
		}
		
		// Casos que o produto está sem estoque
		if(internalId == null && url.contains("prod_key")) {
			int x = url.indexOf("prod_key=") + "prod_key=".length();
			
			String id = url.substring(x);
			
			if(id.contains("&")) {
				internalId = id.split("&")[0];
			} else {
				internalId = id;
			}
		}

		return internalId;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select(".arial24_666").first();
		Element unnavailableProduct = document.select(".arial12red").first();

		if (nameElement != null) {
			name = nameElement.ownText().trim();
		} else if(unnavailableProduct != null) {
			String[] tokens = unnavailableProduct.ownText().split("->");
			
			name = tokens[tokens.length-1];
		}

		return name;
	}

	private Float crawlPrice(Document document) {
		Float price = null;

		String priceText = null;
		Element salePriceElement = document.select(".arial24red").first();

		if (salePriceElement != null) {
			priceText = salePriceElement.text();
			price = MathCommonsMethods.parseFloat(priceText);
		}

		return price;
	}

	private Marketplace crawlMarketplace() {
		return new Marketplace();
	}


	private String crawlPrimaryImage(Document doc) {
		String primaryImage = null;
		Element elementPrimaryImage = doc.select("td[width=\"26%\"] img").first();
		
		if(elementPrimaryImage != null ) {
			primaryImage = HOME_PAGE + "/" + elementPrimaryImage.attr("src");
		} 
		
		return primaryImage;
	}

	/**
	 * In the time when this crawler was made, this market hasn't secondary Images
	 * @param doc
	 * @return
	 */
	private String crawlSecondaryImages(Document doc) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

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
		Elements elementCategories = document.select("a.bread");
		
		for (int i = 1; i < elementCategories.size(); i++) { 
			String cat = elementCategories.get(i).ownText().trim();
			
			if(!cat.isEmpty()) {
				categories.add( cat );
			}
		}

		return categories;
	}

	private String crawlDescription(Document doc) {
		StringBuilder description = new StringBuilder();
		
		Element elementDescription = doc.select(".arial13black").first();
		
		if (elementDescription != null) {
			description.append(elementDescription.html());		
		}
		
		return description.toString();
	}
	
	/**
	 * In the time this crawler was made, every product was available
	 * @param doc
	 * @return
	 */
	private boolean crawlAvailability(Float price) {
		return price != null;		
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
			prices.setBankTicketPrice(price);
			
			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
		}
		
		return prices;
	}

}
