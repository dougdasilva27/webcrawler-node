package br.com.lett.crawlernode.core.models;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

public class RatingsReviews {
	
	private Map<String, Integer> rating;
	
	public RatingsReviews() {
		this.rating = new HashMap<String, Integer>();
		
		this.rating.put("1", null);
		this.rating.put("2", null);
		this.rating.put("3", null);
		this.rating.put("4", null);
		this.rating.put("5", null);
	}
	
	public void addRating(String ratingValue, Integer count) {
		if (count > 0) {
			this.rating.put(ratingValue, count);
		}
	}
	
	public JSONObject assembleJson() {
		JSONObject ratingReviewsJson = new JSONObject();
		
		for (String key : this.rating.keySet()) {
			if (this.rating.get(key) == null) {
				ratingReviewsJson.put(String.valueOf(1), JSONObject.NULL);
			} else {
				ratingReviewsJson.put(String.valueOf(1), this.rating.get(1));
			}
		}
		
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
