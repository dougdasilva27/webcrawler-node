package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Date: 24/08/17
 * @author gabriel
 *
 */
public class BrasilPluscareRatingReviewCrawler extends RatingReviewCrawler {

	public BrasilPluscareRatingReviewCrawler(Session session) {
		super(session);
	}

	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

		if (isProductPage(document)) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			RatingsReviews ratingReviews = new RatingsReviews();			
			ratingReviews.setDate(session.getDate());

			String internalId = crawlInternalId(document);

			Integer totalNumOfEvaluations = getTotalNumOfRatings(document);			
			Double avgRating = getTotalAvgRating(document);

			ratingReviews.setInternalId(internalId);
			ratingReviews.setTotalRating(totalNumOfEvaluations);
			ratingReviews.setAverageOverallRating(avgRating);

			ratingReviewsCollection.addRatingReviews(ratingReviews);
		}

		return ratingReviewsCollection;

	}

	private String crawlInternalId(Document doc){
		String internalId = null;
		Element internalIdElement = doc.select("#IdProduto").first();
		
		if (internalIdElement != null) {
			internalId = internalIdElement.val();
		}

		return internalId;
	}

	/**
	 * Avg appear in html element
	 * 
	 * @param document
	 * @return
	 */
	private Double getTotalAvgRating(Document doc) {
		Double avgRating = 0d;
		Element avg = doc.select("#nota").first();
		
		if(avg != null) {
			avgRating = avg.val().isEmpty() ? 0d: Double.parseDouble(avg.val());
		}

		return avgRating;
	}

	/**
	 * Number of ratings appear in html element 
	 * @param doc
	 * @return
	 */
	private Integer getTotalNumOfRatings(Document doc) {

		return doc.select(".comentarios ul li").size();
	}


	private boolean isProductPage(Document doc) {
		return doc.select("#IdProduto").first() != null;
	}
	
}
