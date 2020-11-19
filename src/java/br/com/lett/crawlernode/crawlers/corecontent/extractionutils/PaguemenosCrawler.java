package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
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
   private static final String API_TOKEN = "e7c22a525979dba2d63a94c2666965a3c76dd289b1937645eb2131d801d3ef7b";
   private static final List<String> MAIN_SELLERS = Arrays.asList("Pague Menos", "Farmácias Pague Menos");
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
      return (productJson.optString("brand") + " " + super.scrapName(doc, productJson, jsonSku)).trim();
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

      if(jsonRating != null){
         JSONObject data = jsonRating.has("data")? jsonRating.optJSONObject("data"): new JSONObject();
         JSONObject productReviews = data.has("productReviews")? data.optJSONObject("productReviews"): new JSONObject();
         element = productReviews.has("Element")? productReviews.optJSONObject("Element") :new JSONObject();
      }

      if(element != null) {
         totalNumOfEvaluations = element.optInt("TotalRatings", 0);
         avgRating = element.optDouble("Rating", 0.0);
      }

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setAdvancedRatingReview(scrapAdvancedRating(element));
      return ratingReviews;
   }

   private AdvancedRatingReview scrapAdvancedRating(JSONObject reviews){

      AdvancedRatingReview advancedRatingReview = new AdvancedRatingReview();

      if(reviews != null){

         JSONObject ratingHistogram = reviews.has("RatingHistogram")? reviews.optJSONObject("RatingHistogram"): new JSONObject();
         JSONArray ratingList =  ratingHistogram.has("RatingList")? ratingHistogram.optJSONArray("RatingList"): new JSONArray();

         if(ratingList !=null){

            for(int i = 0; i < ratingList.length(); i++){
               switch (i){
                  case 0:
                     advancedRatingReview.setTotalStar5(((JSONObject) ratingList.get(i)).optInt("Total",0));
                     break;
                  case 1:
                     advancedRatingReview.setTotalStar4(((JSONObject) ratingList.get(i)).optInt("Total",0));
                     break;
                  case 2:
                     advancedRatingReview.setTotalStar3(((JSONObject) ratingList.get(i)).optInt("Total",0));
                     break;
                  case 3:
                     advancedRatingReview.setTotalStar2(((JSONObject) ratingList.get(i)).optInt("Total",0));
                     break;
                  case 4:
                     advancedRatingReview.setTotalStar1(((JSONObject) ratingList.get(i)).optInt("Total",0));
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

      String api = "https://www.paguemenos.com.br/_v/public/graphql/v1?workspace=master&maxAge=medium&appsEtag=remove&domain=store&locale=pt-BR&__bindingId=23424e23-86bb-4397-98b0-238d88d7f528&operationName=productReviews&variables=";

      String query = "{\"persistedQuery\":{\"version\":1,\"sha256Hash\":\"" + API_TOKEN +"\",\"sender\":\"yourviews.yourviewsreviews@0.x\",\"provider\":\"yourviews.yourviewsreviews@0.x\"}," +
         "\"variables\":\"" + encodedString + "\"}";

      String encodedQuery = URLEncoder.encode(query, "UTF-8");

      Request request = RequestBuilder.create().setUrl(api+encodedQuery)
            .build();
      String response = this.dataFetcher.get(session, request).getBody();


      return JSONUtils.stringToJson(response);
   }
}
