package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import br.com.lett.crawlernode.util.CrawlerUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import models.AdvancedRatingReview;
import models.RatingsReviews;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class BrasilSupermuffatoCrawler extends VTEXOldScraper {

   private static final String HOME_PAGE = "https://www.supermuffato.com.br/";
   private static final List<String> MAIN_SELLER_NAME_LOWER = Arrays.asList("Super Muffato Eletro");

   public BrasilSupermuffatoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return MAIN_SELLER_NAME_LOWER;
   }

   @Override
   protected String scrapDescription(Document doc, JSONObject productJson) {
      return CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#prd-description", "#prd-specifications"));
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      RatingsReviews ratingReviews = new RatingsReviews();

      JSONObject rating = getRating(doc, internalId);
      Integer totalReviews = CrawlerUtils.getIntegerValueFromJSON(rating, "count", 0);
      Double avgRating = CrawlerUtils.getDoubleValueFromJSON(rating, "rate", true, false);

      AdvancedRatingReview advancedRatingReview = new AdvancedRatingReview(getReview(doc, internalId));


      ratingReviews.setDate(session.getDate());
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);
      ratingReviews.setTotalRating(totalReviews);
      ratingReviews.setTotalWrittenReviews(totalReviews);
      ratingReviews.setAverageOverallRating(avgRating == null ? 0d : avgRating);
      ratingReviews.setInternalId(internalId);

      return ratingReviews;
   }

   private JSONObject getRating(Document doc, String internalId) {
      JSONObject ratingJson = new JSONObject();
      String idWebsite = getIdWebsite(doc);
      JSONObject response = CrawlerUtils.stringToJson(sendRequestToAPI(internalId, "rating", idWebsite));

      if (response.optJSONArray(internalId) != null) {
         JSONArray rate = response.getJSONArray(internalId);

         if (rate.length() > 0) {
            ratingJson = rate.getJSONObject(0);
         }
      }

      return ratingJson;
   }

   private JSONObject getReview(Document doc, String internalId) {
      JSONObject ratingJson = new JSONObject();
      String idWebsite = getIdWebsite(doc);
      JSONArray response = CrawlerUtils.stringToJsonArray(sendRequestToAPI(internalId, "reviews", idWebsite));
      if (response.optJSONObject(0) != null) {
         JSONObject jsonReviews = response.optJSONObject(0);
         if (jsonReviews.optJSONArray("stats") != null) {
            JSONArray starts = jsonReviews.optJSONArray("stats");
            ratingJson.put(AdvancedRatingReview.RATING_STAR_1_FIELD, starts.get(0));
            ratingJson.put(AdvancedRatingReview.RATING_STAR_2_FIELD, starts.get(1));
            ratingJson.put(AdvancedRatingReview.RATING_STAR_3_FIELD, starts.get(2));
            ratingJson.put(AdvancedRatingReview.RATING_STAR_4_FIELD, starts.get(3));
            ratingJson.put(AdvancedRatingReview.RATING_STAR_5_FIELD, starts.get(4));
         }
      }

      return ratingJson;
   }

   private String sendRequestToAPI(String internalId, String type, String idWebsite) {
      String apiUrl = "https://awsapis3.netreviews.eu/product";
      String payload =
         "{\"query\":\"" + type + "\",\"products\":\"" + internalId + "\",\"idWebsite\":\"" + idWebsite + "\",\"plateforme\":\"br\"}";
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json; charset=UTF-8");
      Request request =
         RequestBuilder.create().setUrl(apiUrl).setCookies(cookies).setHeaders(headers).setPayload(payload).mustSendContentEncoding(false).build();
      return new FetcherDataFetcher().post(session, request).getBody();
   }

   private String getIdWebsite(Document doc) {
      Optional<Element> optionalUrlToken = doc.select("body > script").stream()
         .filter(x -> (x.hasAttr("src") &&
            (x.attr("src").startsWith("https://cl.avis-verifies.com"))))
         .findFirst();

      String attr = optionalUrlToken.get().attr("src");

      String[] strings = attr.substring(attr.indexOf("br/")).split("/");

      return strings[strings.length - 4];
   }
}
