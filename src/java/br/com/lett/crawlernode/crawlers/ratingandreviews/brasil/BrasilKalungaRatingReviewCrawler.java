package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.RatingsReviews;

/**
 * Date: 13/12/16
 * @author gabriel
 *
 */
public class BrasilKalungaRatingReviewCrawler extends RatingReviewCrawler {

	public BrasilKalungaRatingReviewCrawler(Session session) {
		super(session);
	}

	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

		if (isProductPage(session.getOriginalURL())) {
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
		Element elementInternalId = doc.select("input#hdnCodProduto").first();
		
		if(elementInternalId != null) {
			internalId = elementInternalId.attr("value").trim();
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
		Double avgRating = null;
		Element rating = doc.select("span[itemprop=ratingValue]").first();

		if (rating != null) {
			String text = rating.ownText().trim();
			
			if(!text.isEmpty()) {
				avgRating = MathUtils.normalizeTwoDecimalPlaces(MathUtils.parseFloatWithComma(text).doubleValue());
			}
		}

		return avgRating;
	}

	/**
	 * Number of ratings appear in html element 
	 * @param doc
	 * @return
	 */
	private Integer getTotalNumOfRatings(Document doc) {
		Integer rating = null;
		Element ratingElement = doc.select("span[itemprop=reviewCount]").first();
		
		if(ratingElement != null) {
			rating = Integer.parseInt(ratingElement.ownText().replaceAll("[^0-9]", "").trim());
		}
		
		return rating;
	}


	private boolean isProductPage(String url) {
		return url.contains("/prod/");
	}

}
