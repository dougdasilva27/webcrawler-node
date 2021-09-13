package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.AdvancedRatingReview;
import models.RatingsReviews;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.Arrays;
import java.util.List;

public class BrasilZeedogCrawler extends VTEXOldScraper {
   public BrasilZeedogCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return "https://www.zeedog.com.br/";
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Arrays.asList("Zee.Dog");
   }

   private JSONArray getReviewApi(String internalPid) {

      String url = "https://cl.avis-verifies.com/br/cache/8/5/9/85998c32-9db1-7df4-3d2b-24e786e01fc1/AWS/PRODUCT_API/REVIEWS/" + internalPid + ".json";

      Request request = Request.RequestBuilder.create().setUrl(url).build();
      String content = this.dataFetcher.get(session, request).getBody();

      return CrawlerUtils.stringToJsonArray(content);

   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {

      RatingsReviews ratingsReviews = new RatingsReviews();
      JSONArray reviewApi = getReviewApi(internalPid);
      Double avgRating = null;
      AdvancedRatingReview adRating = new AdvancedRatingReview();
      Integer totalNumOfEvaluations = reviewApi.length();

      if (totalNumOfEvaluations > 0) {
         adRating = scrapAdvancedRatingReview(reviewApi);

         avgRating = getAvgRatingCalculate(adRating, totalNumOfEvaluations);


      }

      ratingsReviews.setTotalRating(totalNumOfEvaluations);
      ratingsReviews.setAverageOverallRating(avgRating);
      ratingsReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingsReviews.setAdvancedRatingReview(adRating);

      return ratingsReviews;

   }

   private AdvancedRatingReview scrapAdvancedRatingReview(JSONArray reviewApi) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;


      for (Object o : reviewApi) {

         if (o instanceof JSONObject) {

            JSONObject review = (JSONObject) o;

            String rate = review.optString("rate");
            Integer value = Integer.valueOf(rate);

            switch (value) {
               case 5:
                  star5 += 1;
                  break;
               case 4:
                  star4 += 1;
                  break;
               case 3:
                  star3 += 1;
                  break;
               case 2:
                  star2 += 1;
                  break;
               case 1:
                  star1 += 1;
                  break;
               default:
                  break;
            }
         }
      }

      return new AdvancedRatingReview.Builder()
         .totalStar1(star1)
         .totalStar2(star2)
         .totalStar3(star3)
         .totalStar4(star4)
         .totalStar5(star5)
         .build();
   }

   private Double getAvgRatingCalculate(AdvancedRatingReview adRating, Integer totalNumOfEvaluations) {

      double totalStar1 = adRating.getTotalStar1();
      double totalStar2 = adRating.getTotalStar2() * 2;
      double totalStar3 = adRating.getTotalStar3() * 3;
      double totalStar4 = adRating.getTotalStar4() * 4;
      double totalStar5 = adRating.getTotalStar5() * 5;


      return (totalStar1 + totalStar2 + totalStar3 + totalStar4 + totalStar5) / totalNumOfEvaluations;

   }
}


//https://cl.avis-verifies.com/br/cache/8/5/9/85998c32-9db1-7df4-3d2b-24e786e01fc1/AWS/PRODUCT_API/REVIEWS/5011124.json
