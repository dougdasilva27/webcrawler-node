package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import java.util.List;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXCrawlersUtils;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.YourreviewsRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.RatingsReviews;

public class BrasilPoloarRatingReviewCrawler extends RatingReviewCrawler {
  public BrasilPoloarRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(document)) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(document, session);
      YourreviewsRatingCrawler yourReviews = new YourreviewsRatingCrawler(session, cookies, logger);

      if (skuJson.has("productId")) {
        String internalPid = Integer.toString(skuJson.getInt("productId"));

        Document docRating = yourReviews.crawlPageRatingsFromYourViews(internalPid, "d6e492c4-14f6-45f0-8241-43a67ce4bb45");
        Integer totalNumOfEvaluations = yourReviews.getTotalNumOfRatingsFromYourViews(docRating);
        Double avgRating = yourReviews.getTotalAvgRatingFromYourViews(docRating);

        ratingReviews.setTotalRating(totalNumOfEvaluations);
        ratingReviews.setAverageOverallRating(avgRating);
        ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);

        List<String> idList = VTEXCrawlersUtils.crawlIdList(skuJson);
        for (String internalId : idList) {
          RatingsReviews clonedRatingReviews = ratingReviews.clone();
          clonedRatingReviews.setInternalId(internalId);
          ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
        }
      }
    }

    return ratingReviewsCollection;
  }

  private boolean isProductPage(Document document) {
    return document.selectFirst(".product-details") != null;
  }
}
