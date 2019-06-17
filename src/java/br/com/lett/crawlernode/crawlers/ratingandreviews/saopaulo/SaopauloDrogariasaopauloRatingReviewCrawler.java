package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.YourreviewsRatingCrawler;
import br.com.lett.crawlernode.test.Test;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.AdvancedRatingReview;
import models.RatingsReviews;

/**
 * Date: 14/12/16
 * 
 * @author gabriel
 *
 */
public class SaopauloDrogariasaopauloRatingReviewCrawler extends RatingReviewCrawler {

  public SaopauloDrogariasaopauloRatingReviewCrawler(Session session) {
    super(session);
  }


  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(document)) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(document, session);

      if (skuJson.has("productId")) {
        String internalPid = Integer.toString(skuJson.getInt("productId"));

        YourreviewsRatingCrawler yr = new YourreviewsRatingCrawler(session, cookies, logger, "87b2aa32-fdcb-4f1d-a0b9-fd6748df725a", dataFetcher);
        Document docRating = yr.crawlPageRatingsFromYourViews(internalPid, "87b2aa32-fdcb-4f1d-a0b9-fd6748df725a", dataFetcher);
        CommonMethods.saveDataToAFile(docRating, Test.pathWrite + "x.html");
        Integer totalNumOfEvaluations = yr.getTotalNumOfRatingsFromYourViews(docRating);
        Double avgRating = yr.getTotalAvgRatingFromYourViews(docRating);

        AdvancedRatingReview advancedRatingReview = yr.getTotalStarsFromEachValue(internalPid);

        ratingReviews.setAdvancedRatingReview(advancedRatingReview);
        ratingReviews.setTotalRating(totalNumOfEvaluations);
        ratingReviews.setAverageOverallRating(avgRating);
        ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);

        List<String> idList = crawlIdList(skuJson);
        for (String internalId : idList) {
          RatingsReviews clonedRatingReviews = (RatingsReviews) ratingReviews.clone();
          clonedRatingReviews.setInternalId(internalId);
          ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
        }

      }

    }

    return ratingReviewsCollection;

  }


  private List<String> crawlIdList(JSONObject skuJson) {
    List<String> idList = new ArrayList<>();

    if (skuJson.has("skus")) {
      JSONArray skus = skuJson.getJSONArray("skus");

      for (int i = 0; i < skus.length(); i++) {
        JSONObject sku = skus.getJSONObject(i);

        if (sku.has("sku")) {
          idList.add(Integer.toString(sku.getInt("sku")));
        }
      }
    }

    return idList;
  }

  private boolean isProductPage(Document document) {
    return document.select("#___rc-p-sku-ids").first() != null;
  }
}
