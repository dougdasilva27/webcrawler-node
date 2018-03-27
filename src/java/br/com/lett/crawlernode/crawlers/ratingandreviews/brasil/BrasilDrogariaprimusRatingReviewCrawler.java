package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import models.RatingsReviews;

/**
 * Date: 28/08/17
 * @author gabriel
 *
 */
public class BrasilDrogariaprimusRatingReviewCrawler extends RatingReviewCrawler {

	public BrasilDrogariaprimusRatingReviewCrawler(Session session) {
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
			Pair<Integer, Double> rating = getRating(document);
			
			Integer totalNumOfEvaluations = rating.getFirst();		
			Double avgRating = rating.getSecond();

			ratingReviews.setInternalId(internalId);
			ratingReviews.setTotalRating(totalNumOfEvaluations);
			ratingReviews.setAverageOverallRating(avgRating);

			ratingReviewsCollection.addRatingReviews(ratingReviews);
		}

		return ratingReviewsCollection;

	}

	private String crawlInternalId(Document doc){
		String internalId = null;
		Element internalIdElement = doc.select(".corpo_conteudo article[itemid]").first();
		
		if (internalIdElement != null) {
			internalId = internalIdElement.attr("itemid");
		}

		return internalId;
	}

	/**
	 * Avg is calculated
	 * 
	 * @param document
	 * @return
	 */
	private Pair<Integer, Double> getRating(Document doc) {
		Double avgRating = 0d;
		Integer ratingNumber = 0;
		
		Elements ratings = doc.select("#ratings .rating li");
		Integer values = 0;
		
		for(Element e : ratings) {
			Element stars = e.select("> span:not([class]) img").first();
			Element value = e.select("> span:not([class])").last();
			
			if(stars != null && value != null) {
				String starText = stars.attr("alt").replaceAll("[^0-9]", "").trim();
				String countStarsText = value.ownText().split("%")[1].replaceAll("[^0-9]", "").trim();
				
				if(!starText.isEmpty() && !countStarsText.isEmpty()) {
					Integer star = Integer.parseInt(starText);
					Integer countStars = Integer.parseInt(countStarsText);
					
					ratingNumber += countStars;
					values += star * countStars;
				}
			}
		}
		
		if(ratingNumber > 0) {
			avgRating =  MathUtils.normalizeTwoDecimalPlaces(((double)values) / ratingNumber);
		}

		return  new Pair<>(ratingNumber, avgRating);
	}

	private boolean isProductPage(Document doc) {
		return doc.select("#detalhe_produto").first() != null;
	}
	
}
