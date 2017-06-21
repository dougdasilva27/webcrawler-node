package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.fetcher.methods.POSTFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.models.RatingsReviews;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

/**
 * Date: 14/12/16
 * @author gabriel
 *
 * In time crawler was made, there was no rating on any product in this market
 *
 */
public class BrasilDrogariapachecoRatingReviewCrawler extends RatingReviewCrawler {

	public BrasilDrogariapachecoRatingReviewCrawler(Session session) {
		super(session);
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
				
				Document docRating = crawlPageRatings(session.getOriginalURL(), internalPid);
			
				Integer totalNumOfEvaluations = getTotalNumOfRatings(docRating);			
				Double avgRating = getTotalAvgRating(docRating, totalNumOfEvaluations);
				
				ratingReviews.setTotalRating(totalNumOfEvaluations);
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
	 * Url: http://www.drogariaspacheco.com.br/userreview
	 * Ex payload: productId=542865&productLinkId=fralda-descartavel-pampers-premium-care-xxg-32-unidades
	 * Required headers to crawl this api
	 * Ex:
	 * Média de avaliações: 5 votos
	 *
	 *	3 Votos
	 *	nenhum voto
	 *	1 Voto
	 *	1 Voto
	 *	nenhum voto
	 * 
	 * 
	 * @param url
	 * @param internalPid
	 * @return document 
	 */
	private Document crawlPageRatings(String url, String internalPid){
		Document doc = new Document(url);
		
		// Parameter in url for request POST ex: "led-32-ilo-hd-smart-d300032-" IN URL "http://www.walmart.com.ar/led-32-ilo-hd-smart-d300032-/p"
		String[] tokens = url.split("/");
		String productLinkId = tokens[tokens.length-2];
		
		String payload = "productId=" + internalPid + "&productLinkId=" + productLinkId;
		
		Map<String,String> headers = new HashMap<>();
		headers.put("Content-Type", "application/x-www-form-urlencoded");
		headers.put("Accept-Language", "pt-BR,pt;q=0.8,en-US;q=0.6,en;q=0.4");
		
		String response = POSTFetcher.fetchPagePOSTWithHeaders("http://www.drogariaspacheco.com.br/userreview", session, payload, cookies, 1, headers);
		
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
		Double avgRating = null;
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
	 * Number of ratings appear in rating page 
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
		Element body = document.select("body").first();
		Element elementId = document.select("div.productReference").first();
		
		return body.hasClass("produto") && elementId != null;
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
