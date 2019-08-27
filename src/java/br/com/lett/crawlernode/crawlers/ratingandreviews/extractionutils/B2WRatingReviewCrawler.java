package br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import com.google.common.net.HttpHeaders;
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
import models.AdvancedRatingReview;
import models.RatingsReviews;

public class B2WRatingReviewCrawler extends RatingReviewCrawler {

  public B2WRatingReviewCrawler(Session session) {
    super(session);
    super.config.setFetcher(FetchMode.FETCHER);
  }

  protected String homePage;

  @Override
  protected Document fetch() {
    return Jsoup.parse(fetchPage(session.getOriginalURL(), session));
  }

  public String fetchPage(String url, Session session) {
    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.REFERER, this.homePage);
    headers.put(
        HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3"
    );
    headers.put(HttpHeaders.CACHE_CONTROL, "max-age=0");
    headers.put(HttpHeaders.CONNECTION, "keep-alive");
    headers.put(HttpHeaders.ACCEPT_LANGUAGE, "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
    headers.put(HttpHeaders.ACCEPT_ENCODING, "no");
    headers.put("Upgrade-Insecure-Requests", "1");

    Request request = RequestBuilder.create()
        .setUrl(url)
        .setCookies(this.cookies)
        .setHeaders(headers)
        .mustSendContentEncoding(false)
        .setFetcheroptions(
            FetcherOptionsBuilder.create()
                .mustUseMovingAverage(false)
                .setForbiddenCssSelector("#px-captcha")
                .build())
        .setProxyservice(
            Arrays.asList(
                ProxyCollection.INFATICA_RESIDENTIAL_BR,
                ProxyCollection.STORM_RESIDENTIAL_US,
                ProxyCollection.BUY))
        .build();

    String content = this.dataFetcher.get(session, request).getBody();

    if (content == null || content.isEmpty()) {
      content = new ApacheDataFetcher().get(session, request).getBody();
    }

    return content;
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    JSONObject frontPageJson = SaopauloB2WCrawlersUtils.getDataLayer(document);
    JSONObject infoProductJson = SaopauloB2WCrawlersUtils.assembleJsonProductWithNewWay(frontPageJson);

    if (infoProductJson.has("skus") && session.getOriginalURL().startsWith(this.homePage)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = crawlRatingReviews(frontPageJson);

      Map<String, String> skuOptions = this.crawlSkuOptions(infoProductJson, document);

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

  private Map<String, String> crawlSkuOptions(JSONObject infoProductJson, Document doc) {
    Map<String, String> skuMap = new HashMap<>();

    boolean unnavailablePage = !doc.select("#title-stock").isEmpty();

    if (infoProductJson.has("skus")) {
      JSONArray skus = infoProductJson.getJSONArray("skus");

      for (int i = 0; i < skus.length(); i++) {
        JSONObject sku = skus.getJSONObject(i);

        if (sku.has("internalId")) {
          String internalId = sku.getString("internalId");
          StringBuilder name = new StringBuilder();

          String variationName = "";
          if (sku.has("variationName")) {
            variationName = sku.getString("variationName");
          }

          String varationNameWithoutVolts = variationName.replace("volts", "").trim();

          if (unnavailablePage || (variationName.isEmpty() && skus.length() < 2) && infoProductJson.has("name")) {
            name.append(infoProductJson.getString("name"));
          } else if (sku.has("name")) {
            name.append(sku.getString("name"));

            if (!name.toString().toLowerCase().contains(varationNameWithoutVolts.toLowerCase())) {
              name.append(" " + variationName);
            }
          }

          skuMap.put(internalId, name.toString().trim());
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
    AdvancedRatingReview advancedRatingReview = getTotalStarsFromEachValue(reviewStatistics);

    Integer totalRating = getTotalReviewCount(reviewStatistics);

    ratingReviews.setAdvancedRatingReview(advancedRatingReview);
    ratingReviews.setTotalRating(totalRating);
    ratingReviews.setTotalWrittenReviews(totalRating);
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

  private AdvancedRatingReview getTotalStarsFromEachValue(JSONObject reviewStatistics) {
    Integer star1 = 0;
    Integer star2 = 0;
    Integer star3 = 0;
    Integer star4 = 0;
    Integer star5 = 0;

    if (reviewStatistics.has("RatingDistribution")) {
      JSONArray ratingDistribution = reviewStatistics.getJSONArray("RatingDistribution");
      for (Object object : ratingDistribution) {
        JSONObject rating = (JSONObject) object;
        Integer option = CrawlerUtils.getIntegerValueFromJSON(rating, "RatingValue", 0);

        if (rating.has("RatingValue") && option == 1 && rating.has("Count")) {
          star1 = CrawlerUtils.getIntegerValueFromJSON(rating, "Count", 0);
        }

        if (rating.has("RatingValue") && option == 2 && rating.has("Count")) {
          star2 = CrawlerUtils.getIntegerValueFromJSON(rating, "Count", 0);
        }

        if (rating.has("RatingValue") && option == 3 && rating.has("Count")) {
          star3 = CrawlerUtils.getIntegerValueFromJSON(rating, "Count", 0);
        }

        if (rating.has("RatingValue") && option == 4 && rating.has("Count")) {
          star4 = CrawlerUtils.getIntegerValueFromJSON(rating, "Count", 0);
        }

        if (rating.has("RatingValue") && option == 5 && rating.has("Count")) {
          star5 = CrawlerUtils.getIntegerValueFromJSON(rating, "Count", 0);
        }

      }
    }

    return new AdvancedRatingReview.Builder().totalStar1(star1).totalStar2(star2).totalStar3(star3).totalStar4(star4).totalStar5(star5).build();
  }
}
