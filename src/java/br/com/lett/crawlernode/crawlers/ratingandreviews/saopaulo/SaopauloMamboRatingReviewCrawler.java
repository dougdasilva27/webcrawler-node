package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.models.RatingsReviews;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

public class SaopauloMamboRatingReviewCrawler extends RatingReviewCrawler {

	public SaopauloMamboRatingReviewCrawler(Session session) {
		super(session);
	}

	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

		if (isProductPage(document)) {
			RatingsReviews ratingReviews = new RatingsReviews();
						
			ratingReviews.setDate(session.getDate());
			ratingReviews.setInternalId(crawlInternalId(document));
			
			JSONObject skuJson = crawlSkuJson(document);
			
			if (skuJson.has("productId")) {
				JSONObject trustVoxResponse = requestTrustVoxEndpoint(skuJson.getInt("productId"));
				
				Integer totalNumOfEvaluations = getTotalNumOfRatings(trustVoxResponse);
				Double totalRating = getTotalRating(trustVoxResponse);
				
				Double avgRating = null;
				if (totalNumOfEvaluations > 0) {
					avgRating = MathCommonsMethods.normalizeTwoDecimalPlaces(totalRating / totalNumOfEvaluations);
				}
				
				ratingReviews.setTotalReviews(totalNumOfEvaluations);
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
	
	private JSONObject requestTrustVoxEndpoint(int id) {
		StringBuilder requestURL = new StringBuilder();
		
		requestURL.append("http://trustvox.com.br/widget/opinions?code=");
		requestURL.append(id);
		
		requestURL.append("&");
		requestURL.append("store_id=944");
		
		requestURL.append("&");
		requestURL.append(session.getOriginalURL());
				
		Map<String, String> headerMap = new HashMap<>();
		headerMap.put(DataFetcher.HTTP_HEADER_ACCEPT, "application/vnd.trustvox-v2+json");
		headerMap.put(DataFetcher.HTTP_HEADER_CONTENT_TYPE, "application/json; charset=utf-8");
		
		String response = DataFetcher.fetchPageGETWithHeaders(session, requestURL.toString(), null, headerMap, 1);
		
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
		return document.select(".hidden-sku-default").first() != null;
	}

	private String crawlInternalId(Document doc) {
		String internalId = null;
		Element elementId = doc.select(".hidden-sku-default").first();
		if (elementId != null) {
			internalId = Integer.toString(Integer.parseInt(elementId.text().trim().replaceAll("[^0-9,]+", "").replaceAll("\\.", "")));
		}

		return internalId;
	}
	
	private JSONObject crawlSkuJson(Document document) {
		Elements scriptTags = document.getElementsByTag("script");
		String scriptVariableName = "var skuJson_0 = ";
		JSONObject skuJson;
		String skuJsonString = null;
		
		for (Element tag : scriptTags){                
			for (DataNode node : tag.dataNodes()) {
				if(tag.html().trim().startsWith(scriptVariableName)) {
					skuJsonString =
							node.getWholeData().split(Pattern.quote(scriptVariableName))[1] +
							node.getWholeData().split(Pattern.quote(scriptVariableName))[1].split(Pattern.quote("};"))[0];
					break;
				}
			}        
		}
		
		try {
			skuJson = new JSONObject(skuJsonString);
		} catch (JSONException e) {
			Logging.printLogError(logger, session, "Error creating JSONObject from var skuJson_0");
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
			
			skuJson = new JSONObject();
		}
		
		return skuJson;
	}

}
