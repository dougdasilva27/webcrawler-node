package br.com.lett.crawlernode.crawlers.ratingandreviews.mexico;

import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

/**
 * 
 * @author gabriel
 * @date 17/07/2017
 * 
 *       Example of product with rating:
 *       https://www.superama.com.mx/catalogo/d-jugos-y-bebidas/f-cafe-preparado/l-capsulas-de-cafe/capsulas-de-cafe-nescafe-dolce-gusto-espresso-intenso-16-pzas/0761303152640
 *
 */
public class MexicoSuperamaRatingReviewCrawler extends RatingReviewCrawler {

  public MexicoSuperamaRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(document)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
      String internalId = crawlInternalId(document);
      RatingsReviews ratingReviews = crawlRatingReviews(internalId);

      ratingReviews.setInternalId(internalId);
      ratingReviewsCollection.addRatingReviews(ratingReviews);
    } else {
      Logging.printLogDebug(logger, session, "Not a product page: " + this.session.getOriginalURL());
    }

    return ratingReviewsCollection;
  }

  private boolean isProductPage(Document doc) {
    return !doc.select("#upcProducto").isEmpty();
  }

  private String crawlInternalId(Document document) {
    String internalId = null;

    Element internalIdElement = document.select("#upcProducto").first();
    if (internalIdElement != null) {
      internalId = internalIdElement.attr("value");
    }

    return internalId;
  }

  /**
   * Crawl rating and reviews stats using the bazaar voice endpoint.
   *
   * @param document
   * @return
   */
  private RatingsReviews crawlRatingReviews(String internalId) {
    RatingsReviews ratingReviews = new RatingsReviews();
    ratingReviews.setDate(session.getDate());

    String bazaarVoicePassKey = "ca00NLtrMkSnTddOCbktCnskwSV7OaQHCOTa3EZNMR2KE";
    String endpointRequest = assembleBazaarVoiceEndpointRequest(internalId, bazaarVoicePassKey, 0, 5);

    Request request = RequestBuilder.create().setUrl(endpointRequest).setCookies(cookies).build();
    JSONObject ratingReviewsEndpointResponse = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
    JSONObject reviewStatistics = getReviewStatisticsJSON(ratingReviewsEndpointResponse, internalId);

    ratingReviews.setTotalRating(getTotalReviewCount(reviewStatistics));
    ratingReviews.setAverageOverallRating(getAverageOverallRating(reviewStatistics));

    return ratingReviews;
  }

  private Integer getTotalReviewCount(JSONObject reviewStatistics) {
    Integer totalReviewCount = 0;
    if (reviewStatistics.has("TotalReviewCount")) {
      totalReviewCount = reviewStatistics.getInt("TotalReviewCount");
    }
    return totalReviewCount;
  }

  private Double getAverageOverallRating(JSONObject reviewStatistics) {
    Double avgOverallRating = 0d;
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


  private JSONObject getReviewStatisticsJSON(JSONObject ratingReviewsEndpointResponse, String internalId) {
    if (ratingReviewsEndpointResponse.has("Includes")) {
      JSONObject includes = ratingReviewsEndpointResponse.getJSONObject("Includes");

      if (includes.has("Products")) {
        JSONObject products = includes.getJSONObject("Products");

        if (products.has(internalId)) {
          JSONObject product = products.getJSONObject(internalId);

          if (product.has("ReviewStatistics")) {
            return product.getJSONObject("ReviewStatistics");
          }
        }
      }
    }

    return new JSONObject();
  }

}
