package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.AdvancedRatingReview;
import models.RatingsReviews;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

public class YotpoRatingReviewCrawler {

   private Session session;
   protected Logger logger;
   private List<Cookie> cookies;

   public YotpoRatingReviewCrawler(Session session, List<Cookie> cookies, Logger logger) {
      this.session = session;
      this.cookies = cookies;
      this.logger = logger;
   }

   /**
    * General method to extract ratings from Yotpo API.
    *
    * @param url           api'url
    * @param appKey
    * @param payloadString (can be different from productId)
    * @param dataFetcher
    * @return
    */
   public Document extractRatingsFromYotpo(String appKey, DataFetcher dataFetcher, String payloadString, String url) {
      Document doc = new Document("");

      try {
         String methodsEncoded = URLEncoder.encode(payloadString, "UTF-8");

         String payload = "methods=" + methodsEncoded + "&app_key=" + appKey;

         Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setPayload(payload).build();
         String response = dataFetcher.post(session, request).getBody();

         JSONArray arr = CrawlerUtils.stringToJsonArray(response);

         doc = new Document("");

         for (Object o : arr) {
            JSONObject json = (JSONObject) o;

            String responseHtml = json.has("result") ? json.getString("result") : null;

            doc.append(responseHtml);
         }

      } catch (UnsupportedEncodingException e) {
         Logging.printLogError(logger, session, "Could not encode url for Yotpo API");
      }

      return doc;
   }

   public RatingsReviews scrapRatingYotpo(Document apiDoc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtml(apiDoc, "a.text-m", true, 0);
      Double avgRating = CrawlerUtils.scrapDoublePriceFromHtml(apiDoc, ".yotpo-bottomline .sr-only", null, true, '.', session);
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingsReviewsYotpo(apiDoc);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   public AdvancedRatingReview scrapAdvancedRatingsReviewsYotpo(Document apiDoc) {

      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Elements reviews = apiDoc.select(".yotpo-distibutions-sum-reviews > span");
      if (reviews != null) {
         for (Element review : reviews) {

            String stringStarNumber = CrawlerUtils.scrapStringSimpleInfoByAttribute(review, null, "data-score-distribution");
            int starNumber = stringStarNumber != null ? Integer.parseInt(stringStarNumber) : 0;

            switch (starNumber) {
               case 5:
                  star5 = CrawlerUtils.scrapIntegerFromHtml(review, null, true, 0);
                  break;
               case 4:
                  star4 = CrawlerUtils.scrapIntegerFromHtml(review, null, true, 0);
                  break;
               case 3:
                  star3 = CrawlerUtils.scrapIntegerFromHtml(review, null, true, 0);
                  break;
               case 2:
                  star2 = CrawlerUtils.scrapIntegerFromHtml(review, null, true, 0);
                  break;
               case 1:
                  star1 = CrawlerUtils.scrapIntegerFromHtml(review, null, true, 0);
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

}
