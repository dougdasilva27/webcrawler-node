package br.com.lett.crawlernode.core.task.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.cookie.Cookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.fetcher.CrawlerWebdriver;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.Fetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.models.RatingsReviews;
import br.com.lett.crawlernode.core.session.RatingReviewsCrawlerSession;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionError;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.core.task.config.RatingCrawlerConfig;
import br.com.lett.crawlernode.database.Persistence;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class RatingReviewCrawler extends Task {

	protected static final Logger logger = LoggerFactory.getLogger(RatingReviewCrawler.class);

	/**
	 * Cookies that must be used to fetch the sku page
	 * this attribute is set by the handleCookiesBeforeFetch method.
	 */
	protected List<Cookie> cookies;

	protected RatingCrawlerConfig config;

	protected CrawlerWebdriver webdriver;


	public RatingReviewCrawler(Session session) {
		this.session = session;
		cookies = new ArrayList<>();

		createDefaultConfig();
	}

	private void createDefaultConfig() {
		this.config = new RatingCrawlerConfig();
		this.config.setFetcher(Fetcher.STATIC);
		this.config.setProxyList(new ArrayList<String>());
		this.config.setConnectionAttempts(0);
	}

	@Override
	public void processTask() {
		if (session instanceof RatingReviewsCrawlerSession) {
			runProduction();
		}
		else {
			runTest();
		}
	}
	
	@Override
	public void onStart() {
		Logging.printLogDebug(logger, session, "START");
	}

	@Override
	public void onFinish() {
		List<SessionError> errors = session.getErrors();

		Logging.printLogDebug(logger, session, "Finalizing session of type [" + session.getClass().getSimpleName() + "]");

		// errors collected manually
		// they can be exceptions or business logic errors
		// and are all gathered inside the session
		if (!errors.isEmpty()) {
			Logging.printLogError(logger, session, "Task failed [" + session.getOriginalURL() + "]");
			session.setTaskStatus(Task.STATUS_FAILED);
		}

		else {

			// only remove the task from queue if it was flawless
			// and if we are not testing, because when testing there is no message processing
			Logging.printLogDebug(logger, session, "Task completed.");

			session.setTaskStatus(Task.STATUS_COMPLETED);
		}
		
		Logging.printLogDebug(logger, session, "END");
	}

	public void runProduction() {
		if (!cookies.isEmpty()) {
			handleCookiesBeforeFetch();
		}

		// apply URL modifications
		String modifiedURL = handleURLBeforeFetch(session.getOriginalURL());
		session.setOriginalURL(modifiedURL);

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

		// apply URL modifications
		String modifiedURL = handleURLBeforeFetch(session.getOriginalURL());
		session.setOriginalURL(modifiedURL);

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

	protected String handleURLBeforeFetch(String url) {
		/* subclasses must implement */
		return url;
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
		String html;
		if (this.config.getFetcher() == Fetcher.STATIC) {
			html = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, session.getOriginalURL(), null, cookies);
		} else if (this.config.getFetcher() == Fetcher.SMART) {
			html = DynamicDataFetcher.fetchPageSmart(session.getOriginalURL(), session);
		} else {
			this.webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), session);
			html = this.webdriver.getCurrentPageSource();
		}

		return Jsoup.parse(html);
	}

}
