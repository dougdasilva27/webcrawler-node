package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import org.jsoup.nodes.Document;


import br.com.lett.crawlernode.core.crawler.RatingReviewCrawler;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;

public class SaopauloShoptimeRatingReviewCrawler extends RatingReviewCrawler {

	public SaopauloShoptimeRatingReviewCrawler(Session session) {
		super(session);
	}

	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
		Logging.printLogDebug(logger, session, "Running extractRatingAndReviews() implementation from " + this.getClass().getSimpleName());
		
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

		return ratingReviewsCollection;
	}

}
