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
import br.com.lett.crawlernode.util.MathUtils;
import models.AdvancedRatingReview;
import models.RatingsReviews;

public class BrasilEletrumRatingReviewCrawler extends RatingReviewCrawler {
  private static final String STORE_KEY = "8ea7baa3-231d-4049-873e-ad5afd085ca4";

  public BrasilEletrumRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(document)) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(document, session);
      YourreviewsRatingCrawler yourReviews = new YourreviewsRatingCrawler(session, cookies, logger, STORE_KEY, this.dataFetcher);

      if (skuJson.has("productId")) {
        String internalPid = Integer.toString(skuJson.getInt("productId"));

        Document docRating = yourReviews.crawlPageRatingsFromYourViews(internalPid, STORE_KEY, this.dataFetcher);
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

  private Double getTotalAvgRatingFromYourViews(Document docRating) {
    Double avgRating = 0d;
    Element rating = docRating.selectFirst("meta[itemprop=ratingValue]");

    if (rating != null) {
      Double avg = MathUtils.parseDoubleWithDot((rating.attr("content")));
      avgRating = avg != null ? avg : 0d;
    }

    return avgRating;
  }

  private Integer getTotalNumOfRatingsFromYourViews(Document docRating) {
    Integer totalRating = 0;
    Element totalRatingElement = docRating.selectFirst("meta[itemprop=ratingCount]");

    if (totalRatingElement != null) {
      Integer total = MathUtils.parseInt(totalRatingElement.attr("content"));
      totalRating = total != null ? total : 0;
    }

    return totalRating;
  }

  private boolean isProductPage(Document document) {
    return document.selectFirst(".bf-product__spot") != null;
  }


}
