package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import java.util.List;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXCrawlersUtils;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.YourreviewsRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.AdvancedRatingReview;
import models.RatingsReviews;

public class BrasilLojasredeRatingReviewCrawler extends RatingReviewCrawler {
  public BrasilLojasredeRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(document)) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(document, session);
      YourreviewsRatingCrawler yourReviews =
          new YourreviewsRatingCrawler(session, cookies, logger, "9c0aa0e9-37a2-4b03-93d7-41c964268161", this.dataFetcher);

      if (skuJson.has("productId")) {
        String internalPid = Integer.toString(skuJson.getInt("productId"));

        Document docRating = yourReviews.crawlPageRatingsFromYourViews(internalPid, "9c0aa0e9-37a2-4b03-93d7-41c964268161", this.dataFetcher);
        Integer totalNumOfEvaluations = getTotalNumOfRatingsFromYourViews(docRating);
        Double avgRating = getTotalAvgRatingFromYourViews(docRating);
        AdvancedRatingReview advancedRatingReview = yourReviews.getTotalStarsFromEachValue(internalPid);

        ratingReviews.setAdvancedRatingReview(advancedRatingReview);
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

  private Double getTotalAvgRatingFromYourViews(Document docRating) {
    Double avgRating = 0d;
    Element rating = docRating.selectFirst("meta[itemprop=ratingValue]");

    if (rating != null) {
      avgRating = Double.parseDouble(rating.attr("content"));
    }

    return avgRating;
  }

  private Integer getTotalNumOfRatingsFromYourViews(Document doc) {
    Integer totalRating = 0;
    Element totalRatingElement = doc.selectFirst("meta[itemprop=ratingCount]");

    if (totalRatingElement != null) {
      totalRating = Integer.parseInt(totalRatingElement.attr("content"));
    }

    return totalRating;
  }
}
