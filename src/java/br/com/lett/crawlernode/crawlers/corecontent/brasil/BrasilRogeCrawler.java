package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

public class BrasilRogeCrawler extends Crawler {

	private final String HOME_PAGE = "https://www.roge.com.br/";

	public BrasilRogeCrawler(Session session) {
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
			boolean available = false;
			String name = crawlName(doc);
			Float price = null;
			Prices prices = new Prices();
			CategoryCollection categories = crawlCategories(doc);
			String primaryImage = crawlPrimaryImage(doc);
			String secondaryImages = crawlSecondaryImages(doc);
			String description = crawlDescription(doc);
			Integer stock = null;
			Marketplace marketplace = new Marketplace();

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
		return doc.select(".product-essential").first() != null;
	}
	
	private String crawlInternalPid(Document doc) {
		String internalPid = null;
		Element pid = doc.select(".short-description strong").last();
		
		if(pid != null) {
			internalPid = pid.ownText().trim();
		}
		
		return internalPid;
	}

	private String crawlDescription(Document doc) {
		StringBuilder description = new StringBuilder();
		
		Element skuInformation = doc.select(".short-description").first();
		if (skuInformation != null) {
			description.append(skuInformation.html());
		}
		
		return description.toString();
	}

	/**
	 * @param doc
	 * @return
	 */
	private String crawlSecondaryImages(Document doc) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();
		
		Elements images = doc.select(".picture-thumbs > div img");
		
		for(int i = 1; i < images.size(); i++) { //first item is the primaryImage
			Element img = images.get(i);
			
			String image = img.attr("data-fullsize").trim();
			
			if(image.isEmpty()) {
				image = img.attr("data-defaultsize").trim();
			}
			
			if(image.isEmpty()) {
				image = img.attr("src").trim();
			}
			
			secondaryImagesArray.put(image);
		}
		
		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}
		
		return secondaryImages;
	}

	private String crawlPrimaryImage(Document doc) {
		String primaryImage = null;
		Element primaryImageElement = doc.select(".gallery .picture img").first();
		if (primaryImageElement != null) {
			primaryImage = primaryImageElement.attr("src").trim();
		}
		return primaryImage;
	}

	/**
	 * @param document
	 * @return
	 */
	private CategoryCollection crawlCategories(Document document) {
		CategoryCollection categories = new CategoryCollection();
		Elements elementCategories = document.select(".breadcrumb ul li a span");
		
		for (int i  = 1; i < elementCategories.size(); i++) { 
			String cat = elementCategories.get(i).ownText().trim();
			
			if(!cat.isEmpty()) {
				categories.add( cat );
			}
		}

		return categories;
	}


	private String crawlName(Document doc) {
		String name = null;
		Element nameElement = doc.select(".product-name > h1").first();
		
		if (nameElement != null) {
			name = nameElement.ownText().trim();
		}
		return name;
	}

	private String crawlInternalId(Document doc) {
		String internalId = null;
		Element internalIdElement = doc.select("#product-details-form > div[data-productid]").first();
		if (internalIdElement != null) {
			internalId = internalIdElement.attr("data-productid").trim();
		}
		return internalId;
	}

}
