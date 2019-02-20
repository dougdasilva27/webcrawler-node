package br.com.lett.crawlernode.crawlers.ratingandreviews.unitedstates;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.methods.GETFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

public class UnitedstatesNikeRatingReviewCrawler extends RatingReviewCrawler {

  private static final String HOME_PAGE = "https://www.nike.com";
  private final Map<String, String> DEFAULT_HEADERS;

  public UnitedstatesNikeRatingReviewCrawler(Session session) {
    super(session);

    DEFAULT_HEADERS = new HashMap<>();
    DEFAULT_HEADERS.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
    DEFAULT_HEADERS.put("accept-encoding", "gzip, deflate, br");
    DEFAULT_HEADERS.put("accept-language", "en-US,en;q=0.9");
    DEFAULT_HEADERS.put("cache-control", "max-age=0");
    DEFAULT_HEADERS.put("upgrade-insecure-requests", "1");
    DEFAULT_HEADERS.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36");
  }

  @Override
  public void handleCookiesBeforeFetch() { // cookies =
    CrawlerUtils.fetchCookiesFromAPage(HOME_PAGE + "/us/en_us/", null, ".nike.com", "/", null, session, DEFAULT_HEADERS);
  }

  @Override
  protected Document fetch() {
    return Jsoup.parse(GETFetcher.fetchPageGETWithHeaders(session, session.getOriginalURL(), cookies, DEFAULT_HEADERS, null, 1));
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

            if (reviewsJson.has("total") && reviewsJson.has("averageRating")) {
              Integer totalNumOfEvaluations = reviewsJson.getInt("total");
              Double avgRating = reviewsJson.getDouble("averageRating");

              ratingReviews.setInternalId(internalId);
              ratingReviews.setTotalRating(totalNumOfEvaluations);
              ratingReviews.setAverageOverallRating(avgRating);

              ratingReviewsCollection.addRatingReviews(ratingReviews);
            }
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
