package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper;
import br.com.lett.crawlernode.crawlers.extractionutils.core.YourreviewsRatingCrawler;
import models.AdvancedRatingReview;
import models.RatingsReviews;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;

public class BrasilLojasredeCrawler extends VTEXNewScraper {

   public BrasilLojasredeCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return session.getOptions().optString("homePage");
   }

   @Override
   protected List<String> getMainSellersNames() {
      List<String> sellers = new ArrayList<>();
      session.getOptions().optJSONArray("sellers").toList().forEach(s -> sellers.add((String) s));
      return sellers;
   }


   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      RatingsReviews ratingReviews = new RatingsReviews();
      YourreviewsRatingCrawler rating = new YourreviewsRatingCrawler(session, cookies, logger, "9c0aa0e9-37a2-4b03-93d7-41c964268161", dataFetcher);
      Document docRating = rating.crawlPageRatingsFromYourViews(internalPid, "9c0aa0e9-37a2-4b03-93d7-41c964268161", dataFetcher);

      Integer totalNumOfEvaluations = rating.getTotalNumOfRatingsFromYourViews(docRating);
      Double avgRating = rating.getTotalAvgRatingFromYourViews(docRating);
      AdvancedRatingReview adRating = rating.getTotalStarsFromEachValue(internalPid);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAdvancedRatingReview(adRating);

      return ratingReviews;
   }
}
