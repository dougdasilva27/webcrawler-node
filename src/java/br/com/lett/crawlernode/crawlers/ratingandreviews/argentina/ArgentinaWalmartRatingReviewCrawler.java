package br.com.lett.crawlernode.crawlers.ratingandreviews.argentina;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.RatingReviewCrawler;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.models.RatingsReviews;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;


/**
 * Date: 11/12/16
 * @author gabriel
 *
 */
public class ArgentinaWalmartRatingReviewCrawler extends RatingReviewCrawler {

	public ArgentinaWalmartRatingReviewCrawler(Session session) {
		super(session);
	}

	/**
	 * To acess product page is required put ?sc=15 in url
	 */
	@Override
	public String handleURLBeforeFetch(String curURL) {

		if(curURL.endsWith("/p")) {

			try {
				String url = curURL;
				List<NameValuePair> paramsOriginal = URLEncodedUtils.parse(new URI(url), "UTF-8");
				List<NameValuePair> paramsNew = new ArrayList<>();

				for (NameValuePair param : paramsOriginal) {
					if (!"sc".equals(param.getName())) {
						paramsNew.add(param);
					}
				}

				paramsNew.add(new BasicNameValuePair("sc", "15"));
				URIBuilder builder = new URIBuilder(curURL.split("\\?")[0]);

				builder.clearParameters();
				builder.setParameters(paramsNew);

				curURL = builder.build().toString();

				return curURL;

			} catch (URISyntaxException e) {
				return curURL;
			}
		}

		return curURL;

	}
	
	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

		if (isProductPage(document)) {
			RatingsReviews ratingReviews = new RatingsReviews();			
			ratingReviews.setDate(session.getDate());
			
			JSONObject skuJson = crawlSkuJson(document);
			
			if (skuJson.has("productId")) {
				String internalPid = Integer.toString(skuJson.getInt("productId"));
				
				Document docRating = crawlApiRatings(session.getOriginalURL(), internalPid);
			
				Integer totalNumOfEvaluations = getTotalNumOfRatings(docRating);			
				Double avgRating = getTotalAvgRating(docRating, totalNumOfEvaluations);
				
				ratingReviews.setTotalReviews(totalNumOfEvaluations);
				ratingReviews.setAverageOverallRating(avgRating);
			
				List<String> idList = crawlIdList(skuJson);
				for (String internalId : idList) {
					RatingsReviews clonedRatingReviews = (RatingsReviews)ratingReviews.clone();
					clonedRatingReviews.setInternalId(internalId);
					ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
				}
		
			}
			
		}

		return ratingReviewsCollection;

	}
	
	/**
	 * Api Ratings
	 * Url: http://www.walmart.com.ar/userreview
	 * Ex payload: productId=8213&productLinkId=home-theater-5-1-microlab-m-710u51
	 * Required headers to crawl this api
	 * 
	 * @param url
	 * @param internalPid
	 * @return document 
	 */
	private Document crawlApiRatings(String url, String internalPid){
		Document doc = new Document(url);
		
		// Parameter in url for request POST ex: "led-32-ilo-hd-smart-d300032-" IN URL "http://www.walmart.com.ar/led-32-ilo-hd-smart-d300032-/p"
		String[] tokens = url.split("/");
		String productLinkId = tokens[tokens.length-2];
		
		String payload = "productId=" + internalPid + "&productLinkId=" + productLinkId;
		
		Map<String,String> headers = new HashMap<>();
		headers.put("Content-Type", "application/x-www-form-urlencoded");
		headers.put("Accept-Language", "pt-BR,pt;q=0.8,en-US;q=0.6,en;q=0.4");
		
		String response = DataFetcher.fetchPagePOSTWithHeaders("http://www.walmart.com.ar/userreview", session, payload, cookies, 1, headers);
		
		if(response != null){
			doc = Jsoup.parse(response);
		}
		
		return doc;
	}
	
	/**
	 * Average is calculate 
	 * @param document
	 * @return
	 */
	private Double getTotalAvgRating(Document docRating, Integer totalRating) {
		Double avgRating = 0.0;
		Elements rating = docRating.select("ul.rating li");
		
		if (totalRating != null) {
			Double total = 0.0;
			
			for (Element e : rating) {
				Element star = e.select("strong.rating-demonstrativo").first();
				Element totalStar = e.select("> span:not([class])").first();
				
				if (totalStar != null) {
					String votes = totalStar.text().replaceAll("[^0-9]", "").trim();
					
					if (!votes.isEmpty()) {
						Integer totalVotes = Integer.parseInt(votes);
						if(star != null){
							if(star.hasClass("avaliacao50")){
								total += totalVotes * 5;
							} else if(star.hasClass("avaliacao40")){
								total += totalVotes * 4;
							} else if(star.hasClass("avaliacao30")){
								total += totalVotes * 3;
							} else if(star.hasClass("avaliacao20")){
								total += totalVotes * 2;
							} else if(star.hasClass("avaliacao10")){
								total += totalVotes * 1;
							}
						}
					}
				}
			}
			
			avgRating = MathCommonsMethods.normalizeTwoDecimalPlaces(total / totalRating);
		}
		
		return avgRating;
	}
	
	/**
	 * Number of ratings appear in api 
	 * @param docRating
	 * @return
	 */
	private Integer getTotalNumOfRatings(Document docRating) {
		Integer totalRating = null;
		Element totalRatingElement = docRating.select(".media em > span").first();
		
		if(totalRatingElement != null) {
			String totalText  = totalRatingElement.ownText().replaceAll("[^0-9]", "").trim();
			
			if(!totalText.isEmpty()){
				totalRating = Integer.parseInt(totalText);
			}
		}
		
		return totalRating;
	}
	 

	private List<String> crawlIdList(JSONObject skuJson) {
		List<String> idList = new ArrayList<>();
		
		if (skuJson.has("skus")) {
			JSONArray skus = skuJson.getJSONArray("skus");
			
			for (int i = 0; i < skus.length(); i++) {
				JSONObject sku = skus.getJSONObject(i);
				
				if (sku.has("sku")) {
					idList.add(Integer.toString(sku.getInt("sku")));
				}
			}
		}
		
		return idList;
	}
	
	private boolean isProductPage(Document document) {
		if ( document.select(".productName").first() != null ) {
			return true;
		}
		return false;
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
