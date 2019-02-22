package br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.RatingsReviews;

public class AdidasRatingReviewCrawler extends RatingReviewCrawler {
  private String HOME_PAGE = "";

  public AdidasRatingReviewCrawler(Session session, String HOME_PAGE) {
    super(session);
    this.HOME_PAGE = HOME_PAGE;
  }



  @Override
  protected Document fetch() {
    try {
      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
      headers.put("accept-encoding", "gzip, deflate, br");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
      headers.put("cache-control", "max-age=0");
      headers.put("upgrade-insecure-requests", "1");
      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36");

      return Jsoup.connect(session.getOriginalURL()).headers(headers).get();

    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    }
  }

  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
    String internalPid = crawlInternalPid(document);
    String url = HOME_PAGE + "/api/models/" + internalPid + "/ratings";
    JSONObject ratingJson = fecthJson(url);

    if (isProductPage(document)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      String internalId = crawlInternalPid(document);

      Integer totalNumOfEvaluations = getTotalNumOfRatings(ratingJson);
      Double avgRating = getTotalAvgRating(ratingJson);

      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);

      ratingReviewsCollection.addRatingReviews(ratingReviews);
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return ratingReviewsCollection;

  }

  private Double getTotalAvgRating(JSONObject ratingJson) {
    Double avg = 0d;
    if (ratingJson.has("overallRating")) {
      avg = MathUtils.parseDoubleWithDot(ratingJson.get("overallRating").toString());
      if (avg == null) {
        avg = 0d;
      }
    }
    return avg;
  }

  private Integer getTotalNumOfRatings(JSONObject ratingJson) {
    Integer total = 0;
    Object totalObject;
    if (ratingJson.has("reviewCount")) {
      totalObject = ratingJson.get("reviewCount");

      if (totalObject instanceof Integer) {
        total = Integer.parseInt(totalObject.toString());
      }
    }

    return total;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".pdpBar  > div[data-auto-id=\"product-information\"]") != null;
  }

  private String crawlInternalPid(Document document) {
    String internalPid = null;

    JSONObject scriptApi = CrawlerUtils.selectJsonFromHtml(document, "script", "window.DATA_STORE = ", ";", false, true);
    if (scriptApi.has("product")) {

      JSONObject product = scriptApi.getJSONObject("product");
      if (product.has("model_number")) {
        internalPid = product.get("model_number").toString();
      }
    }

    return internalPid;
  }

  private JSONObject fecthJson(String url) {
    JSONObject jsonSku = new JSONObject();
    Map<String, String> headers = new HashMap<>();
    headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
    headers.put("accept-encoding", "gzip, deflate, br");
    headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
    headers.put("cache-control", "max-age=0");
    headers.put("upgrade-insecure-requests", "1");
    headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36");
    try {
      jsonSku = new JSONObject(Jsoup.connect(url).headers(headers).ignoreContentType(true).execute().body());
    } catch (JSONException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return jsonSku;
  }


}
