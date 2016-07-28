package br.com.lett.crawlernode.base;

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
import br.com.lett.crawlernode.util.Logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Crawler superclass. All crawler tasks must extend this class to override both
 * the shouldVisit and extract methods.
 * 
 * @author Samir Leão
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


	public Crawler(CrawlerSession session) {
		this.session = session;
	}
	
	@Override
	public void run() {
		//Product product = extract();
		Logging.printLogDebug(logger, session, "Processing task: " + session.getUrl());
		
		Logging.printLogDebug(logger, session, "Apagando task: " + session.getOriginalURL() + "...");
		QueueService.deleteMessage(Main.queue, session.getSessionId(), session.getMessageReceiptHandle());
	}

//	@Override 
//	public void run() {
//
//		/*
//		 * Initial iteration
//		 */
//
//		boolean mustEnterTrucoMode = false;
//
//		// crawl informations and create a product
//		Product product = extract();
//
//		// persist the product
//		Persistence.persistProduct(product, session);
//
//		// fetch the previous processed product stored on database
//		ProcessedModel previousProcessedProduct = Processor.fetchPreviousProcessed(product, session);
//
//		// create the new processed product
//		ProcessedModel newProcessedProduct = Processor.createProcessed(product, session, previousProcessedProduct);
//
//		if (previousProcessedProduct == null) {
//
//			// if a new processed product was created
//			if (newProcessedProduct != null) {
//				Persistence.persistProcessedProduct(newProcessedProduct, session);
//			} else {
//				// indicates we had some invalid information crawled
//			}
//		}
//
//
//		else { // we already have a processed product, so we must decide if we update 
//
//			if (newProcessedProduct != null) {
//
//				// the two processed are different, so we must enter in truco mode
//				if ( compare(newProcessedProduct, previousProcessedProduct) ) {
//					mustEnterTrucoMode = true;
//				}
//
//				// the two processed are equals, so we can update it
//				else {
//					Persistence.persistProcessedProduct(newProcessedProduct, session);
//					return;
//				}
//			}
//
//		}
//
//		/*
//		 * Running truco
//		 */
//		if (mustEnterTrucoMode) {
//			ProcessedModel currentTruco = newProcessedProduct;
//			
//			while (true) {
//				product = extract();
//				Persistence.persistProduct(product, session);
//				newProcessedProduct = Processor.createProcessed(product, session, previousProcessedProduct);
//				session.incrementTrucoAttempts();
//				
//				if (newProcessedProduct != null) {					
//					if ( compare(newProcessedProduct, currentTruco) ) {
//						currentTruco = newProcessedProduct;	
//					} 
//					
//					// if we found two consecutive equals processed products, persist and end 
//					else {
//						Persistence.persistProcessedProduct(newProcessedProduct, session);
//						return;
//					}
//				}
//				
//				if (session.getTrucoAttempts() >= MAX_TRUCO_ATTEMPTS) break;
//			}
//		}
//
//
//	}

	/**
	 * It defines wether the crawler must true to extract data or not
	 * 
	 * @param url
	 * @return
	 */
	public boolean shouldVisit() {
		return true;
	}

	public Product extract() {
//		if ( shouldVisit() ) {
//			Document document = preProcessing();
//			return extractInformation(document);
//		}

		return new Product();
	}


	public Product extractInformation(Document document) {
		/*
		 * Return an empty Product by default. Will be implemented on subclasses.
		 */

		return new Product();
	}

	/**
	 * Request the sku URL and parse to a DOM format
	 * 
	 * @return parsed HTML in form of a Document
	 */
	private Document preProcessing() {
		String html = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, null, null);
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

}
