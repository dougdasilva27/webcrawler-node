package br.com.lett.crawlernode.crawlers.ratingandreviews.chile;

import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.RatingsReviews;

public class ChileParisRatingReviewCrawler extends RatingReviewCrawler {

  public ChileParisRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(document)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(document);
      String internalPid = CrawlerUtils.scrapStringSimpleInfo(document, ".product-sku", true);

      String endpointRequest = assembleBazaarVoiceEndpointRequest(internalPid, "cawhDUNXMzzke7yV6JTnIiPm8Eh0hP8s7Oqzo57qihXkk");
      Request request = RequestBuilder.create().setUrl(endpointRequest).setCookies(cookies).build();
      JSONObject ratingReviewsEndpointResponse = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
      JSONObject reviewSummary = getReviewSummary(ratingReviewsEndpointResponse);

      Integer numReviews = getNumReviews(reviewSummary);
      Double avg = getNumAvg(reviewSummary);
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      ratingReviews.setInternalId(internalId);
      ratingReviews.setAverageOverallRating(avg);
      ratingReviews.setTotalRating(numReviews);
      ratingReviews.setTotalWrittenReviews(numReviews);

      ratingReviewsCollection.addRatingReviews(ratingReviews);
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return ratingReviewsCollection;
  }

  private Double getNumAvg(JSONObject reviewSummary) {
    Double avg = 0d;
    if (reviewSummary.has("primaryRating")) {
      JSONObject primaryRating = reviewSummary.getJSONObject("primaryRating");
      if (primaryRating.has("average") && !primaryRating.isNull("average")) {
        avg = MathUtils.parseDoubleWithDot(primaryRating.get("average").toString());
      }
    }
    return avg;
  }

  private Integer getNumReviews(JSONObject reviewSummary) {
    return reviewSummary.has("numReviews") ? reviewSummary.getInt("numReviews") : 0;
  }

  private JSONObject getReviewSummary(JSONObject ratingReviewsEndpointResponse) {
    JSONObject reviewSummary = new JSONObject();
    if (ratingReviewsEndpointResponse.has("reviewSummary")) {
      reviewSummary = ratingReviewsEndpointResponse.getJSONObject("reviewSummary");
    }
    return reviewSummary;
  }

  private boolean isProductPage(Document doc) {
    return doc.select("#pdpMain").first() != null;
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    JSONObject product = CrawlerUtils.selectJsonFromHtml(doc, "script", "varproduct=", ";", true, false);
    if (product.has("sku")) {
      String sku = product.get("sku").toString().trim();

      if (!sku.isEmpty()) {
        internalId = sku;
      }
    }

    if (internalId == null && product.has("id")) {
      String id = product.get("id").toString().trim();

      if (!id.isEmpty()) {
        internalId = id;
      }
    }

    return internalId;
  }

  /*
   * https://api.bazaarvoice.com/data/display/0.2alpha/product/summary?PassKey=
   * cawhDUNXMzzke7yV6JTnIiPm8Eh0hP8s7Oqzo57qihXkk&productid=260370999&contentType=reviews,questions&
   * reviewDistribution=primaryRating,recommended&rev=0&contentlocale=es_CL
   */
  private String assembleBazaarVoiceEndpointRequest(String skuInternalPid, String bazaarVoiceEnpointPassKey) {

    StringBuilder request = new StringBuilder();

    request.append("https://api.bazaarvoice.com/data/display/0.2alpha/product/summary");
    request.append("?PassKey=" + bazaarVoiceEnpointPassKey);
    request.append("&productid=" + skuInternalPid);
    request.append("&contentType=reviews,questions");
    request.append("&reviewDistribution=primaryRating,recommended");
    request.append("&rev=0&contentlocale=es_CL");

    return request.toString();
  }
}
