package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilFastshopCrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

public class BrasilFastshopRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilFastshopRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    List<String> idList;
    JSONArray jsonArrayInfo = BrasilFastshopCrawlerUtils.crawlSkusInfo(document);
    String partnerId;

    if (jsonArrayInfo.length() > 0) {
      idList = crawlIdList(jsonArrayInfo);
      partnerId = BrasilFastshopCrawlerUtils.crawlPartnerId(document);
    } else {
      partnerId = BrasilFastshopCrawlerUtils.crawlPartnerId(session);
      JSONObject productAPIJSON = BrasilFastshopCrawlerUtils.crawlApiJSON(partnerId, session, cookies);
      JSONArray arraySkus = productAPIJSON.has("voltage") ? productAPIJSON.getJSONArray("voltage") : new JSONArray();

      idList = crawlIdList(arraySkus);
    }

    if (!idList.isEmpty()) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
      RatingsReviews ratingReviews = crawlRatingReviews(partnerId);

      for (String internalId : idList) {
        RatingsReviews clonedRatingReviews = ratingReviews.clone();
        clonedRatingReviews.setInternalId(internalId);
        ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
      }
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return ratingReviewsCollection;
  }

  private List<String> crawlIdList(JSONArray jsonArrayInfo) {
    List<String> idList = new ArrayList<>();

    for (int i = 0; i < jsonArrayInfo.length(); i++) {
      JSONObject productInfo = jsonArrayInfo.getJSONObject(i);

      // InternalId
      idList.add(crawlInternalId(productInfo));
    }

    return idList;
  }

  private String crawlInternalId(JSONObject jsonInfo) {
    String internalId = null;

    if (jsonInfo.has("catentry_id")) {
      internalId = jsonInfo.getString("catentry_id").trim();
    }

    return internalId;
  }

  /**
   * Crawl rating and reviews stats using the bazaar voice endpoint. To get only the stats summary we
   * need at first, we only have to do one request. If we want to get detailed information about each
   * review, we must perform pagination.
   * 
   * The RatingReviews crawled in this method, is the same across all skus variations in a page.
   *
   * @param document
   * @return
   */
  private RatingsReviews crawlRatingReviews(String partnerId) {
    RatingsReviews ratingReviews = new RatingsReviews();

    ratingReviews.setDate(session.getDate());

    final String bazaarVoicePassKey = "caw1ZMlxPTUHLUFtjzQeE602umnQqFlKyTwhRjlDvuTac";
    String endpointRequest = assembleBazaarVoiceEndpointRequest(partnerId, bazaarVoicePassKey, 0, 50);

    JSONObject ratingReviewsEndpointResponse = DataFetcher.fetchJSONObject(DataFetcher.GET_REQUEST, session, endpointRequest, null, null);
    JSONObject reviewStatistics = getReviewStatisticsJSON(ratingReviewsEndpointResponse, partnerId);

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
   * e.g: http://api.bazaarvoice.com/data/reviews.json?apiversion=5.4
   * &passkey=oqu6lchjs2mb5jp55bl55ov0d &Offset=0 &Limit=5 &Sort=SubmissionTime:desc
   * &Filter=ProductId:113048617 &Include=Products &Stats=Reviews
   * 
   * Endpoint request parameters:
   * <p>
   * &passKey: the password used to request the bazaar voice endpoint. This pass key e crawled inside
   * the html of the sku page, inside a script tag. More details on how to crawl this passKey
   * </p>
   * <p>
   * &Offset: the number of the chunk of data retrieved by the endpoint. If we want the second chunk,
   * we must add this value by the &Limit parameter.
   * </p>
   * <p>
   * &Limit: the number of reviews that a request will return, at maximum.
   * </p>
   * 
   * The others parameters we left as default.
   * 
   * Request Method: GET
   */
  private String assembleBazaarVoiceEndpointRequest(String skuInternalPid, String bazaarVoiceEnpointPassKey, Integer offset, Integer limit) {

    StringBuilder request = new StringBuilder();

    request.append("http://api.bazaarvoice.com/data/reviews.json?apiversion=5.5");
    request.append("&passkey=" + bazaarVoiceEnpointPassKey);
    request.append("&Offset=" + offset);
    request.append("&Limit=" + limit);
    request.append("&Sort=SubmissionTime:desc");
    request.append("&Filter=ProductId:" + skuInternalPid);
    request.append("&Include=Products");
    request.append("&Stats=Reviews");

    return request.toString();
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
