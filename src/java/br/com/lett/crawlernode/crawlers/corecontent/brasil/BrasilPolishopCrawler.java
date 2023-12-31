package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper;
import br.com.lett.crawlernode.crawlers.extractionutils.core.YourreviewsRatingCrawler;
import models.AdvancedRatingReview;
import models.RatingsReviews;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.Arrays;
import java.util.List;

public class BrasilPolishopCrawler extends VTEXNewScraper {

   private static final String HOME_PAGE = "https://www.polishop.com.br/";
   private static final List<String> SELLERS = Arrays.asList("polishop");

   public BrasilPolishopCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }


   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return SELLERS;
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject apiJson) {
      RatingsReviews ratingReviews = new RatingsReviews();
      YourreviewsRatingCrawler rating = new YourreviewsRatingCrawler(session, cookies, logger, "6c375e6c-50ab-4543-abbf-08ace7c3154d", dataFetcher);

      Document docRating = rating.crawlPageRatingsFromYourViews(internalPid, "6c375e6c-50ab-4543-abbf-08ace7c3154d", dataFetcher);

      Integer totalNumOfEvaluations = rating.getTotalNumOfRatingsFromYourViews(docRating);
      Double avgRating = rating.getTotalAvgRatingFromYourViews(docRating);
      AdvancedRatingReview adRating = rating.getTotalStarsFromEachValue(internalPid);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAdvancedRatingReview(adRating);

      return ratingReviews;
   }

   @Override
   protected String scrapName(Document doc, JSONObject productJson, JSONObject jsonSku) {
      String name = null;

      if (jsonSku.has("nameComplete")) {
         name = jsonSku.optString("nameComplete");
      }

      if (name == null && productJson.has("productName") && productJson.opt("productName") != null) {
         name = productJson.optString("productName");

      } else if (name == null && jsonSku.has("name")) {
         name = jsonSku.optString("name");
      }

      if (name != null && !name.isEmpty() && productJson.has("brand")) {
         String brand = productJson.optString("brand");
         if (brand != null && !brand.isEmpty() && !checkIfNameHasBrand(brand, name)) {
            name = name + " - " + brand;
         }
      }
      return name;
   }
}
