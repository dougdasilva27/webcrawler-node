package br.com.lett.crawlernode.kernel.task;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import br.com.lett.crawlernode.database.Persistence;
import br.com.lett.crawlernode.kernel.fetcher.CrawlerWebdriver;
import br.com.lett.crawlernode.kernel.fetcher.DataFetcher;
import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.processor.base.Processor;
import br.com.lett.crawlernode.processor.models.ProcessedModel;
import br.com.lett.crawlernode.server.QueueService;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

import org.apache.http.cookie.Cookie;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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

	protected static final int MAX_VOID_ATTEMPTS = 3;
	protected static final int MAX_TRUCO_ATTEMPTS = 3;

	protected final static Pattern FILTERS = Pattern.compile(
			".*(\\.(css|js|bmp|gif|jpe?g"
					+ "|png|ico|tiff?|mid|mp2|mp3|mp4"
					+ "|wav|avi|mov|mpeg|ram|m4v|pdf" 
					+ "|rm|smil|wmv|swf|wma|zip|rar|gz))(\\?.*)?$"
			);

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

		// crawl informations and create a list of products
		List<Product> products = extract();

		Logging.printLogDebug(logger, session, "Number of crawled products: " + products.size());

		/* 
		 * MODE INSIGHTS							
		 * There is only one product that will be 					
		 * processed in this mode. This product will be selected 	
		 * by it's internalId, passed by the CrawlerSession																
		 */
		if (session.getType().equals(CrawlerSession.INSIGHTS_TYPE)) {

			// get crawled product by it's internalId
			Product crawledProduct = getProductByInternalId(products, session.getInternalId());

			// run active void status analysis
			Product finalProduct = activeVoid(crawledProduct);

			if (finalProduct.isVoid()) {
				Logging.printLogDebug(logger, session, "Product is void.");

				// set previous processed as void
				ProcessedModel previousProcessedProduct = Processor.fetchPreviousProcessed(finalProduct, session);
				if (previousProcessedProduct != null && previousProcessedProduct.getVoid() == false) {
					Logging.printLogDebug(logger, session, "Setting previous processed void status to true.");
					Persistence.updateProcessedVoid(previousProcessedProduct, true, session);
				}
			} 

			// process the resulting product after active void analysis
			else {
				processProduct(finalProduct);
			}

		}


		/* 
		 * MODE DISCOVERY												
		 * In this mode, we must process each crawled product.  												
		 */
		else {
			for (Product product : products) {
				processProduct(product);
			}
		}

		// if the crawler used the webdriver at some point, it must be closed
		if (webdriver != null) {
			Logging.printLogDebug(logger, session, "Closing webdriver...");
			webdriver.closeDriver();
		}

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
		ProcessedModel newProcessedProduct = null;
		if (session.getType().equals(CrawlerSession.TEST_TYPE)) {
			newProcessedProduct = Processor.createProcessed(product, session, previousProcessedProduct, br.com.lett.crawlernode.test.Tester.processorResultManager);
		} else {
			newProcessedProduct = Processor.createProcessed(product, session, previousProcessedProduct, Main.processorResultManager);
		}

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
				if ( compare(previousProcessedProduct, newProcessedProduct) ) {
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
		 * Inside the truco iterations we also keep track if the product is void
		 * If at the end of truco attempts we end up with a void product, we
		 * must change the status of the processed product to void in database.
		 */
		if (mustEnterTrucoMode) {
			Logging.printLogDebug(logger, session, "Entering truco mode...");

			ProcessedModel currentTruco = newProcessedProduct;

			while (true) {
				session.incrementTrucoAttemptsCounter();

				List<Product> products = extract();

				/*
				 * when we are processing all the the products in array (mode discovery)
				 * we will select only the product being 'trucado'
				 */
				Product localProduct = getProductByInternalId(products, currentTruco.getInternalId());

				// proceed the iteration only if the product is not void
				if (!localProduct.isVoid()) {
					Persistence.persistProduct(localProduct, session);

					if (session.getType().equals(CrawlerSession.TEST_TYPE)) {
						newProcessedProduct = Processor.createProcessed(localProduct, session, previousProcessedProduct, br.com.lett.crawlernode.test.Tester.processorResultManager);
					} else {
						newProcessedProduct = Processor.createProcessed(localProduct, session, previousProcessedProduct, Main.processorResultManager);
					}

					if (newProcessedProduct != null) {					
						if ( compare(newProcessedProduct, currentTruco) ) {
							currentTruco = newProcessedProduct;	
						} 

						// we found two consecutive equals processed products, persist and end 
						else {
							Persistence.persistProcessedProduct(newProcessedProduct, session);

							//							// create webdriver
							//							if (webdriver == null) {
							//								Logging.printLogDebug(logger, session, "Initializing webdriver");
							//								webdriver = new CrawlerWebdriver();
							//							}
							//							
							//							// get a screenshot from the page
							//							File screenshot = webdriver.takeScreenshot(session.getUrl());
							//							
							//							// the the page html
							//							String html = webdriver.loadUrl(session.getUrl());
							//							
							//							// upload screenshot and html to Amazon
							//							S3Service.uploadFileToAmazon(session, screenshot);
							//							S3Service.uploadHtmlToAmazon(session, html);

							return;
						}
					}
				}

				if (session.getTrucoAttempts() >= MAX_TRUCO_ATTEMPTS) {
					
					Logging.printLogDebug(logger, session, "Ended truco session but will not persist the product.");

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


	/**
	 * It defines wether the crawler must true to extract data or not.
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
	public List<Product> extract() {

		// handle cookie
		handleCookiesBeforeFetch();


		// handle URL modifications
		String url = handleURLBeforeFetch(session.getUrl());
		session.setUrl(url);
		session.setOriginalURL(url);

		if ( shouldVisit() ) {
			Document document = fetch();
			List<Product> products = null;
			try {
				products = extractInformation(document);
			} catch (Exception e) {
				Logging.printLogError(logger, session, "Error during extraction.");
				Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
			}
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
	private Product getProductByInternalId(List<Product> products, String internalId) {
		for (Product product : products) {
			if (product.getInternalId() != null && product.getInternalId().equals(internalId)) {
				return product;
			}
		}

		return new Product();
	}

	/**
	 * This method performs an active analysis of the void status. 
	 * @param product the crawled product
	 * @return The resultant product from the analysis
	 */
	private Product activeVoid(Product product) {

		/*
		 * There are two cases in which we return the original product.
		 * The first case is if it's not void.
		 * The second is if it is void, and there is a previous processed product 
		 * with the same internalId that was also void.
		 */
		if (!product.isVoid()) return product;

		ProcessedModel previousProcessedProduct = Processor.fetchPreviousProcessed(product, session);
		if (previousProcessedProduct != null) {
			if (previousProcessedProduct.getVoid()) return product;
		}

		Product currentProduct = product;
		while (true) {
			if (session.getVoidAttempts() >= MAX_VOID_ATTEMPTS || !currentProduct.isVoid()) break;
			session.incrementVoidAttemptsCounter();

			Logging.printLogDebug(logger, session, "[ACTIVE_VOID_ATTEMPT]" + session.getVoidAttempts());
			List<Product> products = extract();
			currentProduct = getProductByInternalId(products, session.getInternalId());

		}

		return currentProduct;
	}

}

