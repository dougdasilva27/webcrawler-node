package br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

public class NikeRatingReviewCrawler extends RatingReviewCrawler {

  // Must be changed for each child (default: USA)
  protected static final String HOME_PAGE = null;
  protected static final String COUNTRY_URL = null;
  protected final Map<String, String> defaultHeaders;

  public NikeRatingReviewCrawler(Session session) {
    super(session);

    defaultHeaders = new HashMap<>();
    defaultHeaders.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
    defaultHeaders.put("accept-encoding", "gzip, deflate, br");
    defaultHeaders.put("accept-language", "en-US,en;q=0.9");
    defaultHeaders.put("cache-control", "max-age=0");
    defaultHeaders.put("upgrade-insecure-requests", "1");
    defaultHeaders.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36");
  }

  @Override
  public void handleCookiesBeforeFetch() {
    CrawlerUtils.fetchCookiesFromAPage(HOME_PAGE + COUNTRY_URL, null, ".nike.com", "/", null, session, defaultHeaders, dataFetcher);
  }

  @Override
  protected Document fetch() {
    Request request = RequestBuilder.create().setUrl(session.getOriginalURL()).setCookies(cookies).setHeaders(defaultHeaders).build();
    return Jsoup.parse(this.dataFetcher.get(session, request).getBody());
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.INITIAL_REDUX_STATE=", ";", false, true);

      JSONObject idJson = json.has("Threads") ? json.getJSONObject("Threads") : new JSONObject();
      idJson = idJson.has("products") ? idJson.getJSONObject("products") : new JSONObject();

      for (int i = 0; i < idJson.names().length(); i++) {
        JSONObject internalProduct = idJson.getJSONObject(idJson.names().getString(i));
        JSONArray skus = internalProduct.has("skus") ? internalProduct.getJSONArray("skus") : new JSONArray();

        for (Object o : skus) {
          JSONObject sku = (JSONObject) o;

          if (sku.has("id")) {
            String internalId = sku.getString("id");

            JSONObject reviewsJson = json.has("reviews") ? json.getJSONObject("reviews") : new JSONObject();

            Integer totalNumOfEvaluations = CrawlerUtils.getIntegerValueFromJSON(reviewsJson, "total", 0);
            Double avgRating = CrawlerUtils.getDoubleValueFromJSON(reviewsJson, "averageRating");


            RatingsReviews clone = ratingReviews.clone();
            clone.setInternalId(internalId);
            clone.setTotalRating(totalNumOfEvaluations);
            clone.setAverageOverallRating(avgRating == null ? 0.0 : avgRating);

            ratingReviewsCollection.addRatingReviews(clone);
          }
        }
      }
    }

    return ratingReviewsCollection;
  }

  protected boolean isProductPage(Document doc) {
    return doc.selectFirst(".visual-search-product-col") != null;
  }
}
