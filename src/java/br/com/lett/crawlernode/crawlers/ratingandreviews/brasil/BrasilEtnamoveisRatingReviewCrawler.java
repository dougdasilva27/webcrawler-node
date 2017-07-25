package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.models.RatingsReviews;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;

/**
 * Date: 25/07/17
 * @author gabriel
 *
 */
public class BrasilEtnamoveisRatingReviewCrawler extends RatingReviewCrawler {

	public BrasilEtnamoveisRatingReviewCrawler(Session session) {
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
		Element internalIdElement = doc.select("span#sku-id span").first();

		if (internalIdElement != null) {
			internalId = internalIdElement.text().trim();			
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
		Element rating = doc.select(".avaliacoes .avalia span.nota").first();

		if (rating != null) {
			String text = rating.attr("class").replaceAll("[^0-9]", "").trim();
			
			if(!text.isEmpty()) {
				avgRating = Double.parseDouble(text);
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
		Integer ratingTotal = 0;
		Element total = doc.select(".acoescoment div.btver").first();
		
		if(total != null) {
			String text = total.ownText().replaceAll("[^0-9]", "").trim();
			
			if(!text.isEmpty()) {
				ratingTotal = Integer.parseInt(text);
			}
		}
		
		return ratingTotal;
	}


	private boolean isProductPage(String url) {
		return url.startsWith("https://www.etna.com.br/etna/p/");
	}

}
