package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.models.RatingsReviews;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SaopauloB2WCrawlersUtils;
import br.com.lett.crawlernode.util.Logging;

/**
 * Date: 13/12/16
 * @author gabriel
 *
 */
public class SaopauloShoptimeRatingReviewCrawler extends RatingReviewCrawler {

	public SaopauloShoptimeRatingReviewCrawler(Session session) {
		super(session);
	}

	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

		if (isProductPage(session.getOriginalURL())) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
			
			// Api onde se consegue todos os preços
			JSONObject initialJson = SaopauloB2WCrawlersUtils.getDataLayer(document);
			
			// Pega só o que interessa do json da api
			JSONObject infoProductJson = SaopauloB2WCrawlersUtils.assembleJsonProductWithNewWay(initialJson);
			
			String internalPid = crawlInternalPid(infoProductJson);
			
			RatingsReviews ratingReviews = crawlRatingReviews(internalPid);

			List<String> idList = crawlIdList(infoProductJson);
			for (String internalId : idList) {
				RatingsReviews clonedRatingReviews = (RatingsReviews)ratingReviews.clone();
				clonedRatingReviews.setInternalId(internalId);
				ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
			}

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return ratingReviewsCollection;
	}

	private boolean isProductPage(String url) {
		if (url.startsWith("https://www.shoptime.com.br/produto/") || url.startsWith("http://www.shoptime.com.br/produto/")){
			return true;
		}

		return false;
	}

	private String crawlInternalPid(JSONObject assembleJsonProduct) {
		String internalPid = null;

		if (assembleJsonProduct.has("internalPid")) {
			internalPid = assembleJsonProduct.getString("internalPid").trim();
		}

		return internalPid;
	}
	
	private List<String> crawlIdList(JSONObject infoProductJson) {
		List<String> idList = new ArrayList<>();
		
		// sku data in json
		Map<String,String> skuOptions = crawlSkuOptions(infoProductJson);
		
		for (String internalId : skuOptions.keySet()) {	
			idList.add(internalId);
		}
		
		return idList;
	}

	private Map<String,String> crawlSkuOptions(JSONObject infoProductJson){
		Map<String,String> skuMap = new HashMap<>();

		if(infoProductJson.has("skus")){
			JSONArray skus = infoProductJson.getJSONArray("skus");

			for(int i = 0; i < skus.length(); i++){
				JSONObject sku = skus.getJSONObject(i);

				if(sku.has("internalId")){
					String internalId = sku.getString("internalId");
					String name = "";

					if (sku.has("variationName")) {
						name = sku.getString("variationName");
					}

					skuMap.put(internalId, name);
				}
			}
		}

		return skuMap;
	}
	
	/**
	 * Crawl rating and reviews stats using the bazaar voice endpoint.
	 * To get only the stats summary we need at first, we only have to do
	 * one request. If we want to get detailed information about each review, we must
	 * perform pagination.
	 * 
	 * The RatingReviews crawled in this method, is the same across all skus variations
	 * in a page.
	 *
	 * @param document
	 * @return
	 */
	private RatingsReviews crawlRatingReviews(String internalPid) {
		RatingsReviews ratingReviews = new RatingsReviews();

		ratingReviews.setDate(session.getDate());

		String bazaarVoicePassKey = crawlBazaarVoiceEndpointPassKey();
		String endpointRequest = assembleBazaarVoiceEndpointRequest(internalPid, bazaarVoicePassKey);

		JSONObject ratingReviewsEndpointResponse = DataFetcher.fetchJSONObject(DataFetcher.GET_REQUEST, session, endpointRequest, null, null);	
		JSONObject reviewStatistics = getReviewStatisticsJSON(ratingReviewsEndpointResponse, internalPid);
		
		ratingReviews.setTotalReviews(getTotalReviewCount(reviewStatistics));
		ratingReviews.setAverageOverallRating(getAverageOverallRating(reviewStatistics));

		return ratingReviews;
	}

	private Integer getTotalReviewCount(JSONObject reviewStatistics) {
		Integer totalReviewCount = null;
		if (reviewStatistics.has("TotalReviewCount")) {
			totalReviewCount = reviewStatistics.getInt("TotalReviewCount");
		}
		return totalReviewCount;
	}

	private Double getAverageOverallRating(JSONObject reviewStatistics) {
		Double avgOverallRating = null;
		if (reviewStatistics.has("AverageOverallRating")) {
			avgOverallRating = reviewStatistics.getDouble("AverageOverallRating");
		}
		return avgOverallRating;
	}

	/**
	 * e.g: 
	 * http://api.bazaarvoice.com/data/reviews.json?apiversion=5.4
	 * &passkey=oqu6lchjs2mb5jp55bl55ov0d
	 * &Offset=0
	 * &Limit=5
	 * &Sort=SubmissionTime:desc
	 * &Filter=ProductId:113048617
	 * &Include=Products
	 * &Stats=Reviews
	 * 
	 * Endpoint request parameters:
	 * <p>
	 * &passKey: the password used to request the bazaar voice endpoint.
	 * This pass key e crawled inside the html of the sku page, inside a script tag.
	 * More details on how to crawl this passKey
	 * </p>
	 * <p>
	 * &Offset: the number of the chunk of data retrieved by the endpoint. If
	 * we want the second chunk, we must add this value by the &Limit parameter.
	 * </p>
	 * <p>
	 * &Limit: the number of reviews that a request will return, at maximum.
	 * </p>
	 * 
	 * The others parameters we left as default.
	 * 
	 * Request Method: GET
	 */
	private String assembleBazaarVoiceEndpointRequest(
			String skuInternalPid,
			String bazaarVoiceEnpointPassKey) {

		StringBuilder request = new StringBuilder();

		request.append("http://api.bazaarvoice.com/data/reviews.json?apiversion=5.4");
		request.append("&passkey=" + bazaarVoiceEnpointPassKey);
		request.append("&Filter=ProductId:" + skuInternalPid);
		request.append("&Include=Products");
		request.append("&Stats=Reviews");

		return request.toString();
	}

	/**
	 * Comment on other b2w markets:
	 * 
	 *  Crawl the bazaar voice endpoint passKey on the sku page.
	 * The passKey is located inside a script tag, which contains
	 * a json object is several metadata, including the passKey.
	 * 
	 * Shoptime:
	 * 
	 * In this market the key was not found in json,
	 * but it was noticed that it does not change.
	 * 
	 * Default key: u8r57b32b8bf7n12fmvnj8mjm
	 * 
	 * 
	 * @param document
	 * @return
	 */
	private String crawlBazaarVoiceEndpointPassKey() { 	
		return "u8r57b32b8bf7n12fmvnj8mjm";
	}

	private JSONObject getReviewStatisticsJSON(JSONObject ratingReviewsEndpointResponse, String skuInternalPid) {
		if (ratingReviewsEndpointResponse.has("Includes")) {
			JSONObject includes = ratingReviewsEndpointResponse.getJSONObject("Includes");

			if (includes.has("Products")) {
				JSONObject products = includes.getJSONObject("Products");

				if (products.has(skuInternalPid)) {
					JSONObject product = products.getJSONObject(skuInternalPid);

					if (product.has("ReviewStatistics")) {
						return product.getJSONObject("ReviewStatistics");
					}
				}
			}
		}

		return new JSONObject();
	}
}
