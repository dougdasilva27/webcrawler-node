package br.com.lett.crawlernode.core.task;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import br.com.lett.crawlernode.core.fetcher.CrawlerWebdriver;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.core.session.CrawlerSessionError;
import br.com.lett.crawlernode.database.Persistence;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.processor.base.Processor;
import br.com.lett.crawlernode.processor.models.ProcessedModel;
import br.com.lett.crawlernode.server.S3Service;
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
	
	protected final static Pattern FILTERS = Pattern.compile
			(
			".*(\\.(css|js|bmp|gif|jpe?g"
					+ "|png|ico|tiff?|mid|mp2|mp3|mp4"
					+ "|wav|avi|mov|mpeg|ram|m4v|pdf" 
					+ "|rm|smil|wmv|swf|wma|zip|rar|gz))(\\?.*)?$"
			);

	/**
	 * Maximum attempts during active void analysis
	 * It's essentially the number of times that we will
	 * rerun the extract method to crawl a product from a page 
	 */
	protected static final int MAX_VOID_ATTEMPTS = 3;
	
	
	protected static final int MAX_TRUCO_ATTEMPTS = 3;

	
	/** The current crawling session. */
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

		// crawl informations and create a list of products
		List<Product> products = null;
		try {
			products = extract();
		} catch (Exception e) {
			Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
			if (products == null) products = new ArrayList<Product>();
		}

		Logging.printLogDebug(logger, session, "Number of crawled products: " + products.size());

		// insights session
		// there is only one product that will be selected
		// by it's internalId, passed by the crawler session
		if (session.getType().equals(CrawlerSession.INSIGHTS_TYPE)) {

			// get crawled product by it's internalId
			Logging.printLogDebug(logger, session, "Selecting product with internalId " + session.getInternalId());
			Product crawledProduct = filter(products, session.getInternalId());
			
			// if the product is void run the active void analysis
			Product activeVoidResultProduct = crawledProduct;
			if (crawledProduct.isVoid()) {
				try {
					activeVoidResultProduct = activeVoid(crawledProduct);
				} catch (Exception e) {
					Logging.printLogError(logger, session, "Error in active void method.");
					
					if (activeVoidResultProduct == null) activeVoidResultProduct = new Product();
					CrawlerSessionError error = new CrawlerSessionError(CrawlerSessionError.EXCEPTION, CommonMethods.getStackTrace(e));
					session.registerError(error);
				}
			}
			
			// after active void analysis we have the resultant
			// product after the extra extraction attempts
			// if the resultant product is not void, the we will process it
			if (!activeVoidResultProduct.isVoid()) {
				try {
					processProduct(activeVoidResultProduct);
				} catch (Exception e) {
					Logging.printLogError(logger, session, "Error in process product method.");
					
					CrawlerSessionError error = new CrawlerSessionError(CrawlerSessionError.EXCEPTION, CommonMethods.getStackTrace(e));
					session.registerError(error);
				}
			}

		}

		// discovery session
		// when processing a task of a suggested URL by the webcrawler or
		// an URL scheduled manually, we won't run active void and 
		// we must process each crawled product
		else if (session.getType().equals(CrawlerSession.DISCOVERY_TYPE) || session.getType().equals(CrawlerSession.SEED_TYPE)) {
			for (Product product : products) {
				try {
					processProduct(product);
				} catch (Exception e) {
					CrawlerSessionError error = new CrawlerSessionError(CrawlerSessionError.EXCEPTION, CommonMethods.getStackTrace(e));
					session.registerError(error);
				}
			}
		}

		// terminate the process
		terminate();
	}
	
	/**
	 * Run method to be used when testing
	 */
//	@Override 
//	public void run() {
//
//		// crawl informations and create a list of products
//		List<Product> products = null;
//		try {
//			products = extract();
//		} catch (Exception e) {
//			Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
//			if (products == null) products = new ArrayList<Product>();
//		}
//
//		Logging.printLogDebug(logger, session, "Number of crawled products: " + products.size());
//		
//		for (Product p : products) {
//			printCrawledInformation(p);
//		}
//
//	}

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
	private void processProduct(Product product) throws Exception {
		boolean mustEnterTrucoMode = false;

		// print crawled information
		printCrawledInformation(product);

		// persist the product
		Persistence.persistProduct(product, session);

		// fetch the previous processed product stored on database
		ProcessedModel previousProcessedProduct = Processor.fetchPreviousProcessed(product, session);

		if ( (previousProcessedProduct == null && (session.getType().equals(CrawlerSession.DISCOVERY_TYPE) || session.getType().equals(CrawlerSession.SEED_TYPE))) 
				||
				previousProcessedProduct != null) 
		{

			// create the new processed product
			ProcessedModel newProcessedProduct = Processor.createProcessed(product, session, previousProcessedProduct, Main.processorResultManager);

			// the product doesn't exists yet
			if (previousProcessedProduct == null) { 

				// if a new processed product was created
				if (newProcessedProduct != null) {

					// persist the new created processed product, and end
					Persistence.persistProcessedProduct(newProcessedProduct, session);
					return;
				}

				// the new processed product is null. Indicates that it occurred some faulty information crawled in the product
				// this isn't supposed to happen in insights mode, because previous to this process we ran into
				// the active void analysis. This case will only happen in with discovery url or seed url, where we probably doesn't
				// have the product on the database yet.
				else {
					// if we haven't a previous processed, and the new processed was null,
					// we don't have anything to give a trucada!
					Logging.printLogDebug(logger, session, "New processed product is null, and don't have a previous processed. Exiting processProduct method...");
					return;
				}
			}


			else { // we already have a processed product, so we must decide if we update 

				if (newProcessedProduct != null) {

					// the two processed are different, so we must enter in truco mode
					if ( compare(previousProcessedProduct, newProcessedProduct) ) {
						mustEnterTrucoMode = true;
						Logging.printLogDebug(logger, session, "Must enter in truco mode.");
					}

					// the two processed are equals, so we can update it
					else {
						
						// get the id of the processed product on database
						// if it was only updated it will be the id of the previous existent processed product
						// if a new processed was created, it will be the id generated by the
						// this id will be added to the found_products field on the task document on Mongo
						Long id = Persistence.persistProcessedProduct(newProcessedProduct, session);
						if (id != null) {
							Persistence.insertProcessedIdOnMongo(session, Main.dbManager.mongoBackendPanel);
							Persistence.appendProcessedIdOnMongo(id, session, Main.dbManager.mongoBackendPanel);
						}
						return;
					}
				}

			}

			// truco!
			if (mustEnterTrucoMode) {
				Logging.printLogDebug(logger, session, "Entering truco mode...");
				truco(newProcessedProduct, previousProcessedProduct);
			}
		}
	}


	/**
	 * It defines wether the crawler must true to extract data or not.
	 * @return
	 */
	public boolean shouldVisit() {
		return true;
	}

	/**
	 * By default this method does nothing.
	 * If the crawler needs to set some cookie to fetch the sku page,
	 * then it must implement this method.
	 */
	public void handleCookiesBeforeFetch() {

	}

	/**
	 * Performs any desired transformation on the URL before the actual fetching.
	 * @param url the URL we want to modify
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
	public List<Product> extract() throws Exception {

		// handle cookie
		if (cookies.isEmpty()) {
			handleCookiesBeforeFetch();
		}

		// handle URL modifications
		String url = handleURLBeforeFetch(session.getUrl());
		session.setUrl(url);
		session.setOriginalURL(url);

		if ( shouldVisit() ) {
			Document document = fetch();
			List<Product> products = null;
			products = extractInformation(document);
			if (products == null) products = new ArrayList<Product>();

			return products;
		}

		return new ArrayList<Product>();
	}


	/**
	 * Contains all the logic to sku information extraction.
	 * Must be implemented on subclasses.
	 * By default, returns an empty product.
	 * @param document
	 * @return A product with all it's crawled informations
	 */
	public List<Product> extractInformation(Document document) throws Exception {
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
	 * Obs: this method expects that both p1 and p2 are different from null.
	 * According to the method processProduct, this never occurs when the
	 * compare(p1, p2) method is called.
	 * 
	 * @param p1
	 * @param p2
	 * @return true if they are different or false otherwise
	 */
	private boolean compare(ProcessedModel p1, ProcessedModel p2) {
		return p1.compareHugeChanges(p2, session);
	}

	private void printCrawledInformation(Product product) {
		Logging.printLogDebug(logger, session, "Crawled information: " + product.toString());
	}

	/**
	 * Get only the product with the desired internalId.
	 * @param products
	 * @param internalId
	 * @return The product with the desired internal id, or an empty product if it was not found.
	 */
	private Product filter(List<Product> products, String internalId) {
		for (Product product : products) {
			if (product.getInternalId() != null && product.getInternalId().equals(internalId)) {
				return product;
			}
		}
		
		Logging.printLogDebug(logger, session, "Product with internalId " + internalId + " was not found.");
		return new Product();
	}

	/**
	 * This method performs an active analysis of the void status. 
	 * @param product the crawled product
	 * @return The resultant product from the analysis
	 */
	private Product activeVoid(Product product) throws Exception {
		
		// fetch the previous processed product
		// if a processed already exists and is void, then
		// we won't perform new attempts to extract the current product
		ProcessedModel previousProcessedProduct = Processor.fetchPreviousProcessed(product, session);
		if (previousProcessedProduct != null) {
			if (previousProcessedProduct.getVoid()) {
				return product;
			}
		}
		
		Logging.printLogDebug(logger, session, "Starting active void attempts...");
		
		// starting the active void iterations
		// until a maximum number of attempts, we will rerun the extract
		// method and check if the newly extracted product is void
		// in case it isn't, the loop interrupts and returns the product
		// when attempts reach it's maximum, we interrupt the loop and return the last extracted
		// product, even if it's void
		Product currentProduct = product;
		while (true) {
			session.incrementVoidAttemptsCounter();

			Logging.printLogDebug(logger, session, "[ACTIVE_VOID_ATTEMPT]" + session.getVoidAttempts());
			List<Product> products = extract();
			Logging.printLogDebug(logger, session, "Number of crawled products: " + products.size());
			currentProduct = filter(products, session.getInternalId());
			
			if (session.getVoidAttempts() >= MAX_VOID_ATTEMPTS || !currentProduct.isVoid()) break;
		}
		
		// if we ended with a void product after all the attempts
		// we must set void status of the existent processed product to true
		if (currentProduct.isVoid()) {
			Logging.printLogDebug(logger, session, "Product is void.");

			// set previous processed as void
			if (previousProcessedProduct != null && previousProcessedProduct.getVoid() == false) {
				Logging.printLogDebug(logger, session, "Setting previous processed void status to true...");
				Persistence.setProcessedVoidTrue(previousProcessedProduct, session);
			}
		}
		
		return currentProduct;

	}

	/**
	 * Run the 'truco' attempts.
	 * Before entering in this method, the two parameters newProcessed and previousProcessed where compared.
	 *  
	 * @param newProcessed the initial processed model, from which we will start the iteration
	 * @param previousProcessed the processed model that already exists in database
	 */
	private void truco(ProcessedModel newProcessed, ProcessedModel previousProcessed) throws Exception {
		ProcessedModel currentTruco = newProcessed;
		ProcessedModel next;

		while (true) {
			session.incrementTrucoAttemptsCounter();

			List<Product> products = extract();

			/*
			 * when we are processing all the the products in array (mode discovery)
			 * we will select only the product being 'trucado'
			 */
			Product localProduct = filter(products, currentTruco.getInternalId());

			// proceed the iteration only if the product is not void
			if (localProduct != null && !localProduct.isVoid()) {
				Persistence.persistProduct(localProduct, session);

				next = Processor.createProcessed(localProduct, session, previousProcessed, Main.processorResultManager);

				if (next != null) {					
					if ( compare(next, currentTruco) ) {
						currentTruco = next;	
					} 

					// we found two consecutive equals processed products, persist and end 
					else {
						Long id = Persistence.persistProcessedProduct(next, session);
						if (id != null) {
							Persistence.insertProcessedIdOnMongo(session, Main.dbManager.mongoBackendPanel);
							Persistence.appendProcessedIdOnMongo(id, session, Main.dbManager.mongoBackendPanel);
						}

						// upload screenshot to Amazon
						//saveScreenshot();
						
						// upload html to Amazon
						//saveHtml();

						return;
					}
				}
			}

			if (session.getTrucoAttempts() >= MAX_TRUCO_ATTEMPTS) {
				Logging.printLogDebug(logger, session, "Ended truco session but will not persist the product.");

				// register business logic error on session
				CrawlerSessionError error = new CrawlerSessionError(CrawlerSessionError.BUSINESS_LOGIC, "Ended truco session but will not persist the product.");
				session.registerError(error);

				// if we end up with a void at end of truco, we must change the status of the processed to void
				//					if (localProduct.isVoid()) {
				//						if (previousProcessedProduct != null && previousProcessedProduct.getVoid() == false) {
				//							Logging.printLogDebug(logger, session, "Seting previous processed void to true");
				//							Persistence.updateProcessedVoid(previousProcessedProduct, true, session);
				//						}
				//					}

				break;
			}

		}
	}

	/**
	 * Performs all finalization routines freeing
	 * any necessary elements
	 */
	private void terminate() {

		// if the crawler used the webdriver at some point, it must be closed
		if (webdriver != null) {
			Logging.printLogDebug(logger, session, "Closing webdriver...");
			webdriver.closeDriver();
		}
	}

	/**
	 * Save page screenshot on Amazon, using webdriver
	 */
	private void saveScreenshot() {
		
		// create webdriver
		if (webdriver == null) {
			Logging.printLogDebug(logger, session, "Initializing webdriver");
			webdriver = new CrawlerWebdriver();
		}

		// get a screenshot from the page
		File screenshot = webdriver.takeScreenshot(session.getUrl());
		
		// upload screenshot to Amazon
		S3Service.uploadFileToAmazon(session, screenshot);

	}

}

