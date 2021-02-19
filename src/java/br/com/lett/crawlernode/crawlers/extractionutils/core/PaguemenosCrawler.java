package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import models.AdvancedRatingReview;
import models.RatingsReviews;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class PaguemenosCrawler extends VTEXNewScraper {

   public PaguemenosCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   private static final String HOME_PAGE = "https://www.paguemenos.com.br/";
   private static final String API_TOKEN = "c5c833774d6feccd351ce70d3b8353c0d99ae61432ec46a2c648f430936ff8e5";
   private static final List<String> MAIN_SELLERS = Arrays.asList("Farmácias Pague Menos");
   private RatingsReviews rating = new RatingsReviews();

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return MAIN_SELLERS;
   }

   @Override
   protected void processBeforeScrapVariations(Document doc, JSONObject productJson, String internalPid) throws UnsupportedEncodingException {
      super.processBeforeScrapVariations(doc, productJson, internalPid);
      this.rating = scrapRating(internalPid);
   }

   @Override
   protected String scrapName(Document doc, JSONObject productJson, JSONObject jsonSku) {
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1 .vtex-store-components-3-x-productBrand", false);
      return name != null ? (productJson.optString("brand") + " " + name) : null;
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return this.rating;
   }

   protected RatingsReviews scrapRating(String internalPid) throws UnsupportedEncodingException {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      JSONObject jsonRating = crawlPageRatings(internalPid);

      JSONObject element = null;
      Integer totalNumOfEvaluations = null;
      Double avgRating = null;
      System.err.println(jsonRating);

      if (jsonRating != null) {
         JSONObject data = jsonRating.has("data") ? jsonRating.optJSONObject("data") : new JSONObject();
         JSONObject productReviews = data.has("productReviews") ? data.optJSONObject("productReviews") : new JSONObject();
         element = productReviews.has("Element") ? productReviews.optJSONObject("Element") : new JSONObject();
      }

      if (element != null) {
         totalNumOfEvaluations = element.optInt("TotalRatings", 0);
         avgRating = element.optDouble("Rating", 0.0);
      }

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setAdvancedRatingReview(scrapAdvancedRating(element));
      return ratingReviews;
   }

   private AdvancedRatingReview scrapAdvancedRating(JSONObject reviews) {

      AdvancedRatingReview advancedRatingReview = new AdvancedRatingReview();

      if (reviews != null) {

         JSONObject ratingHistogram = reviews.has("RatingHistogram") ? reviews.optJSONObject("RatingHistogram") : new JSONObject();
         JSONArray ratingList = ratingHistogram.has("RatingList") ? ratingHistogram.optJSONArray("RatingList") : new JSONArray();

         if (ratingList != null) {

            for (int i = 0; i < ratingList.length(); i++) {
               switch (i) {
                  case 0:
                     advancedRatingReview.setTotalStar5(((JSONObject) ratingList.get(i)).optInt("Total", 0));
                     break;
                  case 1:
                     advancedRatingReview.setTotalStar4(((JSONObject) ratingList.get(i)).optInt("Total", 0));
                     break;
                  case 2:
                     advancedRatingReview.setTotalStar3(((JSONObject) ratingList.get(i)).optInt("Total", 0));
                     break;
                  case 3:
                     advancedRatingReview.setTotalStar2(((JSONObject) ratingList.get(i)).optInt("Total", 0));
                     break;
                  case 4:
                     advancedRatingReview.setTotalStar1(((JSONObject) ratingList.get(i)).optInt("Total", 0));
                     break;
                  default:
               }
            }
         }
      }

      return advancedRatingReview;
   }

   private JSONObject crawlPageRatings(String internalPid) throws UnsupportedEncodingException {
      String jsonProductId = "{\"productId\":\"" + internalPid + "\",\"page\":1,\"count\":5,\"orderBy\":0,\"filters\":\"\"}";
      String encodedString = Base64.getEncoder().encodeToString(jsonProductId.getBytes());

      String api = "https://www.paguemenos.com.br/_v/public/graphql/v1?extensions=";

      String query = "{\"persistedQuery\":{\"version\":1,\"sha256Hash\":\"" + API_TOKEN + "\",\"sender\":\"yourviews.yourviewsreviews@0.x\",\"provider\":\"yourviews.yourviewsreviews@0.x\"}," +
         "\"variables\":\"" + encodedString + "\"}";

      String encodedQuery = URLEncoder.encode(query, "UTF-8");

      Request request = RequestBuilder.create().setUrl(api + encodedQuery)
         .build();
      String response = this.dataFetcher.get(session, request).getBody();


      return JSONUtils.stringToJson(response);
   }
}
