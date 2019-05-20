package br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
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
    super.config.setFetcher(FetchMode.JAVANET);
    this.HOME_PAGE = HOME_PAGE;
  }

  @Override
  protected Document fetch() {
    return Jsoup.parse(fetchApi(session.getOriginalURL()));
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
    String internalPid = crawlInternalPid(document);
    String ratingUrl = HOME_PAGE + "/api/models/" + internalPid + "/ratings";
    String id = scrapId(document);
    String apiUrl = HOME_PAGE + "/api/products/" + id;

    JSONObject available = CrawlerUtils.stringToJson(fetchApi(apiUrl + "/availability"));
    JSONArray variations = available.has("variation_list") ? available.getJSONArray("variation_list") : new JSONArray();
    JSONObject ratingJson = CrawlerUtils.stringToJson(fetchApi(ratingUrl));

    if (isProductPage(document)) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      for (Object object : variations) {
        RatingsReviews clone = ratingReviews.clone();
        JSONObject variation = (JSONObject) object;
        String internalId = scrapInternalId(variation);
        Integer totalNumOfEvaluations = getTotalNumOfRatings(ratingJson);
        Double avgRating = getTotalAvgRating(ratingJson);

        clone.setInternalId(internalId);
        clone.setTotalRating(totalNumOfEvaluations);
        clone.setTotalWrittenReviews(totalNumOfEvaluations);
        clone.setAverageOverallRating(avgRating);

        ratingReviewsCollection.addRatingReviews(clone);
      }
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
      if (product.has("productData")) {
        JSONObject productData = product.getJSONObject("productData");

        if (productData.has("model_number")) {
          internalPid = productData.get("model_number").toString();
        }
      }
    }

    return internalPid;
  }

  private String scrapInternalId(JSONObject variation) {
    return variation.has("sku") ? variation.getString("sku") : null;
  }

  private String fetchApi(String url) {
    Map<String, String> headers = new HashMap<>();
    headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
    headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
    headers.put("cache-control", "max-age=0");
    headers.put("upgrade-insecure-requests", "1");
    headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36");

    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).build();
    return this.dataFetcher.get(session, request).getBody();
  }

  private String scrapId(Document doc) {
    String internalId = null;
    Element metaElement = doc.selectFirst("meta[itemprop=\"sku\"]");

    if (metaElement != null) {
      internalId = metaElement.attr("content");
    }
    return internalId;
  }


}
