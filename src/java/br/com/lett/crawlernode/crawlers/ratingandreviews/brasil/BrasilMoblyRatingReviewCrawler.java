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

/**
 * Date: 25/07/17
 * @author gabriel
 *
 */
public class BrasilMoblyRatingReviewCrawler extends RatingReviewCrawler {

	public BrasilMoblyRatingReviewCrawler(Session session) {
		super(session);
	}

	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

		if (isProductPage(document)) {
			RatingsReviews ratingReviews = new RatingsReviews();			
			ratingReviews.setDate(session.getDate());

			Integer totalNumOfEvaluations = getTotalNumOfRatings(document);			
			Double avgRating = getTotalAvgRating(document);

			ratingReviews.setTotalRating(totalNumOfEvaluations);
			ratingReviews.setAverageOverallRating(avgRating);

			List<String> idList = crawlIdList(document);
			
			for(String internalId : idList) {					
				RatingsReviews clonedRatingReviews = (RatingsReviews)ratingReviews.clone();
				clonedRatingReviews.setInternalId(internalId);
				ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
			}

		}

		return ratingReviewsCollection;

	}


	private List<String> crawlIdList(Document doc){
		List<String> products = new ArrayList<>();
		Elements skus = doc.select(".product-option .custom-select option[data-js-function]");

		if(skus.size() > 0){
			for(Element sku : skus){
				products.add(sku.val().trim());
			}
		} else {
			Element internalIdElement = doc.select(".add-wishlistsel-product-move-to-wishlist").first();
			
			if (internalIdElement != null) {
				products.add(internalIdElement.attr("data-simplesku"));
			}
		}
		
		return products;
	}
	
	/**
	 * Average is in html element 
	 * @param document
	 * @return
	 */
	private Double getTotalAvgRating(Document doc) {
		Double avgRating = 0d;
		Element rating = doc.select("#product-reviews .c100.p94.size span").first();

		if(rating != null) {
			String avgText = rating.text().trim();
			
			if(!avgText.isEmpty()){
				avgRating =  Double.parseDouble(avgText);
			}
		}
		
		return avgRating;
	}

	/**
	 * Number of ratings appear in html element 
	 * @param docRating
	 * @return
	 */
	private Integer getTotalNumOfRatings(Document docRating) {
		Integer totalRating = 0;
		Element totalRatingElement = docRating.select("#product-reviews span[itemprop=reviewCount]").first();

		if(totalRatingElement != null) {
			String totalText = totalRatingElement.ownText().replaceAll("[^0-9]", "").trim();
			
			if(!totalText.isEmpty()){
				totalRating = Integer.parseInt(totalText);
			}
		}

		return totalRating;
	}



	private boolean isProductPage(Document doc) {
		return doc.select("#product-info").first() != null;
	}
}
