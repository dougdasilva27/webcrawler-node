package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SaopauloB2WCrawlersUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

/**
 * Date: 13/12/16
 * 
 * @author gabriel
 *
 */
public class SaopauloShoptimeRatingReviewCrawler extends RatingReviewCrawler {

  public SaopauloShoptimeRatingReviewCrawler(Session session) {
    super(session);
    super.config.setFetcher(FetchMode.FETCHER);
  }

  @Override
  protected Document fetch() {
    return Jsoup.parse(fetchPage(session.getOriginalURL(), session));
  }

  public String fetchPage(String url, Session session) {
    Map<String, String> headers = new HashMap<>();
    headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/apng,*/*;q=0.8");
    headers.put("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
    headers.put("Accept-Encoding", "");

    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).mustSendContentEncoding(false)
        .setSendUserAgent(false).setFetcheroptions(FetcherOptionsBuilder.create().mustUseMovingAverage(false).build())
        .setProxyservice(Arrays.asList(ProxyCollection.STORM_RESIDENTIAL_EU, ProxyCollection.BUY)).build();

    String content = this.dataFetcher.get(session, request).getBody();

    if (content == null || content.isEmpty()) {
      content = new ApacheDataFetcher().get(session, request).getBody();
    }

    return content;
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(session.getOriginalURL())) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      JSONObject frontPageJson = SaopauloB2WCrawlersUtils.getDataLayer(document);
      JSONObject infoProductJson = SaopauloB2WCrawlersUtils.assembleJsonProductWithNewWay(frontPageJson);

      RatingsReviews ratingReviews = crawlRatingReviews(frontPageJson);

      Map<String, String> skuOptions = this.crawlSkuOptions(infoProductJson);

      for (String internalId : skuOptions.keySet()) {
        RatingsReviews clonedRatingReviews = ratingReviews.clone();
        clonedRatingReviews.setInternalId(internalId);
        ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return ratingReviewsCollection;
  }

  private boolean isProductPage(String url) {
    if (url.startsWith("https://www.shoptime.com.br/produto/") || url.startsWith("http://www.shoptime.com.br/produto/")) {
      return true;
    }
    return false;
  }

  private Map<String, String> crawlSkuOptions(JSONObject infoProductJson) {
    Map<String, String> skuMap = new HashMap<>();

    if (infoProductJson.has("skus")) {
      JSONArray skus = infoProductJson.getJSONArray("skus");

      for (int i = 0; i < skus.length(); i++) {
        JSONObject sku = skus.getJSONObject(i);

        if (sku.has("internalId")) {
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
   * Crawl rating and reviews stats using the bazaar voice endpoint. To get only the stats summary we
   * need at first, we only have to do one request. If we want to get detailed information about each
   * review, we must perform pagination.
   * 
   * The RatingReviews crawled in this method, is the same across all skus variations in a page.
   *
   * @param document
   * @return
   */
  private RatingsReviews crawlRatingReviews(JSONObject frontPageJson) {
    RatingsReviews ratingReviews = new RatingsReviews();

    ratingReviews.setDate(session.getDate());

    String bazaarVoicePassKey = crawlBazaarVoiceEndpointPassKey(frontPageJson);
    String skuInternalPid = crawlSkuInternalPid(frontPageJson);

    String endpointRequest = assembleBazaarVoiceEndpointRequest(skuInternalPid, bazaarVoicePassKey, 0, 5);

    Request request = RequestBuilder.create().setUrl(endpointRequest).setCookies(cookies).build();
    JSONObject ratingReviewsEndpointResponse = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());


    JSONObject reviewStatistics = getReviewStatisticsJSON(ratingReviewsEndpointResponse, skuInternalPid);

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
   * Crawl the bazaar voice endpoint passKey on the sku page. The passKey is located inside a script
   * tag, which contains a json object is several metadata, including the passKey.
   * 
   * @param document
   * @return
   */
  private String crawlBazaarVoiceEndpointPassKey(JSONObject embeddedJSONObject) {
    String passKey = null;
    if (embeddedJSONObject != null) {
      if (embeddedJSONObject.has("configuration")) {
        JSONObject configuration = embeddedJSONObject.getJSONObject("configuration");

        if (configuration.has("bazaarvoicePasskey")) {
          passKey = configuration.getString("bazaarvoicePasskey");
        }
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
