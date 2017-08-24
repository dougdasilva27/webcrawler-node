package br.com.lett.crawlernode.core.models;

import java.util.ArrayList;
import java.util.List;

import models.RatingsReviews;

public class RatingReviewsCollection {
	
	private List<RatingsReviews> ratingReviewsList;
	
	public RatingReviewsCollection() {
		this.ratingReviewsList = new ArrayList<>();
	}
	
	public void addRatingReviews(RatingsReviews ratingReviews) {
		this.ratingReviewsList.add(ratingReviews);
	}
	
	public RatingsReviews getRatingReviews(String internalId) {
		for (RatingsReviews ratingReviews : this.ratingReviewsList) {
			if (ratingReviews.getInternalId() != null && internalId.equals(ratingReviews.getInternalId())) {
				return ratingReviews;
			}
		}
		return null;
	}
	
	public List<RatingsReviews> getRatingReviewsList() {
		return this.ratingReviewsList;
	}
	
	public int getRatingReviewsCount() {
		return this.ratingReviewsList.size();
	}

}
