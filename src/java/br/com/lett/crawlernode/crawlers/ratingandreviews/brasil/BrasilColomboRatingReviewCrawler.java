package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.models.RatingsReviews;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;

/**
 * Date: 24/07/17
 * @author gabriel
 *
 */
public class BrasilColomboRatingReviewCrawler extends RatingReviewCrawler {

	public BrasilColomboRatingReviewCrawler(Session session) {
		super(session);
	}

	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

		if (isProductPage(document)) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			RatingsReviews ratingReviews = new RatingsReviews();			
			ratingReviews.setDate(session.getDate());

			Integer totalNumOfEvaluations = getTotalNumOfRatings(document);			
			Double avgRating = getTotalAvgRating(document);

			ratingReviews.setTotalRating(totalNumOfEvaluations);
			ratingReviews.setAverageOverallRating(avgRating);
			
			List<String> idList = crawlIdList(document);
			for (String internalId : idList) {
				RatingsReviews clonedRatingReviews = (RatingsReviews)ratingReviews.clone();
				clonedRatingReviews.setInternalId(internalId);
				ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
			}

		}

		return ratingReviewsCollection;

	}

	private List<String> crawlIdList(Document doc){
		List<String> internalIds = new ArrayList<>();
		Elements selections = doc.select(".dados-itens-table.dados-itens-detalhe tr[data-item]");
		
		if(selections.size() <= 1) {
			Element elementInternalID = doc.select("input[type=radio][checked]").first();
			if (elementInternalID != null) {
				internalIds.add(elementInternalID.attr("value").trim());
			} else {
				elementInternalID = doc.select("#itemAviso").first();
				if (elementInternalID != null) {
					internalIds.add(elementInternalID.attr("value").trim());
				}
				
			}
		} else {
			Elements variations = doc.select(".dados-itens-table.dados-itens-detalhe tr[data-item]");

			for(Element e : variations) {

				// ID interno
				Element variationElementInternalID = e.select("input").first();
				if (variationElementInternalID != null) {
					internalIds.add(e.attr("data-item").trim());
				}
			}
		}

		return internalIds;
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
	private Double getTotalAvgRating(Document docRating) {
		Double avgRating = 0d;
		Elements rating = docRating.select(".avalicoes-count strong");

		if (rating != null) {
			String text = rating.text().replaceAll("[^0-9,]", "").replace(",", ".").trim();
			
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
		Integer totalRating = 0;
		Element rating = doc.select(".avalicoes-count").first();

		if(rating != null) {
			String votes = rating.ownText().replaceAll("[^0-9]", "");

			if(!votes.isEmpty()) {
				totalRating = Integer.parseInt(votes);	
			}
		}

		return totalRating;
	}


	private boolean isProductPage(Document doc) {
		return doc.select(".detalhe-produto").first() != null;
	}

}
