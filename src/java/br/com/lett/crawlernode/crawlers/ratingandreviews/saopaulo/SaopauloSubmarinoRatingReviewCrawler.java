package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import org.jsoup.nodes.Document;


import br.com.lett.crawlernode.core.crawler.RatingReviewCrawler;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;

public class SaopauloSubmarinoRatingReviewCrawler extends RatingReviewCrawler {

	public SaopauloSubmarinoRatingReviewCrawler(Session session) {
		super(session);
	}

	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
		
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

		return ratingReviewsCollection;
	}

}
