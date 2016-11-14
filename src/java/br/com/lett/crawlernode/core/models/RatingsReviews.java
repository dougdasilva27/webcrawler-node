package br.com.lett.crawlernode.core.models;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

public class RatingsReviews {
	
	private Map<Integer, Integer> rating;
	
	public RatingsReviews() {
		this.rating = new HashMap<Integer, Integer>();
		
		this.rating.put(1, 0);
		this.rating.put(2, 0);
		this.rating.put(3, 0);
		this.rating.put(4, 0);
		this.rating.put(5, 0);
	}
	
	public void addRating(Integer ratingValue, Integer count) {
		this.rating.put(ratingValue, count);
	}
	
	public JSONObject assembleJson() {
		JSONObject ratingReviewsJson = new JSONObject();
		
		ratingReviewsJson.put(String.valueOf(1), this.rating.get(1));
		ratingReviewsJson.put(String.valueOf(2), this.rating.get(2));
		ratingReviewsJson.put(String.valueOf(3), this.rating.get(3));
		ratingReviewsJson.put(String.valueOf(4), this.rating.get(4));
		ratingReviewsJson.put(String.valueOf(5), this.rating.get(5));
		
		return ratingReviewsJson;
	}
	
	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		
		stringBuilder.append("1: " + this.rating.get("1") + ", ");
		stringBuilder.append("2: " + this.rating.get("2") + ", ");
		stringBuilder.append("3: " + this.rating.get("3") + ", ");
		stringBuilder.append("4: " + this.rating.get("4") + ", ");
		stringBuilder.append("5: " + this.rating.get("5"));
		
		return stringBuilder.toString();
	}
}
