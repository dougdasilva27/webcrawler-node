package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.RatingReviewCrawler;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.models.RatingsReviews;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

/**
 * Date: 13/12/16
 * @author gabriel
 *
 */
public class BrasilBemolRatingReviewCrawler extends RatingReviewCrawler {

	public BrasilBemolRatingReviewCrawler(Session session) {
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

			if(internalId != null) {
				Integer totalNumOfEvaluations = getTotalNumOfRatings(document);			
				Double avgRating = getTotalAvgRating(document, totalNumOfEvaluations);

				ratingReviews.setInternalId(internalId);
				ratingReviews.setTotalReviews(totalNumOfEvaluations);
				ratingReviews.setAverageOverallRating(avgRating);

				ratingReviewsCollection.addRatingReviews(ratingReviews);
			}

		}

		return ratingReviewsCollection;

	}

	private String crawlInternalId(Document doc){
		String internalID = null;
		Element elementInternalID = doc.select("input.txtHiddenCantentryId").first();

		if (elementInternalID != null && elementInternalID.val() != null && !elementInternalID.val().isEmpty()) {
			internalID = elementInternalID.val().trim();
		}

		return internalID;
	}

	/**
	 * Average is calculate 
	 * Example: 
	 *  5 estrelas [percentage bar] 347
	 *  4 estrelas [percentage bar] 42
	 * 
	 * @param document
	 * @return
	 */
	private Double getTotalAvgRating(Document docRating, Integer totalRating) {
		Double avgRating = null;
		Elements rating = docRating.select("#RatingReviewDiv .rating_bars .bar-value");

		if (totalRating != null && totalRating > 0) {
			Double total = 0.0;
			
			// numero da estrela
			int star = 5;
			
			for (Element e : rating) {				
				total += Double.parseDouble(e.ownText().trim()) * star;
				
				// começa de 5 estrelas e vai ate 1
				star--;
			}

			avgRating = MathCommonsMethods.normalizeTwoDecimalPlaces(total / totalRating);
		}

		return avgRating;
	}

	/**
	 * Number of ratings appear in html element 
	 * @param doc
	 * @return
	 */
	private Integer getTotalNumOfRatings(Document doc) {
		Integer totalRating = 0;
		Elements rating = doc.select("#RatingReviewDiv .rating_bars .bar-value");

		for (Element e : rating) {
			String votes = e.text().replaceAll("[^0-9]", "").trim();

			totalRating += Integer.parseInt(votes);				
		}

		return totalRating;
	}


	private boolean isProductPage(Document doc) {
		return doc.select("#product").first() != null;
	}

}
