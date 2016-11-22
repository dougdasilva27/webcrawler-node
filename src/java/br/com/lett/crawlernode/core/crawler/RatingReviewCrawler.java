package br.com.lett.crawlernode.core.crawler;

import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.models.RatingsReviews;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionError;
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
			
			// get only the desired rating and review, according to the internal id
			RatingsReviews ratingReviews = ratingReviewsCollection.getRatingReviews(session.getInternalId());
			
			if (ratingReviews != null) {
				printRatingsReviews(ratingReviews);
			} else {
				Logging.printLogError(logger, session, "Rating and reviews for internalId " + session.getInternalId() + " was not crawled.");
			}
			
		} catch (Exception e) {
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
			session.registerError(new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e)));
		}
	}
	
	protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
		/* subclasses must implement */
		return new RatingReviewsCollection();
	}
	
	private void printRatingsReviews(RatingsReviews ratingReviews) {
		Logging.printLogDebug(logger, session, ratingReviews.toString());
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
