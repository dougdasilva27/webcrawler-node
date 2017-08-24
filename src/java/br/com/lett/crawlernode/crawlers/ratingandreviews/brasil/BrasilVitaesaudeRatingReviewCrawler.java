package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;
import models.RatingsReviews;

/**
 * Date: 24/08/17
 * @author gabriel
 *
 */
public class BrasilVitaesaudeRatingReviewCrawler extends RatingReviewCrawler {

	public BrasilVitaesaudeRatingReviewCrawler(Session session) {
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
			Double avgRating = getTotalAvgRating(document, totalNumOfEvaluations);

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
		List<String> idList = new ArrayList<>();

		String internalPid = crawlInternalPid(doc);
		Elements variationsRadio = doc.select(".ProductOptionList li label");
		Elements variationsBox = doc.select(".ProductOptionList option");
		
		boolean isRadio = variationsRadio.size() > 0;
		Elements variations = isRadio ? variationsRadio : variationsBox;
		
		if(variationsRadio.size() > 0 || variationsBox.size() > 0) {
			for(Element e : variations) {
				//Id variation
				String variationId = isRadio ? e.select("input").val() : e.val().trim();
				
				if(!variationId.isEmpty()) {
					idList.add(internalPid + "-" + variationId);
				}
			}
		} else {
			idList.add(internalPid + "-" + internalPid);
		}
		
		return idList;
	}

	private String crawlInternalPid(Document doc) {
		String internalPid = null;
		Element pdi = doc.select("input[name=product_id]").first();
		
		if(pdi != null) {
			internalPid = pdi.val();
		}
		
		return internalPid;
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
			Elements ratings = doc.select(".boxAvaliacao");
			
			Integer values = 0;
			Integer count = 0;
			
			for(Element e : ratings) {
				Element stars = e.select("> i").first();
				Element value = e.select("> span").first();
				
				if(stars != null && value != null) {
					Integer star = Integer.parseInt(CommonMethods.getLast(stars.attr("class").split("-")));
					Integer countStars = Integer.parseInt(value.ownText().replaceAll("[^0-9]", "").trim());
					
					count += countStars;
					values += star * countStars;
				}
			}
			
			if(count > 0) {
				avgRating =  MathCommonsMethods.normalizeTwoDecimalPlaces(((double)values) / count);
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
		Element reviews = doc.select(".ProductRating").first();

		if(reviews != null) {
			String text = reviews.ownText().replaceAll("[^0-9]", "").trim();
			
			if(!text.isEmpty()) {
				ratingNumber = Integer.parseInt(text);
			}
		}
		
		return ratingNumber;
	}


	private boolean isProductPage(Document doc) {
		return doc.select("input[name=product_id]").first() != null;
	}
	
}
