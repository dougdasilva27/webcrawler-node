package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
public class BrasilCentralarRatingReviewCrawler extends RatingReviewCrawler {

	public BrasilCentralarRatingReviewCrawler(Session session) {
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
			ratingReviews.setAverageOverallRating(avgRating);

			ratingReviewsCollection.addRatingReviews(ratingReviews);
		}

		return ratingReviewsCollection;

	}

	private String crawlInternalId(Document doc){
		String internalId = null;
		Element boxElement = doc.select(".dados1 .box div[style]:not([class])").first();

		if (boxElement != null) {
			String childText = boxElement.ownText().toLowerCase();

			if (childText.contains("do produto:")) {
				String[] tokens = childText.split(":");

				if (tokens.length > 1) {
					internalId = tokens[1].trim();
				}
			}
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
		
		if(totalRatings != null && totalRatings > 0) {
			Elements ratings = doc.select("div.estrelas");
			
			Integer values = 0;
			Integer count = 0;
			
			for(Element e : ratings) {
				Element stars = e.select("> .avaliacao2").first();
				Element value = e.select("> span[style]").first();
				
				if(stars != null && value != null) {
					Integer star = Integer.parseInt(stars.ownText().replaceAll("[^0-9]", "").trim());
					Integer countStars = Integer.parseInt(value.ownText().replaceAll("[^0-9]", "").trim());
					
					count += countStars;
					values += star * countStars;
				}
			}
			
			if(count > 0) {
				avgRating =  MathUtils.normalizeTwoDecimalPlaces(((double)values) / count);
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
		Integer ratingNumber = 0;
		Element reviews = doc.select("#formAvaliacao .hreview > span").first();

		if(reviews != null) {
			String text = reviews.ownText().replaceAll("[^0-9]", "").trim();
			
			if(!text.isEmpty()) {
				ratingNumber = Integer.parseInt(text);
			}
		}
		
		return ratingNumber;
	}


	private boolean isProductPage(Document doc) {
		if ( doc.select(".dados1").first() != null ) {
			return true;
		}
		return false;
	}
	
}
