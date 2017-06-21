package br.com.lett.crawlernode.core.models;


import org.joda.time.DateTime;
import org.json.JSONObject;

import br.com.lett.crawlernode.util.DateConstants;
import br.com.lett.crawlernode.util.MathCommonsMethods;

public class RatingsReviews implements Cloneable {

	public static final String DATE_JSON_FIELD = "date";
	public static final String TOTAL_REVIEWS_JSON_FIELD = "total_reviews";
	public static final String AVERAGE_OVERALL_RATING_JSON_FIELD = "average_overall_rating";

	private DateTime date;
	private Integer totalReviews;
	private Double averageOverallRating;
	private String internalId;

	public RatingsReviews() {
		this.date = null;
		this.totalReviews = null;
		this.averageOverallRating = null;
		this.internalId = null;
	}

	public void setJSONRepresentation(JSONObject ratingReviewsJSON) {
		if (ratingReviewsJSON != null) {
			if (ratingReviewsJSON.has(DATE_JSON_FIELD) && !ratingReviewsJSON.isNull(DATE_JSON_FIELD)) {
				date = new DateTime(ratingReviewsJSON.getString(DATE_JSON_FIELD)).withZone(DateConstants.timeZone);
			}
			if (ratingReviewsJSON.has(TOTAL_REVIEWS_JSON_FIELD) && !ratingReviewsJSON.isNull(TOTAL_REVIEWS_JSON_FIELD)) {
				totalReviews = ratingReviewsJSON.getInt(TOTAL_REVIEWS_JSON_FIELD);
			}
			if (ratingReviewsJSON.has(AVERAGE_OVERALL_RATING_JSON_FIELD) && !ratingReviewsJSON.isNull(AVERAGE_OVERALL_RATING_JSON_FIELD)) {
				averageOverallRating = MathCommonsMethods.normalizeTwoDecimalPlaces(ratingReviewsJSON.getDouble(AVERAGE_OVERALL_RATING_JSON_FIELD));
			}
			
			sanitize();
		}
	}
	
	private void sanitize() {
		if (date != null && totalReviews == null) {
			totalReviews = 0;
		}
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public JSONObject getJSON() {

		if (date == null) {
			return null;
		}

		JSONObject ratingReviewsJson = new JSONObject();
		
		sanitize();

		ratingReviewsJson.put(DATE_JSON_FIELD, this.date.toString());

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

		stringBuilder.append("Date: " + date.toString() + "\n");
		stringBuilder.append("TotalReviews: " + totalReviews + "\n");
		stringBuilder.append("AverageOverallRating: " + averageOverallRating + "\n");
		stringBuilder.append("InternalId: " + internalId + "\n");

		return stringBuilder.toString();
	}

	public DateTime getDate() {
		return date;
	}

	public void setDate(DateTime date) {
		this.date = date;
	}

	public Integer getTotalReviews() {
		return totalReviews;
	}

	public void setTotalRating(Integer totalReviews) {
		this.totalReviews = totalReviews;
	}

	public Double getAverageOverallRating() {
		return averageOverallRating;
	}

	public void setAverageOverallRating(Double averageOverallRating) {
		this.averageOverallRating = MathCommonsMethods.normalizeTwoDecimalPlaces(averageOverallRating);
	}

	public String getInternalId() {
		return internalId;
	}

	public void setInternalId(String internalId) {
		this.internalId = internalId;
	}
}
