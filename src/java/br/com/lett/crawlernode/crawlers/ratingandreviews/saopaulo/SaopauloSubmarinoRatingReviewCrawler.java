package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.RatingReviewCrawler;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.models.RatingsReviews;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;

/**
 * Date: 13/12/16
 * @author gabriel
 *
 */
public class SaopauloSubmarinoRatingReviewCrawler extends RatingReviewCrawler {

	public SaopauloSubmarinoRatingReviewCrawler(Session session) {
		super(session);
	}

	@Override
	protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
		RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

		if (isProductPage(session.getOriginalURL())) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			JSONObject embeddedJSONObject = crawlEmbeddedJSONObject(document);
			RatingsReviews ratingReviews = crawlRatingReviews(embeddedJSONObject);

			List<String> idList = crawlIdList(embeddedJSONObject);
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
		if (url.startsWith("http://www.submarino.com.br/produto/")){
			return true;
		}

		return false;
	}

	private List<String> crawlIdList(JSONObject embeddedJSONObject) {
		List<String> idList = new ArrayList<String>();
		String internalPid = crawlSkuInternalPid(embeddedJSONObject);

		if (embeddedJSONObject.has("skus")) {
			JSONArray skus = embeddedJSONObject.getJSONArray("skus");

			for (int i = 0; i < skus.length(); i++) {
				JSONObject sku = skus.getJSONObject(i);

				if (sku.has("id")) {
					String id = internalPid + "-" + sku.getString("id");
					idList.add(id);
				}
			}
		}

		return idList;
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
	private RatingsReviews crawlRatingReviews(JSONObject embeddedJSONObject) {
		RatingsReviews ratingReviews = new RatingsReviews();

		ratingReviews.setDate(session.getDate());

		String bazaarVoicePassKey = crawlBazaarVoiceEndpointPassKey(embeddedJSONObject);
		String skuInternalPid = crawlSkuInternalPid(embeddedJSONObject);

		String endpointRequest = assembleBazaarVoiceEndpointRequest(skuInternalPid, bazaarVoicePassKey, 0, 5);

		JSONObject ratingReviewsEndpointResponse = DataFetcher.fetchJSONObject(DataFetcher.GET_REQUEST, session, endpointRequest, null, null);

		JSONObject reviewStatistics = getReviewStatisticsJSON(ratingReviewsEndpointResponse, skuInternalPid);

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
			String bazaarVoiceEnpointPassKey,
			Integer offset,
			Integer limit) {

		StringBuilder request = new StringBuilder();

		request.append("http://api.bazaarvoice.com/data/reviews.json?apiversion=5.4");
		request.append("&passkey=" + bazaarVoiceEnpointPassKey);
		request.append("&Offset=" + offset);
		request.append("&Limit=" + limit);
		request.append("&Sort=SubmissionTime:desc");
		request.append("&Filter=ProductId:" + skuInternalPid);
		request.append("&Include=Products");
		request.append("&Stats=Reviews");

		return request.toString();
	}

	/**
	 * Crawl the bazaar voice endpoint passKey on the sku page.
	 * The passKey is located inside a script tag, which contains
	 * a json object is several metadata, including the passKey.
	 * 
	 * @param document
	 * @return
	 */
	private String crawlBazaarVoiceEndpointPassKey(JSONObject embeddedJSONObject) {
		String passKey = null;		
		if (embeddedJSONObject != null && embeddedJSONObject.has("configuration")) {
			JSONObject configuration = embeddedJSONObject.getJSONObject("configuration");

			if (configuration.has("bazaarvoicePasskey")) {
				passKey = configuration.getString("bazaarvoicePasskey");
			}
		}		
		return passKey;
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

	/**
	 * Crawl an embedded JSONObject, inside a script html tag
	 * which contains several metadata, including the passKey
	 * and product id to perform requests on bazaar voice API.
	 * 
	 * e.g:
	 * 
	 * window.__INITIAL_STATE__ = {"configuration":{"warrantyBaseUrl":"http://www.americanas.com.br/garantia","brandId":"02","freight":{"XP":"A Jato"},
	 * "beacon":"http://img.americanas.com.br/mktacom/beacon/beacon.js",
	 * "domain":"www.americanas.com.br",
	 * "neemuGravaUrl":"https://laas.americanas.com.br/acom/grava.php",
	 * "opns":["YSMESP","YYNKZB","YYNKZU","FACEBOOKADS","FACEBOOKDPA","YSMESC"],
	 * "fullstory":{"enabled":true,"cookieNick":"acomNick"},
	 * ...
	 * "bazaarvoicePasskey":"oqu6lchjs2mb5jp55bl55ov0d",
	 * ...
	 * };
	 * 
	 * @param document
	 * @return
	 */
	private JSONObject crawlEmbeddedJSONObject(Document document) {
		Elements scriptTags = document.getElementsByTag("script");
		JSONObject embeddedJSONObject = null;

		for (Element tag : scriptTags) {                
			for (DataNode node : tag.dataNodes()) {
				if(tag.html().trim().startsWith("window.__INITIAL_STATE__ = ")) {
					embeddedJSONObject = new JSONObject
							(node.getWholeData().split(Pattern.quote("window.__INITIAL_STATE__ = "))[1] +
									node.getWholeData().split(Pattern.quote("window.__INITIAL_STATE__ = "))[1].split(Pattern.quote("};"))[0]
									);
				}
			}        
		}

		if (embeddedJSONObject == null) {
			embeddedJSONObject = new JSONObject();
		}

		return embeddedJSONObject;
	}

	/**
	 * 
	 * @param embeddedJSONObject
	 * @return
	 */
	private String crawlSkuInternalPid(JSONObject embeddedJSONObject) {
		String skuInternalPid = null;

		if (embeddedJSONObject.has("skus")) {
			JSONArray skus = embeddedJSONObject.getJSONArray("skus");

			if (skus.length() > 0) {
				JSONObject sku = skus.getJSONObject(0);

				if (sku.has("_embedded")) {
					JSONObject embedded = sku.getJSONObject("_embedded");

					if (embedded.has("product")) {
						JSONObject product = embedded.getJSONObject("product");

						if (product.has("id")) {
							skuInternalPid = product.getString("id");
						}
					}
				}
			}
		}

		return skuInternalPid;
	}

}
