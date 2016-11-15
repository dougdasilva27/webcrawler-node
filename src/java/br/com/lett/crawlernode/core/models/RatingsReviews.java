package br.com.lett.crawlernode.core.models;


import org.joda.time.DateTime;
import org.json.JSONObject;

public class RatingsReviews {
	
	public static final String DATE_JSON_FIELD = "date";
	public static final String TOTAL_REVIEWS_JSON_FIELD = "total_reviews";
	public static final String AVERAGE_OVERALL_RATING_JSON_FIELD = "average_overall_rating";
	
	private DateTime date;
	private Integer totalReviews;
	private Double averageOverallRating;
	
	public RatingsReviews(DateTime date) {
		this.date = date;
		this.totalReviews = null;
		this.averageOverallRating = null;
	}
	
	public RatingsReviews(JSONObject ratingReviewsJSON) {
		this.date = new DateTime(ratingReviewsJSON.getString(DATE_JSON_FIELD));
		this.totalReviews = ratingReviewsJSON.getInt(TOTAL_REVIEWS_JSON_FIELD);
		this.averageOverallRating = ratingReviewsJSON.getDouble(AVERAGE_OVERALL_RATING_JSON_FIELD);
	}
	
	public JSONObject getJSON() {
		JSONObject ratingReviewsJson = new JSONObject();
		
		if (date == null) {
			ratingReviewsJson.put(DATE_JSON_FIELD, JSONObject.NULL);
		} else {
			ratingReviewsJson.put(DATE_JSON_FIELD, this.date.toString());
		}
		
		if (this.totalReviews == null) {
			ratingReviewsJson.put(TOTAL_REVIEWS_JSON_FIELD, JSONObject.NULL);
		} else {
			ratingReviewsJson.put(TOTAL_REVIEWS_JSON_FIELD, this.totalReviews);
		}
		
		if (this.averageOverallRating == null) {
			ratingReviewsJson.put(AVERAGE_OVERALL_RATING_JSON_FIELD, JSONObject.NULL);
		} else {
			ratingReviewsJson.put(AVERAGE_OVERALL_RATING_JSON_FIELD, this.averageOverallRating);
		}		
		
		return ratingReviewsJson;
	}
	
	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		
		stringBuilder.append("Date: " + this.date.toString() + ", ");
		stringBuilder.append("TotalReviews: " + this.totalReviews + ", ");
		stringBuilder.append("AverageOverallRating: " + this.averageOverallRating + ", ");
		
		return stringBuilder.toString();
	}

	public Integer getTotalReviews() {
		return totalReviews;
	}

	public void setTotalReviews(Integer totalReviews) {
		this.totalReviews = totalReviews;
	}

	public Double getAverageOverallRating() {
		return averageOverallRating;
	}

	public void setAverageOverallRating(Double averageOverallRating) {
		this.averageOverallRating = averageOverallRating;
	}
}
