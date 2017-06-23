package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.GETFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.models.RatingsReviews;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

public class SaopauloDrogasilRatingReviewCrawler extends RatingReviewCrawler {

	public SaopauloDrogasilRatingReviewCrawler(Session session) {
		super(session);
	}

	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

		if (isProductPage(document)) {
			RatingsReviews ratingReviews = new RatingsReviews();
			
			String internalId = crawlInternalId(document);
			
			ratingReviews.setDate(session.getDate());
			ratingReviews.setInternalId(internalId);
			
			JSONObject trustVoxResponse = requestTrustVoxEndpoint(internalId);
			
			ratingReviews.setTotalRating(getTotalNumOfRatings(trustVoxResponse));
			ratingReviews.setAverageOverallRating(getTotalRating(trustVoxResponse));
			
			ratingReviewsCollection.addRatingReviews(ratingReviews);
			
			
		}

		return ratingReviewsCollection;

	}
	
	/**
	 * 
	 * @param trustVoxResponse
	 * @return the total of evaluations
	 */
	private Integer getTotalNumOfRatings(JSONObject trustVoxResponse) {
		if (trustVoxResponse.has("items")) {
			JSONArray ratings = trustVoxResponse.getJSONArray("items");
			return ratings.length();
		}
		return 0;
	}
	
	private Double getTotalRating(JSONObject trustVoxResponse) {
		Double totalRating = 0.0;
		int count = 0;
		if (trustVoxResponse.has("items")) {
			JSONArray ratings = trustVoxResponse.getJSONArray("items");
			
			for (int i = 0; i < ratings.length(); i++) {
				JSONObject rating = ratings.getJSONObject(i);
				
				if (rating.has("rate")) {
					totalRating += rating.getInt("rate");
					count++;
				}
			}
		}

		if(count > 0) {
			return MathCommonsMethods.normalizeTwoDecimalPlaces(totalRating / count);
		} 
		
		return totalRating;
	}
	
	private JSONObject requestTrustVoxEndpoint(String id) {
		StringBuilder requestURL = new StringBuilder();
		
		requestURL.append("http://trustvox.com.br/widget/opinions?code=");
		requestURL.append(id);
		
		requestURL.append("&");
		requestURL.append("store_id=71447");
		
		requestURL.append("&");
		requestURL.append(session.getOriginalURL());
				
		Map<String, String> headerMap = new HashMap<>();
		headerMap.put(DataFetcher.HTTP_HEADER_ACCEPT, "application/vnd.trustvox-v2+json");
		headerMap.put(DataFetcher.HTTP_HEADER_CONTENT_TYPE, "application/json; charset=utf-8");
		
		String response = GETFetcher.fetchPageGETWithHeaders(session, requestURL.toString(), null, headerMap, 1);
		
		JSONObject trustVoxResponse;
		try {
			trustVoxResponse = new JSONObject(response);
		} catch (JSONException e) {
			Logging.printLogError(logger, session, "Error creating JSONObject from trustvox response.");
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
			
			trustVoxResponse = new JSONObject();
		}
		
		return trustVoxResponse;
	}

	private boolean isProductPage(Document document) {
		return document.select("#details .col-2 .data-table tr .data").first() != null;
	}

	private String crawlInternalId(Document doc) {
		String internalId = null;
		
		Element elementInternalID = doc.select("#details .col-2 .data-table tr .data").first();
		if(elementInternalID != null) {
			internalId = elementInternalID.text();
		}

		return internalId;
	}
	

}
