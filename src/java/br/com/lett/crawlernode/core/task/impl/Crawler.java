package br.com.lett.crawlernode.core.task.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import br.com.lett.crawlernode.core.fetcher.CrawlerWebdriver;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.Fetcher;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionError;
import br.com.lett.crawlernode.core.session.DiscoveryCrawlerSession;
import br.com.lett.crawlernode.core.session.InsightsCrawlerSession;
import br.com.lett.crawlernode.core.session.SeedCrawlerSession;
import br.com.lett.crawlernode.core.session.TestCrawlerSession;
import br.com.lett.crawlernode.core.task.Scheduler;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.core.task.config.CrawlerConfig;
import br.com.lett.crawlernode.database.Persistence;
import br.com.lett.crawlernode.database.PersistenceResult;
import br.com.lett.crawlernode.database.ProcessedModelPersistenceResult;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.processor.base.Processor;
import br.com.lett.crawlernode.processor.models.ProcessedModel;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

import org.apache.http.cookie.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Crawler superclass. All crawler tasks must extend this class to override both
 * the shouldVisit and extract methods.
 * 
 * @author Samir Leao
 *
 */

public class Crawler extends Task {

	protected static final Logger logger = LoggerFactory.getLogger(Crawler.class);

	protected static final Pattern FILTERS = Pattern.compile
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

	protected CrawlerConfig config;

	protected CrawlerWebdriver webdriver;

	/**
	 * Cookies that must be used to fetch the sku page
	 * this attribute is set by the handleCookiesBeforeFetch method.
	 */
	protected List<Cookie> cookies;


	public Crawler(Session session) {
		this.session = session;
		this.cookies = new ArrayList<>();

		createDefaultConfig();
	}

	/**
	 * Create the config with default values
	 */
	private void createDefaultConfig() {
		this.config = new CrawlerConfig();
		this.config.setFetcher(Fetcher.STATIC);
		this.config.setProxyList(new ArrayList<String>());
		this.config.setConnectionAttempts(0);
	}

	/**
	 * Overrides the run method that will perform a task within a thread.
	 * The actual thread performs it's computation controlled by an Executor, from
	 * Java's Executors Framework.
	 */
	@Override 
	public void processTask() {
		if (session instanceof TestCrawlerSession) {
			testRun();
		}
		else {
			productionRun();
		}
	}
	
	@Override
	public void onStart() {
		Logging.printLogDebug(logger, session, "START");
	}

	@Override
	public void onFinish() {
		
		// close the webdriver
		if (webdriver != null ) {
			webdriver.terminate();
		}
		
		List<SessionError> errors = session.getErrors();

		Logging.printLogDebug(logger, session, "Finalizing session of type [" + session.getClass().getSimpleName() + "]");

		// errors collected manually
		// they can be exceptions or business logic errors
		// and are all gathered inside the session
		if (!errors.isEmpty()) {
			Logging.printLogError(logger, session, "Task failed [" + session.getOriginalURL() + "]");

			Persistence.setTaskStatusOnMongo(Persistence.MONGO_TASK_STATUS_FAILED, session, Main.dbManager.mongoBackendPanel);
			
			session.setTaskStatus(Task.STATUS_FAILED);
		}

		// only remove the task from queue if it was flawless
		// and if we are not testing, because when testing there is no message processing
		else if (session instanceof InsightsCrawlerSession || session instanceof SeedCrawlerSession || session instanceof DiscoveryCrawlerSession) {
			Logging.printLogDebug(logger, session, "Task completed.");
				
			//TODO enviar reposta OK para o daemon
			
			Persistence.setTaskStatusOnMongo(Persistence.MONGO_TASK_STATUS_DONE, session, Main.dbManager.mongoBackendPanel);
			
			session.setTaskStatus(Task.STATUS_COMPLETED);
		}

		// only print statistics of void and truco if we are running an Insights session crawling
		if (session instanceof InsightsCrawlerSession) {
			Logging.printLogDebug(logger, session, "[ACTIVE_VOID_ATTEMPTS]" + session.getVoidAttempts());
			Logging.printLogDebug(logger, session, "[TRUCO_ATTEMPTS]" + session.getTrucoAttempts());
		}

		Logging.printLogDebug(logger, session, "END");
	}

	private void productionRun() {
		// crawl informations and create a list of products
		List<Product> products = null;
		try {
			products = extract();
		} catch (Exception e) {
			Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
			products = new ArrayList<>();
		}

		Logging.printLogDebug(logger, session, "Number of crawled products: " + products.size());

		// print crawled information
		for (Product product : products) {
			printCrawledInformation(product);
		}

		// insights session
		// there is only one product that will be selected
		// by it's internalId, passed by the crawler session
		if (session instanceof InsightsCrawlerSession) {

			// get crawled product by it's internalId
			Logging.printLogDebug(logger, session, "Selecting product with internalId " + ((InsightsCrawlerSession)session).getInternalId());
			Product crawledProduct = filter(products, ((InsightsCrawlerSession)session).getInternalId());

			// if the product is void run the active void analysis
			Product activeVoidResultProduct = crawledProduct;
			if (crawledProduct.isVoid()) {
				Logging.printLogDebug(logger, session, "Product is void...going to start the active void.");
				try {
					activeVoidResultProduct = activeVoid(crawledProduct);
				} catch (Exception e) {
					Logging.printLogError(logger, session, "Error in active void method.");
					SessionError error = new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTrace(e));
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

					SessionError error = new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTrace(e));
					session.registerError(error);
				}
			}

		}

		// discovery session
		// when processing a task of a suggested URL by the webcrawler or
		// an URL scheduled manually, we won't run active void and 
		// we must process each crawled product
		else if (session instanceof DiscoveryCrawlerSession || session instanceof SeedCrawlerSession) {
			Logging.printLogDebug(logger, session, "Processing session of type: " + session.getClass().getName());

			for (Product product : products) {
				try {
					processProduct(product);
				} catch (Exception e) {
					SessionError error = new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTrace(e));
					session.registerError(error);
				}
			}
		}
	}

	/**
	 * Run method to be used when testing
	 */
	private void testRun() {

		// crawl informations and create a list of products
		List<Product> products = null;
		try {
			products = extract();
		} catch (Exception e) {
			Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
			products = new ArrayList<>();
		}

		Logging.printLogDebug(logger, session, "Number of crawled products: " + products.size());

		for (Product p : products) {
			printCrawledInformation(p);
		}
	}

	/**
	 * This method is responsible for the main post processing stages of a crawled product.
	 * It takes care of the following tasks
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

		// persist the product
		Persistence.persistProduct(product, session);

		// fetch the previous processed product stored on database
		ProcessedModel previousProcessedProduct = Processor.fetchPreviousProcessed(product, session);

		if ( (previousProcessedProduct == null && 
				(session instanceof DiscoveryCrawlerSession || session instanceof SeedCrawlerSession)) 
				||
				previousProcessedProduct != null) 
		{

			// create the new processed product
			ProcessedModel newProcessedProduct = Processor.createProcessed(product, session, previousProcessedProduct, Main.processorResultManager);

			// the product doesn't exists yet
			if (previousProcessedProduct == null) { 

				// if a new processed product was created
				if (newProcessedProduct != null) {

					// persist the new created processed product
					PersistenceResult persistenceResult = Persistence.persistProcessedProduct(newProcessedProduct, session);
					processPersistenceResult(persistenceResult);
					scheduleImages(persistenceResult, newProcessedProduct);

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
						PersistenceResult persistenceResult = Persistence.persistProcessedProduct(newProcessedProduct, session);
						processPersistenceResult(persistenceResult);
						scheduleImages(persistenceResult, newProcessedProduct);

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
	 * 
	 * @param persistenceResult
	 */
	private void processPersistenceResult(PersistenceResult persistenceResult) {
		Long createdId = null;
		Long modifiedId = null;
		if (persistenceResult instanceof ProcessedModelPersistenceResult) {
			createdId = ((ProcessedModelPersistenceResult) persistenceResult).getCreatedId();
			modifiedId = ((ProcessedModelPersistenceResult) persistenceResult).getModifiedId();
		}

		if (createdId != null) {
			Persistence.appendProcessedIdOnMongo(createdId, session, Main.dbManager.mongoBackendPanel);
			Persistence.appendCreatedProcessedIdOnMongo(createdId, session, Main.dbManager.mongoBackendPanel);
		}
		else if (modifiedId != null) {
			Persistence.appendProcessedIdOnMongo(modifiedId, session, Main.dbManager.mongoBackendPanel);
		}
	}


	private void scheduleImages(PersistenceResult persistenceResult, ProcessedModel processed) {
		Long createdId = null;
		if (persistenceResult instanceof ProcessedModelPersistenceResult) {
			createdId = ((ProcessedModelPersistenceResult) persistenceResult).getCreatedId();
		}

		if (createdId != null) {
			Logging.printLogDebug(logger, session, "Scheduling images download tasks...");
			Scheduler.scheduleImages(session, Main.queueHandler, processed, createdId);
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
	 * Set cookies before page fetching
	 */
	public void handleCookiesBeforeFetch() {
		/* subclasses must implement */
	}

	/**
	 * Performs any desired transformation on the URL before the actual fetching.
	 * 
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

		// in cases we are running a truco iteration
		if (webdriver != null) {
			webdriver.terminate();
		}

		// handle cookie
		if (cookies.isEmpty()) {
			handleCookiesBeforeFetch();
		}

		// handle URL modifications
		String url = handleURLBeforeFetch(session.getOriginalURL());
		session.setOriginalURL(url);

		Document document = fetch();
		List<Product> products;
		products = extractInformation(document);
		if (products == null) {
			products = new ArrayList<>();
		}

		return products;
	}


	/**
	 * Contains all the logic to sku information extraction.
	 * Must be implemented on subclasses.
	 * 
	 * @param document
	 * @return A product with all it's crawled informations
	 */
	public List<Product> extractInformation(Document document) throws Exception {
		return new ArrayList<>();
	}

	/**
	 * Request the sku URL and parse to a DOM format.
	 * This method uses the preferred fetcher according to the crawler configuration.
	 * If the fetcher is static, then we use de StaticDataFetcher, otherwise we use the
	 * DynamicDataFetcher.
	 * 
	 * @return Parsed HTML in form of a Document.
	 */
	private Document fetch() {
		String html;
		if (config.getFetcher() == Fetcher.STATIC) {
			html = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, session.getOriginalURL(), null, cookies);
		} else if (config.getFetcher() == Fetcher.SMART) {
			html = DynamicDataFetcher.fetchPageSmart(session.getOriginalURL(), session);
		} else {
			webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), session);
			html = webdriver.getCurrentPageSource();
		}

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
	 * 
	 * @param products
	 * @param internalId
	 * @return The product with the desired internal id, or an empty product if it was not found.
	 */
	private Product filter(List<Product> products, String desiredInternalId) {
		Logging.printLogDebug(logger, session, "Desired internalId " + desiredInternalId);
		for (Product product : products) {
			String crawledInternalId = product.getInternalId();
			if (crawledInternalId != null && crawledInternalId.equals(desiredInternalId)) {
				return product;
			}
		}

		Logging.printLogDebug(logger, session, "Product with internalId " + desiredInternalId + " was not found...geting an empty product.");
		return new Product();
	}

	/**
	 * This method performs an active analysis of the void status.
	 *  
	 * @param product the crawled product
	 * @return The resultant product from the analysis
	 */
	private Product activeVoid(Product product) throws Exception {

		// fetch the previous processed product
		// if a processed already exists and is void, then
		// we won't perform new attempts to extract the current product
		ProcessedModel previousProcessedProduct = Processor.fetchPreviousProcessed(product, session);
		if (previousProcessedProduct != null && previousProcessedProduct.isVoid()) {
			Logging.printLogDebug(logger, session, "The previous processed is void. Returning...");
			
			return product;
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
			currentProduct = filter(products, ((InsightsCrawlerSession)session).getInternalId());

			if (session.getVoidAttempts() >= MAX_VOID_ATTEMPTS || !currentProduct.isVoid()) {
				break;
			}
		}

		// if we ended with a void product after all the attempts
		// we must set void status of the existent processed product to true
		if (currentProduct.isVoid()) {
			Logging.printLogDebug(logger, session, "Product is void.");

			// set previous processed as void
			if (previousProcessedProduct != null && !previousProcessedProduct.isVoid()) {
				Logging.printLogDebug(logger, session, "Setting previous processed void status to true...");
				Persistence.setProcessedVoidTrue(session);
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
						Persistence.insertProcessedIdOnMongo(session, Main.dbManager.mongoBackendPanel);

						PersistenceResult persistenceResult = Persistence.persistProcessedProduct(next, session);
						processPersistenceResult(persistenceResult);
						scheduleImages(persistenceResult, next);

						return;
					}
				}
			}

			if (session.getTrucoAttempts() >= MAX_TRUCO_ATTEMPTS) {
				Logging.printLogDebug(logger, session, "Ended truco session but will not persist the product.");

				// register business logic error on session
				SessionError error = new SessionError(SessionError.BUSINESS_LOGIC, "Ended truco session but will not persist the product.");
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

}

