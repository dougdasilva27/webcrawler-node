package br.com.lett.crawlernode.core.crawler;

import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class RatingReviewCrawler implements Runnable {
	
	protected static final Logger logger = LoggerFactory.getLogger(RatingReviewCrawler.class);
	
	protected Session session;
	
	public RatingReviewCrawler(Session session) {
		this.session = session;
	}
	
	public void run() {
		extract();
	}
	
	public void extract() {
		Document document = fetch();
		try {
			RatingReviewsCollection ratingReviewsCollection = extractRatingAndReviews(document);
		} catch (Exception e) {
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
		}
	}
	
	protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
		/* subclasses must implement */
		return new RatingReviewsCollection();
	}
	
	/**
	 * Request the sku URL and parse to a DOM format.
	 * 
	 * @return Parsed HTML in form of a Document
	 */
	private Document fetch() {
		return DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, session.getOriginalURL(), null, null);	
	}

}
