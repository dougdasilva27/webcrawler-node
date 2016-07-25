package br.com.lett.crawlernode.crawlers;

import org.jsoup.nodes.Document;

import br.com.lett.crawlernode.base.Crawler;
import br.com.lett.crawlernode.models.CrawlerSession;

public class BrasilAmericanasCrawler extends Crawler {

	public BrasilAmericanasCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit(String url) {
		return true;
	}

	@Override
	public void extractInformation(Document doc, String url) {
		super.extractInformation(doc, url);
		
		System.out.println("Thread: " + Thread.currentThread().getId() + ", task: " + url);

//		if ( isProductPage(url) ) {
//
//			Logging.printLogDebug(logger, "Product page identified: " + url);
//
//			/* ***********************************
//			 * crawling data of only one product *
//			 *************************************/
//
//			// InternalId
//			String internalId = crawlInternalId(doc);
//
//			// Pid
//			String internalPid = crawlInternalPid(doc);
//
//			// Name
//			String name = crawlName(doc);
//
//			// Price
//			Float price = crawlMainPagePrice(doc);
//			
//			// Availability
//			boolean available = crawlAvailability(doc);
//
//			// Categories
//			ArrayList<String> categories = crawlCategories(doc);
//			String category1 = getCategory(categories, 0);
//			String category2 = getCategory(categories, 1);
//			String category3 = getCategory(categories, 2);
//
//			// Primary image
//			String primaryImage = crawlPrimaryImage(doc);
//
//			// Secondary images
//			String secondaryImages = crawlSecondaryImages(doc);
//
//			// Description
//			String description = crawlDescription(doc);
//
//			// Stock
//			Integer stock = null;
//
//			// Marketplace map
//			Map<String, Float> marketplaceMap = crawlMarketplace(doc);
//
//			// Marketplace
//			JSONArray marketplace = assembleMarketplaceFromMap(marketplaceMap);
//
//			// Creating the product
//			Product product = new Product();
//			product.setUrl(url);
//			product.setInternalId(internalId);
//			product.setInternalPid(internalPid);
//			product.setName(name);
//			product.setPrice(price);
//			product.setAvailable(available);
//			product.setCategory1(category1);
//			product.setCategory2(category2);
//			product.setCategory3(category3);
//			product.setPrimaryImage(primaryImage);
//			product.setSecondaryImages(secondaryImages);
//			product.setDescription(description);
//			product.setStock(stock);
//			product.setMarketplace(marketplace);
//
//			// execute finalization routines of this sku crawling
////			executeFinishingRoutines(product);
//
//
//		} else {
//			Logging.printLogTrace(logger, "Not a product page " + url);
//
//		}

	}

}
