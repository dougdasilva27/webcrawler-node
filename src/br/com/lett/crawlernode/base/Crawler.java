package br.com.lett.crawlernode.base;

import java.sql.SQLException;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import br.com.lett.crawlernode.database.Persistence;
import br.com.lett.crawlernode.fetcher.DataFetcher;
import br.com.lett.crawlernode.models.CrawlerSession;
import br.com.lett.crawlernode.models.ProcessedModel;
import br.com.lett.crawlernode.models.Product;
import br.com.lett.crawlernode.processor.base.Processor;
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

		ProcessedModel newProcessedProduct = null;

		while (true) {

			// crawl informations and create a product
			Product product = extract();

			// persist the product
			Persistence.persistProduct(product, session);

			// fetch the previous processed product stored on database
			ProcessedModel previousProcessedProduct = Processor.fetchPreviousProcessed(product, session);

			// create the new processed product
			newProcessedProduct = Processor.createProcessed(product, session, previousProcessedProduct);

			// if truco was valid and performed some verification
			if ( !performTruco(product, newProcessedProduct, previousProcessedProduct, session) ) break; 

		}

		// persist the new processed product
		if (newProcessedProduct != null) {
			Persistence.persistProcessedProduct(newProcessedProduct, session);
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

	public Product extract() {
		if ( shouldVisit() ) {
			Document document = preProcessing();
			return extractInformation(document);
		}

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
	 * 
	 * @param product
	 * @param newProcessedProduct
	 * @param actualProcessedProduct
	 * @param session
	 * @return
	 */
	private boolean performTruco(Product product, ProcessedModel newProcessedProduct, ProcessedModel actualProcessedProduct, CrawlerSession session) {
		ProcessedModel truco = session.getTruco();		

		// validating truco mode
		if(truco != null) {
			if(session.getOriginalURL() == null) { // TODO conferir o que seria essa originalURL
				Logging.printLogError(logger, "Erro tentando começar modo truco sem enviar a originalUrl");
				return false;
			}

			if(truco.getInternalId() == null) {
				Logging.printLogError(logger, "Error: truco with null internalId");
				return false;
			}

			// Se estou no modo truco mas o produto sendo verificado não é o mesmo,
			// que acontece no caso de URLs com múltiplos produtos, então paro por aqui.
			if(!truco.getInternalId().equals(product.getInternalId())) {
				Logging.printLogDebug(logger, "Abortando este modo truco pois estou trucando outro produto.");
				return false;
			}

			// see if we reached maximum number of attempts in truco mode
			if (session.getTrucoAttempts() >= MAX_TRUCO_ATTEMPTS) {
				return false;
			}

			// increment truco attempts counter
			session.incrementTrucoAttempts();

			return mustCheckAgain(actualProcessedProduct, newProcessedProduct, session);

		}

		return false;		
	}

	private boolean mustCheckAgain(ProcessedModel actualProcessedProduct, ProcessedModel newProcessedProduct, CrawlerSession session) {
		ProcessedModel truco = session.getTruco();
		boolean mustCheckAgain = false;

		if(actualProcessedProduct != null || truco != null) {
			if(truco != null) {
				mustCheckAgain = newProcessedProduct.compareHugeChanges(truco);
			} else {
				mustCheckAgain = newProcessedProduct.compareHugeChanges(actualProcessedProduct);
			}
		} else {
			mustCheckAgain = false;
		}

		return mustCheckAgain;
	}



}
