package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.models.RatingsReviews;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;

/**
 * Date: 14/12/16
 * @author gabriel
 *
 */
public class BrasilMagazineluizaRatingReviewCrawler extends RatingReviewCrawler {

	public BrasilMagazineluizaRatingReviewCrawler(Session session) {
		super(session);
	}

	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

		if (isProductPage(session.getOriginalURL())) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
			
			String internalPid = crawlInternalPid(document);
			RatingsReviews ratingReviews = crawlRatingReviews(internalPid);

			List<String> idList = crawlIdList(document, internalPid);
			
			for (String internalId : idList) {
				RatingsReviews clonedRatingReviews = (RatingsReviews)ratingReviews.clone();
				clonedRatingReviews.setInternalId(internalId);
				ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
			}
		}

		return ratingReviewsCollection;

	}
	
	private String crawlInternalPid(Document doc){
		String internalPid = null;
		
		Element pidElement = doc.select("small[itemprop=productID]").first();
		if(pidElement != null){
			int begin = pidElement.text().indexOf(".com") + 4;
			internalPid = pidElement.text().substring(begin).replace(")", "").trim();
		}
		
		return internalPid;
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

		ratingReviews.setTotalRating(getTotalReviewCount(reviewStatistics));
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
	 * http://api.bazaarvoice.com/data/batch.json
	 * ?passkey=il42j5b24bkrm28v8zbo93vo1
	 * &apiversion=5.5
	 * &filter.q0=productid:eq:1933843
	 * &resource.q0=reviews
	 * &stats.q0=reviews
	 * &filteredstats.q0=reviews
	 * &include.q0=products
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

		request.append("http://api.bazaarvoice.com/data/batch.json?apiversion=5.5");
		request.append("&passkey=" + bazaarVoiceEnpointPassKey);
		request.append("&filter.q0=productid:eq:" + skuInternalPid);
		request.append("&resource.q0=reviews");
		request.append("&stats.q0=reviews");
		request.append("&filteredstats.q0=reviews");
		request.append("&include.q0=products");

		return request.toString();
	}

	/**
	 * Comment on other markets with bazaar api:
	 * 
	 *  Crawl the bazaar voice endpoint passKey on the sku page.
	 * The passKey is located inside a script tag, which contains
	 * a json object is several metadata, including the passKey.
	 * 
	 * Magazineluiza:
	 * 
	 * In this market the key was not found in product page,
	 * but it was noticed that it does not change.
	 * 
	 * Default key: il42j5b24bkrm28v8zbo93vo1
	 * 
	 * 
	 * @param document
	 * @return
	 */
	private String crawlBazaarVoiceEndpointPassKey() { 	
		return "il42j5b24bkrm28v8zbo93vo1";
	}

	private JSONObject getReviewStatisticsJSON(JSONObject ratingReviewsEndpointResponse, String skuInternalPid) {
		if(ratingReviewsEndpointResponse.has("BatchedResults")){
			JSONObject batchedResults = ratingReviewsEndpointResponse.getJSONObject("BatchedResults");
			
			if(batchedResults.has("q0")){
				JSONObject q0 = batchedResults.getJSONObject("q0");
				
				if (q0.has("Includes")) {
					JSONObject includes = q0.getJSONObject("Includes");

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
			}
		}

		return new JSONObject();
	}
	
	private List<String> crawlIdList(Document doc, String internalPid) {
		List<String> idList = new ArrayList<>();
		
		if(internalPid != null) {						
			
			idList.add(internalPid);
			
		}
		
		return idList;
	}
	
	private boolean isProductPage(String url) {
		return url.contains("/p/") || url.contains("/p1/");
	}

}
