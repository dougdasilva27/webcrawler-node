package br.com.lett.crawlernode.crawlers.ratingandreviews.peru;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXCrawlersUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

public class PeruMetroRatingReviewCrawler extends RatingReviewCrawler {
  private static final String HOME_PAGE = "https://www.metro.pe/";
  private static final String MAIN_SELLER_NAME_LOWER = "metro";

  public PeruMetroRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

    // Acessing API to get reviews HTML
    JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);
    VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies);
    String internalPid = vtexUtil.crawlInternalPid(skuJson);
    String url = "https://www.metro.pe/wongfood/dataentities/RE/documents/" + internalPid + "?_fields=reviews";
    Document html = Jsoup.parse(DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, cookies));

    JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

    for (int i = 0; i < arraySkus.length(); i++) {
      JSONObject jsonSku = arraySkus.getJSONObject(i);

      String internalId = vtexUtil.crawlInternalId(jsonSku);
      RatingsReviews ratingReviews = extractReviews(html, internalId);

      ratingReviewsCollection.addRatingReviews(ratingReviews);
    }

    return ratingReviewsCollection;
  }

  private RatingsReviews extractReviews(Document doc, String internalId) {
    RatingsReviews ratingReviews = new RatingsReviews();

    Integer totalNumOfEvaluations = 0;
    Double avgRating = 0.0;

    Element aux = doc.selectFirst("body");

    if (aux != null) {
      JSONObject outterJson = new JSONObject(aux.text());

      if (outterJson.has("reviews")) {
        JSONObject middleJson = new JSONObject(outterJson.get("reviews").toString());

        for (String s : middleJson.keySet()) {
          JSONObject innerJson = middleJson.getJSONObject(s);

          // Valid review
          if (!innerJson.has("date")) {
            continue;
          }

          totalNumOfEvaluations += 1;

          if (innerJson.has("rating")) {
            avgRating = (double) (avgRating + innerJson.getInt("rating"));;
          }
        }

        // Handling division by 0
        if (totalNumOfEvaluations != 0) {
          avgRating /= totalNumOfEvaluations;
        }

        ratingReviews.setDate(session.getDate());
      }
    }

    ratingReviews.setInternalId(internalId);
    ratingReviews.setTotalRating(totalNumOfEvaluations);
    ratingReviews.setAverageOverallRating(avgRating);
    ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);

    return ratingReviews;
  }
}
