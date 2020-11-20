package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import br.com.lett.crawlernode.crawlers.extractionutils.core.YourreviewsRatingCrawler;
import br.com.lett.crawlernode.util.MathUtils;
import models.AdvancedRatingReview;
import models.RatingsReviews;

public class BrasilEletrumCrawler extends VTEXOldScraper {

   private static final String HOME_PAGE = "https://www.eletrum.com.br/";
   private static final String MAIN_SELLER_NAME = "Eletrum";
   private static final String STORE_KEY = "8ea7baa3-231d-4049-873e-ad5afd085ca4";

   public BrasilEletrumCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Arrays.asList(MAIN_SELLER_NAME);
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      YourreviewsRatingCrawler yourReviews = new YourreviewsRatingCrawler(session, cookies, logger, STORE_KEY, this.dataFetcher);

      Document docRating = yourReviews.crawlPageRatingsFromYourViews(internalPid, STORE_KEY, this.dataFetcher);
      Integer totalNumOfEvaluations = getTotalNumOfRatingsFromYourViews(docRating);
      Double avgRating = getTotalAvgRatingFromYourViews(docRating);
      AdvancedRatingReview advancedRatingReview = yourReviews.getTotalStarsFromEachValue(internalPid);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);

      return ratingReviews;
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


   @Override
   protected String scrapDescription(Document doc, JSONObject productJson) {
      StringBuilder description = new StringBuilder();

      if (productJson.has("description")) {
         description.append("<div><h3>Descrição</h3></div>");
         description.append(productJson.get("description").toString());
      }

      if (productJson.has("Descrição Do Produto")) {
         JSONArray jsonArray = productJson.getJSONArray("Descrição Do Produto");
         List<String> listKeys = new ArrayList<>();

         for (Object object : jsonArray) {
            String keys = (String) object;
            listKeys.add(keys);
         }

         for (String string : listKeys) {
            if (productJson.has(string)) {
               description.append(string).append(": ").append(productJson.get(string).toString().replace("[", "").replace("]", "")).append("<br>");
            }
         }

      }

      return description.toString();
   }
}
