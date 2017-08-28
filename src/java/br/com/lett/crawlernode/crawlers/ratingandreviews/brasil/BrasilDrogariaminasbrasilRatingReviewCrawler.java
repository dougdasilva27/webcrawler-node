package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

/**
 * Date: 28/08/17
 * @author gabriel
 *
 */
public class BrasilDrogariaminasbrasilRatingReviewCrawler extends RatingReviewCrawler {

	public BrasilDrogariaminasbrasilRatingReviewCrawler(Session session) {
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
			Double avgRating = getTotalAvgRating(document, totalNumOfEvaluations);

			ratingReviews.setInternalId(internalId);
			ratingReviews.setTotalRating(totalNumOfEvaluations);
			ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
			ratingReviews.setAverageOverallRating(avgRating);

			ratingReviewsCollection.addRatingReviews(ratingReviews);
		}

		return ratingReviewsCollection;

	}

	private String crawlInternalId(Document doc){
		String internalId = null;
		
		Element internalIdElement = doc.select("input[name=product]").first();
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
	private Double getTotalAvgRating(Document doc, Integer totalRatings) {
		Double avgRating = 0d;
		Element rating = doc.select("meta[itemprop=ratingValue]").first();
		
		if(rating != null) {
			avgRating = Double.parseDouble(rating.attr("content"));
		}
		
		return avgRating;
	}

	/**
	 * Number of ratings appear in html element 
	 * @param doc
	 * @return
	 */
	private Integer getTotalNumOfRatings(Document doc) {
		return doc.select(".Opinioes .Opiniao").size();
	}


	private boolean isProductPage(Document doc) {
		return doc.select(".PaginaProduto").first() != null;
	}
	
}
