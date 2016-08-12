package br.com.lett.crawlernode.kernel;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import br.com.lett.crawlernode.database.Persistence;
import br.com.lett.crawlernode.kernel.fetcher.CrawlerWebdriver;
import br.com.lett.crawlernode.kernel.fetcher.DataFetcher;
import br.com.lett.crawlernode.kernel.models.ProcessedModel;
import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.processor.base.Processor;
import br.com.lett.crawlernode.queue.QueueService;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

import org.apache.http.cookie.Cookie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Crawler superclass. All crawler tasks must extend this class to override both
 * the shouldVisit and extract methods.
 * @author Samir Leao
 *
 */

public class Crawler implements Runnable {

	protected static final Logger logger = LoggerFactory.getLogger(Crawler.class);

	protected static final int MAX_TRUCO_ATTEMPTS = 3;

	protected final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|bmp|gif|jpe?g"
			+ "|png|ico|tiff?|mid|mp2|mp3|mp4"
			+ "|wav|avi|mov|mpeg|ram|m4v|pdf" 
			+ "|rm|smil|wmv|swf|wma|zip|rar|gz))(\\?.*)?$");

	/**
	 * The current crawling session.
	 */
	protected CrawlerSession session;

	/**
	 * Cookies that must be used to fetch the sku page
	 * this attribute is set by the handleCookiesBeforeFetch method.
	 */
	protected List<Cookie> cookies;
	
	/**
	 * Remote webdriver to be used in case of screenshot in truco mode
	 * or any other need. By default it's not instantiated.
	 */
	protected CrawlerWebdriver webdriver;


	public Crawler(CrawlerSession session) {
		this.session = session;
		this.cookies = new ArrayList<Cookie>();
	}
	
	
	/**
	 * Overrides the run method that will perform a task within a thread.
	 * The actual thread performs it's computation controlled by an Executor, from
	 * Java's Executors Framework.
	 */
	@Override 
	public void run() {
		Logging.printLogDebug(logger, session, "START");

		// crawl informations and create a list of products
		List<Product> products = extract();
		
		Logging.printLogDebug(logger, session, "Number of crawled products: " + products.size());
		
		/* ***************
		 * MODE INSIGHTS *
		 * ***************
		 *  
		 * There is only one product that will be processed in this mode.
		 * This product will be selected by it's internalId, passed by the CrawlerSession
		 */
		if (session.getType().equals(CrawlerSession.INSIGHTS_TYPE)) {
			for (Product product : products) {
				if (product.getInternalId() != null && product.getInternalId().equals(session.getInternalId())) {
					processProduct(product);
				}
			}
		}
		
		/* ****************
		 * MODE DISCOVERY *
		 * ****************
		 * 
		 * In this mode, we must process each crawled product.
		 */
		else {
			for (Product product : products) {
				processProduct(product);
			}
		}
		
//		try {
//			Thread.sleep(15000 + CommonMethods.randInt(2000, 10000));
//		} catch (InterruptedException e) {
//			Logging.printLogDebug(logger, session, "Error in thread sleep!");
//			e.printStackTrace();
//		}

		Logging.printLogDebug(logger, session, "Deleting task: " + session.getUrl() + "...");
		QueueService.deleteMessage(Main.queue, session.getSessionId(), session.getMessageReceiptHandle());
		
		// if the crawler used the webdriver in some point, it must be closed
		if (webdriver != null) {
			Logging.printLogDebug(logger, session, "Closing webdriver...");
			webdriver.closeDriver();
		}

		Logging.printLogDebug(logger, session, "END [trucos = " + session.getTrucoAttempts() + "]");

	}

	/**
	 * This method is responsible for the main post processing stages of a crawled product.
	 * It takes care of the following tasks:
	 * <ul>
	 * 	<li>1. Print the crawled information;</li>
	 * 	<li>2. Persist the product;</li>
	 * 	<li>3. Fetch the previous processed product. Which is a product with the same processed id as the current crawled product;</li>
	 * 	<li>4. Create a new ProcessedModel;</li>
	 * 	<li>5. Persist the new ProcessedModel;</li>
	 * </ul>
	 * <p>
	 * In this method we also have the so called 'truco' stage. In cases that we already have the ProcessedModel, we will only update
	 * the informations of the previous ProcessedModel with the new information crawled. But we don't update the information in the first try.
	 * When we detect some important change, such as in sku availability or price, we run the process all over again. The crawler runs again and all
	 * the above enumerated stages are repeated, just to be shure that the information really changed or if it isn't a crawling bug or an URL blocking,
	 * by the ecommerce website. 
	 * </p>
	 * <p>
	 * This process of rerun the crawler and so on, is repeated, until a maximum number of tries, or until we find two consecutive equals sets of
	 * crawled informations. If this occurs, then we persist the new ProcessedModel. If the we run all the truco checks, and don't find consistent
	 * information, the crawler doesn't persist the new ProcessedModel. 
	 * </p> 
	 * @param product
	 */
	private void processProduct(Product product) {
		boolean mustEnterTrucoMode = false;
		
		// print crawled information
		printCrawledInformation(product);
		
		// persist the product
		Persistence.persistProduct(product, session);

		// fetch the previous processed product stored on database
		ProcessedModel previousProcessedProduct = Processor.fetchPreviousProcessed(product, session);

		// create the new processed product
		ProcessedModel newProcessedProduct = Processor.createProcessed(product, session, previousProcessedProduct);

		if (previousProcessedProduct == null) {

			// if a new processed product was created
			if (newProcessedProduct != null) {
				Persistence.persistProcessedProduct(newProcessedProduct, session);
			} else {
				Logging.printLogError(logger, session, "The new processed product is null. Indicates that the crawler missed vital information.");
			}
		}


		else { // we already have a processed product, so we must decide if we update 

			if (newProcessedProduct != null) {

				// the two processed are different, so we must enter in truco mode
				if ( compare(newProcessedProduct, previousProcessedProduct) ) {
					mustEnterTrucoMode = true;
					Logging.printLogDebug(logger, session, "Must enter in truco mode.");
				}

				// the two processed are equals, so we can update it
				else {
					Persistence.persistProcessedProduct(newProcessedProduct, session);
					return;
				}
			}

		}

		/*
		 * Running truco
		 */
		if (mustEnterTrucoMode) {
			Logging.printLogDebug(logger, session, "Entering truco mode...");
			
			ProcessedModel currentTruco = newProcessedProduct;

			while (true) {
				List<Product> products = extract();
				
				// when we are processing all the the products in array (mode discovery)
				// we will select only the product being 'trucado'
				Product localProduct = this.getProductByInternalId(products, currentTruco.getInternalId());
				
				session.incrementTrucoAttempts();
				
				if (localProduct != null) {
					Persistence.persistProduct(localProduct, session);
					newProcessedProduct = Processor.createProcessed(localProduct, session, previousProcessedProduct);

					if (newProcessedProduct != null) {					
						if ( compare(newProcessedProduct, currentTruco) ) {
							currentTruco = newProcessedProduct;	
						} 

						// if we found two consecutive equals processed products, persist and end 
						else {
							Persistence.persistProcessedProduct(newProcessedProduct, session);
							
							// take a screenshot
//							if (webdriver == null) {
//								Logging.printLogDebug(logger, session, "Initializing webdriver");
//								webdriver = new CrawlerWebdriver();
//							}
//							String path = "/home/samirleao/Pictures/screenshots/" + 
//									session.getMarket().getCity() + "/" + 
//									session.getMarket().getName() + "/" +
//									session.getUrl() + ".png";
//							webdriver.takeScreenshot(session.getUrl(), path);
							
							return;
						}
					}
				}
				
				if (session.getTrucoAttempts() >= MAX_TRUCO_ATTEMPTS) break;
				
			}
		}
	}
	

	/**
	 * It defines wether the crawler must true to extract data or not.
	 * @param url
	 * @return
	 */
	public boolean shouldVisit() {
		return true;
	}

	/**
	 * By default this method only set the list of cookies to null.
	 * If the crawler needs to set some cookie to fetch the sku page,
	 * then it must implement this method.
	 */
	public void handleCookiesBeforeFetch() {
		this.cookies = null;
	}

	/**
	 * Performs any desired transformation on the URL before the actual fetching.
	 * @param the URL we want to modify
	 * @return the modified URL, that will be used in the fetching
	 */
	public String handleURLBeforeFetch(String url) {
		return url;
	}
	
	/**
	 * Performs the data extraction of the URL in the session.
	 * The four main steps of this method are:
	 * <ul>
	 * <li>Handle Cookies: Set any necessary cookies to do a proper http request of the page.</li>
	 * <li>Handle URL: Makes any necessary modification on the URL in the session, before the request.</li>
	 * <li>Fetch: Do a http request and fetch the page data as a DOM.</li>
	 * <li>Extraction: Crawl all skus in the URL on the crawling session.</li>
	 * </ul> 
	 * @return An array with all the products crawled in the URL passed by the CrawlerSession, or an empty array list if no product was found.
	 */
	public List<Product> extract() {

		// handle cookie
		handleCookiesBeforeFetch();

		// handle URL modifications
		String url = handleURLBeforeFetch(session.getUrl());
		session.setUrl(url);
		session.setOriginalURL(url);
		
		if ( shouldVisit() ) {
			Document document = fetch();
			List<Product> products = extractInformation(document);
			if (products.isEmpty()) products.add( new Product() );
			return products;
		}

		List<Product> products = new ArrayList<Product>();
		if (products.isEmpty()) products.add( new Product() );
		return products;
	}


	/**
	 * Contains all the logic to sku information extraction.
	 * Must be implemented on subclasses.
	 * By default, returns an empty product.
	 * @param document
	 * @return A product with all it's crawled informations
	 */
	public List<Product> extractInformation(Document document) {
		return new ArrayList<Product>();
	}

	/**
	 * Request the sku URL and parse to a DOM format
	 * @return Parsed HTML in form of a Document
	 */
	private Document fetch() {
		String html = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, session.getUrl(), null, cookies);
		return Jsoup.parse(html);		
	}

	/**
	 * Compare ProcessedModel p1 against p2
	 * p2 is suposed to be the truco, that is the model we are checking against
	 * @return true if they are different or false otherwise
	 */
	private boolean compare(ProcessedModel p1, ProcessedModel p2) {
		return p1.compareHugeChanges(p2);
	}

	private void printCrawledInformation(Product product) {
		Logging.printLogDebug(logger, session, "Crawled information: " + product.toString());
	}
	
	/**
	 * Get only the product with the desired internalId.
	 * @param products
	 * @param internalId
	 * @return The product with the desired internal id.
	 */
	private Product getProductByInternalId(List<Product> products, String internalId) {
		for (Product product : products) {
			if (product.getInternalId() != null && product.getInternalId().equals(internalId)) {
				return product;
			}
		}
		
		return null;
	}

}
