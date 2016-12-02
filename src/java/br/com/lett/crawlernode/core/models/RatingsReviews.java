package br.com.lett.crawlernode.core.models;


import org.joda.time.DateTime;
import org.json.JSONObject;

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
			if (ratingReviewsJSON.has(DATE_JSON_FIELD)) {
				Object obj = ratingReviewsJSON.get(DATE_JSON_FIELD); 
				if (!obj.equals(JSONObject.NULL)) {
					date = new DateTime(ratingReviewsJSON.getString(DATE_JSON_FIELD));
				}
			}
			if (ratingReviewsJSON.has(TOTAL_REVIEWS_JSON_FIELD)) {
				Object obj = ratingReviewsJSON.get(TOTAL_REVIEWS_JSON_FIELD); 
				if (!obj.equals(JSONObject.NULL)) {
					totalReviews = ratingReviewsJSON.getInt(TOTAL_REVIEWS_JSON_FIELD);
				}
			}
			if (ratingReviewsJSON.has(AVERAGE_OVERALL_RATING_JSON_FIELD)) {
				Object obj = ratingReviewsJSON.get(TOTAL_REVIEWS_JSON_FIELD);
				if (!obj.equals(JSONObject.NULL)) {
					averageOverallRating = MathCommonsMethods.normalizeTwoDecimalPlaces(ratingReviewsJSON.getDouble(AVERAGE_OVERALL_RATING_JSON_FIELD));
				}
			}
		}
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
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

	public void setTotalReviews(Integer totalReviews) {
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
