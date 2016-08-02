package br.com.lett.crawlernode.base;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import br.com.lett.crawlernode.database.Persistence;
import br.com.lett.crawlernode.fetcher.DataFetcher;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.models.CrawlerSession;
import br.com.lett.crawlernode.models.ProcessedModel;
import br.com.lett.crawlernode.models.Product;
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
 * 
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
	 * the current crawling session
	 */
	protected CrawlerSession session;

	/**
	 * cookies that must be used to fetch the sku page
	 * this attribute is set by the handleCookiesBeforeFetch method
	 */
	protected List<Cookie> cookies;


	public Crawler(CrawlerSession session) {
		this.session = session;
	}

	//	@Override
	//	public void run() {
	//		//Product product = extract();
	//		Logging.printLogDebug(logger, session, "Processing task: " + session.getUrl());
	//		
	//		try {
	//			Thread.sleep(5000 + CommonMethods.randInt(1000, 6000));
	//		} catch (InterruptedException e) {
	//			Logging.printLogDebug(logger, session, "Error in thread sleep!");
	//			e.printStackTrace();
	//		}
	//		
	//		Logging.printLogDebug(logger, session, "Apagando task: " + session.getOriginalURL() + "...");
	//		
	//		QueueService.deleteMessage(Main.queue, session.getSessionId(), session.getMessageReceiptHandle());
	//	}

	@Override 
	public void run() {
		Logging.printLogDebug(logger, session, "START");

		// crawl informations and create a list of products
		List<Product> products = extract();
		
		/*
		 * MODE INSIGHTS
		 * 
		 * There is only one product that will be processed in this mode.
		 * This product will be selected by it's internalId, passed by the CrawlerSession
		 */
		if (Main.executionParameters.getMode().equals(ExecutionParameters.MODE_INSIGHTS)) {
			for (Product product : products) {
				if (product.getInternalId() != null && product.getInternalId().equals(session.getInternalId())) {
					processProduct(product);
				}
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
		
		try {
			Thread.sleep(15000 + CommonMethods.randInt(2000, 10000));
		} catch (InterruptedException e) {
			Logging.printLogDebug(logger, session, "Error in thread sleep!");
			e.printStackTrace();
		}

		Logging.printLogDebug(logger, session, "Deleting task: " + session.getUrl() + "...");

		QueueService.deleteMessage(Main.queue, session.getSessionId(), session.getMessageReceiptHandle());

		Logging.printLogDebug(logger, session, "END [trucos = " + session.getTrucoAttempts() + "]");

	}

	/**
	 * 
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
				// indicates we had some invalid information crawled
			}
		}


		else { // we already have a processed product, so we must decide if we update 

			if (newProcessedProduct != null) {

				// the two processed are different, so we must enter in truco mode
				if ( compare(newProcessedProduct, previousProcessedProduct) ) {
					mustEnterTrucoMode = true;
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
							return;
						}
					}
				}
				
				if (session.getTrucoAttempts() >= MAX_TRUCO_ATTEMPTS) break;
				
			}
		}
	}
	

	/**
	 * It defines wether the crawler must true to extract data or not
	 * 
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
	 * 
	 * @param url
	 * @return
	 */
	public String handleURLBeforeFetch(String url) {
		return url;
	}

	public List<Product> extract() {

		// handle cookie
		handleCookiesBeforeFetch();

		// handle URL modifications
		String url = handleURLBeforeFetch(session.getUrl());
		session.setUrl(url);
		session.setOriginalURL(url);

		if ( shouldVisit() ) {
			Document document = preProcessing();
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
	 * @return parsed HTML in form of a Document
	 */
	private Document preProcessing() {
		String html = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, null, cookies);
		return Jsoup.parse(html);		
	}

	/**
	 * Compare ProcessedModel p1 against p2
	 * p2 is suposed to be the truco, that is the model we are checking against
	 * 
	 * @return true if they are different or false otherwise
	 */
	private boolean compare(ProcessedModel p1, ProcessedModel p2) {
		return p1.compareHugeChanges(p2);
	}

	private void printCrawledInformation(Product product) {
		Logging.printLogDebug(logger, "Crawled information[session: " + session.getSessionId() + "]" + product.toString());
	}
	
	/**
	 * 
	 * @param products
	 * @param internalId
	 * @return
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
