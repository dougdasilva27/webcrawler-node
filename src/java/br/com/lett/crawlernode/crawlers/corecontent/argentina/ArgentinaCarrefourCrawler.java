package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.Util;
import models.prices.Prices;

public class ArgentinaCarrefourCrawler extends Crawler {

	private static final String HOME_PAGE = "https://www.carrefour.com.ar/";

	public ArgentinaCarrefourCrawler(Session session) {
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

		if (isProductPage(doc)) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
			
			JSONObject dataLayer = parseDataLayer(doc);
			
			System.out.println(dataLayer);

			String internalId = crawlInternalId(doc);
			String internalPid = crawlInternalPid(session.getOriginalURL());
			String name = crawlName(doc);
			Float price = crawlPrice(doc);
			Prices prices = crawlPrices(price, doc);
			boolean available = crawlAvailability(doc);
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

	private Marketplace crawlMarketplace() {
		// TODO Auto-generated method stub
		return null;
	}

	private String crawlDescription(Document doc) {
		// TODO Auto-generated method stub
		return null;
	}

	private String crawlSecondaryImages(Document doc) {
		// TODO Auto-generated method stub
		return null;
	}

	private String crawlPrimaryImage(Document doc) {
		// TODO Auto-generated method stub
		return null;
	}

	private CategoryCollection crawlCategories(Document doc) {
		// TODO Auto-generated method stub
		return null;
	}

	private boolean crawlAvailability(Document doc) {
		// TODO Auto-generated method stub
		return false;
	}

	private Prices crawlPrices(Float price, Document doc) {
		// TODO Auto-generated method stub
		return null;
	}

	private Float crawlPrice(Document doc) {
		// TODO Auto-generated method stub
		return null;
	}

	private String crawlName(Document doc) {
		// TODO Auto-generated method stub
		return null;
	}

	private String crawlInternalPid(String originalURL) {
		// TODO Auto-generated method stub
		return null;
	}

	private boolean isProductPage(Document doc) {
		if (doc.select(".product-view").first() != null) {
			return true;
		}
		return false;
	}

	private String crawlInternalId(Document document) {
		String internalId = null;

		Element internalIdElement = document.select(".span_codigoplu").first();
		if (internalIdElement != null) {
			internalId = internalIdElement.text().replaceAll("[^0-9]", "");
		}

		return internalId;
	}

	/*
	dataLayer.push({
		"productos": [
		              {
		            	  "name": "Lavarropas Automático Samsung 8 KG WA80F5S4UTW Blanco",
		            	  "id": "6468",
		            	  "price": "15999,00",
		            	  "brand": "Samsung",
		            	  "category": "Lavado y Secado"
		              }
		              ],
		"event": "productdetailview"
	});
	*/
	private JSONObject parseDataLayer(Document document) {
		Elements scripts = document.select("script");
		try {
			return CrawlerUtils.selectJsonFromHtml(document, "script", "dataLayer.push(", ");");
		} catch (Exception e) {
			Logging.printLogWarn(logger, session, Util.getStackTraceString(e));
			return new JSONObject();
		}
	}

}
