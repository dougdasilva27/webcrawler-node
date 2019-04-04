package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.YourreviewsRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

public class BrasilLebesRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilLebesRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      YourreviewsRatingCrawler yrRC = new YourreviewsRatingCrawler(session, cookies, logger);
      JSONObject vtexJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

      if (vtexJson.has("productId")) {
        String internalPid = Integer.toString(vtexJson.getInt("productId"));
        Document docRating = yrRC.crawlPageRatingsFromYourViews(internalPid, "d0b53e21-10fb-4c05-8d9c-10fdafae9edd", dataFetcher);

        Integer totalNumOfEvaluations = yrRC.getTotalNumOfRatingsFromYourViews(docRating);
        Double avgRating = yrRC.getTotalAvgRatingFromYourViews(docRating);

        JSONArray arraySkus = vtexJson.has("skus") ? vtexJson.getJSONArray("skus") : new JSONArray();

        for (int i = 0; i < arraySkus.length(); i++) {
          JSONObject json = arraySkus.getJSONObject(i);

          String internalId = json.has("sku") ? json.get("sku").toString() : null;

          ratingReviews.setDate(session.getDate());
          ratingReviews.setInternalId(internalId);
          ratingReviews.setTotalRating(totalNumOfEvaluations);
          ratingReviews.setAverageOverallRating(avgRating);
          ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);

          ratingReviewsCollection.addRatingReviews(ratingReviews);
        }
      }
    }

    return ratingReviewsCollection;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".productName") != null;
  }
}
