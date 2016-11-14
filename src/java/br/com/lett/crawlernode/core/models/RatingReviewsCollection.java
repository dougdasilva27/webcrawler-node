package br.com.lett.crawlernode.core.models;

import java.util.ArrayList;
import java.util.List;

public class RatingReviewsCollection {
	
	private List<RatingsReviews> ratingReviewsList;
	
	public RatingReviewsCollection() {
		this.ratingReviewsList = new ArrayList<RatingsReviews>();
	}
	
	public void addRatingReviews(RatingsReviews ratingReviews) {
		this.ratingReviewsList.add(ratingReviews);
	}
	
	public List<RatingsReviews> getRatingReviewsList() {
		return this.ratingReviewsList;
	}
	
	public int getRatingReviewsCount() {
		return this.ratingReviewsList.size();
	}

}
