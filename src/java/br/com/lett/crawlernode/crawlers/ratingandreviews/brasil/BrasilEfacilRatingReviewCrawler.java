package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.GETFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;
import models.RatingsReviews;

public class BrasilEfacilRatingReviewCrawler extends RatingReviewCrawler {

	public BrasilEfacilRatingReviewCrawler(Session session) {
		super(session);
	}

	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

		if (isProductPage(session.getOriginalURL())) {

			String trustVoxId = crawlTrustVoxId(document);
			JSONObject trustVoxResponse = requestTrustVoxEndpoint(trustVoxId);
			
			Integer totalNumOfEvaluations = getTotalNumOfRatings(trustVoxResponse);
			Double totalRating = getTotalRating(trustVoxResponse);
			
			Double avgRating = null;
			if (totalNumOfEvaluations > 0) {
				avgRating = MathCommonsMethods.normalizeTwoDecimalPlaces(totalRating / totalNumOfEvaluations);
			}
			
			List<String> ids = crawlProductsIds(document);
			
			for(String internalId : ids) {
				RatingsReviews ratingReviews = new RatingsReviews();
				
				ratingReviews.setDate(session.getDate());
				ratingReviews.setInternalId(internalId);
				ratingReviews.setTotalRating(totalNumOfEvaluations);
				ratingReviews.setAverageOverallRating(avgRating);
				
				ratingReviewsCollection.addRatingReviews(ratingReviews);
			}
			
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
		if (trustVoxResponse.has("items")) {
			JSONArray ratings = trustVoxResponse.getJSONArray("items");
			
			for (int i = 0; i < ratings.length(); i++) {
				JSONObject rating = ratings.getJSONObject(i);
				
				if (rating.has("rate")) {
					totalRating += rating.getInt("rate");
				}
			}
		}
		return totalRating;
	}
	
	private JSONObject requestTrustVoxEndpoint(String id) {
		StringBuilder requestURL = new StringBuilder();
		
		requestURL.append("http://trustvox.com.br/widget/opinions?code=");
		requestURL.append(id);
		
		requestURL.append("&");
		requestURL.append("store_id=545");
		
		requestURL.append("&url=");
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

	private boolean isProductPage(String url) {
		return url.startsWith("http://www.efacil.com.br/loja/produto/");
	}

	private String crawlTrustVoxId(Document doc) {
		String trustVoxId = null;
		
		Element elementInternalPid = doc.select(".product-code #product-code").first();
		if (elementInternalPid != null) {
			trustVoxId = elementInternalPid.text().trim();
		} 
		
		return trustVoxId;
	}
	
	private String crawlInternalId(Document doc) {
		String internalPid = null;
		Element elementInternalPid = doc.select("input#productId").first();
		if (elementInternalPid != null) {
			internalPid = elementInternalPid.attr("value").trim();
		} else {
			elementInternalPid = doc.select("input[name=productId]").first();
			if (elementInternalPid != null) {
				internalPid = elementInternalPid.attr("value").trim();
			}
		}
		
		// ID interno
		String internalId = null;
		
		Element internalIdElement = doc.select("#entitledItem_" + internalPid).first();
		JSONObject info  = new JSONObject();
		
		if(internalIdElement != null){
			JSONArray infoArray = new JSONArray(internalIdElement.text().trim());
			info = infoArray.getJSONObject(0);

			internalId = info.getString("catentry_id");
		}

		return internalId;
	}
	
	private List<String> crawlProductsIds(Document doc) {
		List<String> idList = new ArrayList<>();
		
		// if has variations
		Element variationSelector = doc.select(".options_attributes").first();
		
		if (variationSelector == null) {
			idList.add(crawlInternalId(doc));
		} else {

			Element tmpIdElement = doc.select("input[id=productId]").first();
		
			if (tmpIdElement != null) {
				String tmpId = tmpIdElement.attr("value").trim();
				
				try {
					JSONArray variationsJsonInfo = new JSONArray(doc.select("#entitledItem_" + tmpId).text().trim());
					
					for(int i = 0; i < variationsJsonInfo.length(); i++) {

						JSONObject variationJsonObject = variationsJsonInfo.getJSONObject(i);

						// ID interno
						idList.add(variationJsonObject.getString("catentry_id").trim());
					}
				} catch (Exception e) {
					Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
				}
			}
		}
		
		
		return idList;
	}
}
