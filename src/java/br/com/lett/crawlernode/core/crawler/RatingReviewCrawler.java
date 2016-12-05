package br.com.lett.crawlernode.core.crawler;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.cookie.Cookie;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.models.RatingsReviews;
import br.com.lett.crawlernode.core.session.RatingReviewsCrawlerSession;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionError;
import br.com.lett.crawlernode.database.Persistence;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class RatingReviewCrawler implements Runnable {
	
	protected static final Logger logger = LoggerFactory.getLogger(RatingReviewCrawler.class);
	
	protected Session session;
	
	/**
	 * Cookies that must be used to fetch the sku page
	 * this attribute is set by the handleCookiesBeforeFetch method.
	 */
	protected List<Cookie> cookies;
	
	
	public RatingReviewCrawler(Session session) {
		this.session = session;
		cookies = new ArrayList<>();
	}
	
	@Override
	public void run() {
		if (session instanceof RatingReviewsCrawlerSession) {
			runProduction();
		}
		else {
			runTest();
		}
	}
	
	public void runProduction() {
		if (cookies.isEmpty()) {
			handleCookiesBeforeFetch();
		}
		
		Document document = fetch();
		try {
			RatingReviewsCollection ratingReviewsCollection = extractRatingAndReviews(document);
			
			// get only the desired rating and review, according to the internal id
			RatingsReviews ratingReviews = ratingReviewsCollection.getRatingReviews(session.getInternalId());
			
			if (ratingReviews != null) {
				printRatingsReviews(ratingReviews);
				Persistence.updateRating(ratingReviews, session);
				
			} else {
				Logging.printLogError(logger, session, "Rating and reviews for internalId " + session.getInternalId() + " was not crawled.");
			}
			
		} catch (Exception e) {
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
			session.registerError(new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e)));
		}
	}
	
	public void runTest() {
		if (cookies.isEmpty()) {
			handleCookiesBeforeFetch();
		}
		
		Document document = fetch();
		try {
			RatingReviewsCollection ratingReviewsCollection = extractRatingAndReviews(document);
			
			for (RatingsReviews rating : ratingReviewsCollection.getRatingReviewsList()) {
				printRatingsReviews(rating);
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
	
	protected void handleCookiesBeforeFetch() {
		/* subclasses must implement */
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
		return DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, session.getOriginalURL(), null, cookies);	
	}

}
