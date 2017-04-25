package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.models.RatingsReviews;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

/**
 * Date: 14/12/16
 * @author gabriel
 *
 */
public class BrasilSaraivaRatingReviewCrawler extends RatingReviewCrawler {

	public BrasilSaraivaRatingReviewCrawler(Session session) {
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
				Double avgRating = getTotalAvgRating(document);
				
				ratingReviews.setInternalId(internalId);
				ratingReviews.setTotalReviews(totalNumOfEvaluations);
				ratingReviews.setAverageOverallRating(avgRating);

				ratingReviewsCollection.addRatingReviews(ratingReviews);
			}

		}

		return ratingReviewsCollection;

	}

	private String crawlInternalId(Document doc){
		String internalId = null;

		Element elementSpan = doc.select("section.product-info h1 span").first();
		if (elementSpan != null) {
			String spanText = elementSpan.text();
			List<String> parsedNumbers = MathCommonsMethods.parseNumbers(spanText);
			if (!parsedNumbers.isEmpty()) {
				internalId = parsedNumbers.get(0);
			}
		}

		return internalId;
	}

	/**
	 * Average is in html element
	 * Example: 
	 * Número de Avaliações : 24
	 * Média das notas : 4.6 /5 
	 * 
	 * @param document
	 * @return
	 */
	private Double getTotalAvgRating(Document doc) {
		Double avgRating = null;
		Element avg = doc.select("#product-rating .rating_top.group .avg strong").first();
		
		if(avg != null && !avg.ownText().isEmpty()) {
			avgRating = Double.parseDouble(avg.ownText().trim());
		}

		return avgRating;
	}

	/**
	 * Number of ratings appear in html 
	 * @param docRating
	 * @return
	 */
	private Integer getTotalNumOfRatings(Document docRating) {
		Integer totalRating = null;
		Element totalRatingElement = docRating.select("#product-rating .ratings .amount").first();

		if(totalRatingElement != null) {
			String text = totalRatingElement.text().replaceAll("[^0-9]", "").trim();
			
			if(!text.isEmpty()) {
				totalRating = Integer.parseInt(text);
			}
		}

		return totalRating;
	}


	private boolean isProductPage(Document document) {
		Element elementProduct = document.select("section.product-allinfo").first();
		return elementProduct != null;
	}

}
