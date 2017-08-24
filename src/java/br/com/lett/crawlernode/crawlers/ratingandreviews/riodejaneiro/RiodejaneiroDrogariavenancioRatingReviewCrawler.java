package br.com.lett.crawlernode.crawlers.ratingandreviews.riodejaneiro;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

/**
 * Date: 14/12/16
 * @author gabriel
 *
 */
public class RiodejaneiroDrogariavenancioRatingReviewCrawler extends RatingReviewCrawler {

	public RiodejaneiroDrogariavenancioRatingReviewCrawler(Session session) {
		super(session);
	}

	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

		if (isProductPage(session.getOriginalURL())) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			RatingsReviews ratingReviews = new RatingsReviews();			
			ratingReviews.setDate(session.getDate());

			Integer totalNumOfEvaluations = getTotalNumOfRatings(document);			
			Double avgRating = getTotalAvgRating(document);

			ratingReviews.setTotalRating(totalNumOfEvaluations);
			ratingReviews.setAverageOverallRating(avgRating);

			List<String> idList = crawlInternalIds(document);
			for (String internalId : idList) {
				RatingsReviews clonedRatingReviews = (RatingsReviews)ratingReviews.clone();
				clonedRatingReviews.setInternalId(internalId);
				ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
			}
		}

		return ratingReviewsCollection;

	}

	/**
	 * Average is in html element
	 * 
	 * @param document
	 * @return
	 */
	private Double getTotalAvgRating(Document doc) {
		Double avgRating = null;
		Element avg = doc.select(".comentarios #nota").last();

		if(avg != null && !avg.val().isEmpty()) {
			avgRating = Double.parseDouble(avg.val());
		}

		return avgRating;
	}

	/**
	 * Number of ratings appear in html 
	 * Example: 
	 * Avaliação: *****
	 * Maria de Lourdes
	 * Bom
	 * 
	 * @param docRating
	 * @return
	 */
	private Integer getTotalNumOfRatings(Document docRating) {
		Integer totalRating = null;
		Elements totalRatingElements = docRating.select(".comentarios > ul li");

		if(!totalRatingElements.isEmpty()){
			totalRating = totalRatingElements.size();
		}

		return totalRating;
	}

	private List<String> crawlInternalIds(Document doc){
		List<String> ids = new ArrayList<>();
		
		// Pre id interno
		String preInternalId = null;
		Element elementPreInternalId = doc.select("#miolo .produtoPrincipal .info .codigo").first();
		if(elementPreInternalId != null) {
			preInternalId = elementPreInternalId.text().split(":")[1].trim();
		}

		Element variationElement = doc.select(".variacao").first();
		
		if(variationElement == null || (variationElement.select(".optionsVariacao li").size() == 0)) { 

			// Id interno
			Element internalIdElement = doc.select("input[name=IdProduto]").first();
			String internalId = null;
			if(internalIdElement != null){
				internalId = preInternalId + "-" + internalIdElement.attr("value");
			}
			
			ids.add(internalId);
		} else {
			Elements elementsVariations = doc.select(".optionsVariacao li");
			
			for(Element elementVariation : elementsVariations) {

				// Id interno
				String posInternalId = elementVariation.attr("data-value").split("#")[3];
				String internalId  = preInternalId + "-" + posInternalId;
				
				ids.add(internalId);
			}
		}

		return ids;
	}

	private boolean isProductPage(String url) {
		return url.contains("/produto/");
	}

}
