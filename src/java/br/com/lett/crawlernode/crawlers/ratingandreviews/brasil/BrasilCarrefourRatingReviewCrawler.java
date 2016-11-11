package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.jsoup.nodes.Document;


import br.com.lett.crawlernode.core.crawler.RatingReviewCrawler;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;

public class BrasilCarrefourRatingReviewCrawler extends RatingReviewCrawler {

	public BrasilCarrefourRatingReviewCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractRatingAndReviews(Document document) throws Exception {
		Logging.printLogDebug(logger, session, "Running extractRatingAndReviews() implementation from " + this.getClass().getSimpleName());
	}

}
