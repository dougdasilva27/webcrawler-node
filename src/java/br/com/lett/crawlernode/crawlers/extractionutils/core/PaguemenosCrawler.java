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
import java.util.*;

public class PaguemenosCrawler extends VTEXNewScraper {

   public PaguemenosCrawler(Session session) {
      super(session);
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

      if (jsonRating != null) {
         JSONObject data = jsonRating.has("data") ? jsonRating.optJSONObject("data") : new JSONObject();
         JSONObject productReviews = data.has("productSummary") ? data.optJSONObject("productSummary") : new JSONObject();
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

         JSONArray ratingList = reviews.has("TopOpinions") ? reviews.optJSONArray("TopOpinions") : new JSONArray();

         if (ratingList != null) {

            for (int i = 0; i < ratingList.length(); i++) {
               switch (i) {
                  case 0:
                     advancedRatingReview.setTotalStar5(((JSONObject) ratingList.get(i)).optInt("Rate", 0));
                     break;
                  case 1:
                     advancedRatingReview.setTotalStar4(((JSONObject) ratingList.get(i)).optInt("Rate", 0));
                     break;
                  case 2:
                     advancedRatingReview.setTotalStar3(((JSONObject) ratingList.get(i)).optInt("Rate", 0));
                     break;
                  case 3:
                     advancedRatingReview.setTotalStar2(((JSONObject) ratingList.get(i)).optInt("Rate", 0));
                     break;
                  case 4:
                     advancedRatingReview.setTotalStar1(((JSONObject) ratingList.get(i)).optInt("Rate", 0));
                     break;
                  default:
               }
            }
         }
      }

      return advancedRatingReview;
   }

   private String createVariablesBase64(String internalId) {
      JSONObject variables = new JSONObject();
      variables.put("productId", internalId);
      variables.put("page", 1);
      variables.put("count", 5);
      variables.put("orderBy", 0);
      variables.put("filters", "");

      return Base64.getEncoder().encodeToString(variables.toString().getBytes());
   }

   private JSONObject crawlPageRatings(String internalId) throws UnsupportedEncodingException {

      String query = "{\"persistedQuery\":" +
         "{\"version\":1,\"sha256Hash\":\"4f721042cb5512f59c6aa3030e4a52e79ae1329a53747a6a3bcbff4c66d79f3f\",\"sender\":\"yourviews.yourviewsreviews@0.x\"," +
         "\"provider\":\"yourviews.yourviewsreviews@0.x\"}," +
         "\"variables\":\"" + createVariablesBase64(internalId) + "\"}";

      String encodedQuery = URLEncoder.encode(query, "UTF-8");

      StringBuilder url = new StringBuilder();
      url.append("https://www.paguemenos.com.br/_v/public/graphql/v1?extensions=");
      url.append(encodedQuery);

      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");

      Request request = RequestBuilder.create().setUrl(url.toString()).setHeaders(headers).build();
      String response = this.dataFetcher.get(session, request).getBody();

      return JSONUtils.stringToJson(response);
   }
}
