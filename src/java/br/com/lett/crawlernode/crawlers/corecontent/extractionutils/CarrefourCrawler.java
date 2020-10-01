package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.MathUtils;
import exceptions.MalformedPricingException;
import models.AdvancedRatingReview;
import models.RatingsReviews;
import models.pricing.BankSlip;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class CarrefourCrawler extends VTEXNewScraper{

   private static final List<String> SELLERS = Arrays.asList("Carrefour");
   private static final List<Cookie> COOKIES = new ArrayList<>();

   public CarrefourCrawler(Session session) {
      super(session);
   }

   protected abstract String getLocation();

   @Override
   protected JSONObject crawlProductApi(String internalPid, String parameters) {
      JSONObject productApi = new JSONObject();

      String url = "https://" + getHomePage() + "api/catalog_system/pub/products/search?fq=productId:" + internalPid + (parameters == null ? "" : parameters);

      BasicClientCookie cookie = new BasicClientCookie("ls.uid_armazem", getLocation());
      cookie.setDomain(getHomePage());
      cookie.setPath("/");
      COOKIES.add(cookie);

      Request request = Request.RequestBuilder.create().setUrl(url).setCookies(COOKIES).build();
      JSONArray array = CrawlerUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());

      if (!array.isEmpty()) {
         productApi = array.optJSONObject(0) == null ? new JSONObject() : array.optJSONObject(0);
      }

      return productApi;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return SELLERS;
   }

   @Override
   protected BankSlip scrapBankSlip(Double spotlightPrice, JSONObject comertial, JSONObject discounts, boolean mustSetDiscount) throws MalformedPricingException {
      Double bankSlipPrice = spotlightPrice;
      Double discount = 0d;

      JSONObject paymentOptions = comertial.optJSONObject("PaymentOptions");
      if (paymentOptions != null) {
         JSONArray cardsArray = paymentOptions.optJSONArray("installmentOptions");
         if (cardsArray != null) {
            for (Object o : cardsArray) {
               JSONObject paymentJson = (JSONObject) o;

               String paymentCode = paymentJson.optString("paymentSystem");
               String name = paymentJson.optString("paymentName");

               if (name.toLowerCase().contains("boleto")) {
                  if(paymentJson.has("installments")){
                     JSONArray bankSlipInstallments = paymentJson.optJSONArray("installments");
                     for(Object i: bankSlipInstallments){
                        bankSlipPrice = ((JSONObject) i).optDouble("total") / 100;
                     }
                  }
                  break;
               }
            }
         }
      }

      if (!mustSetDiscount) {
         discount = null;
      }

      return BankSlip.BankSlipBuilder.create()
         .setFinalPrice(bankSlipPrice)
         .setOnPageDiscount(discount)
         .build();
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {

      RatingsReviews ratingReviews = new RatingsReviews();
      AdvancedRatingReview advancedRatingReview = new AdvancedRatingReview();

      String apiRating = "https://carrefourbrasil.mais.social/reviews/transit/get/products/crf/" + internalId + "/reviews/offuser/first";

      Request request = Request.RequestBuilder.create().setUrl(apiRating).build();
      JSONObject response = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

      int totalNumberOfReviews = response.optInt("total");
      JSONObject aggregateRating = response.optJSONObject("aggregateRating");
      Double avgRating = aggregateRating.optDouble("ratingValue");
      JSONObject stars = aggregateRating.optJSONObject("ratingComposition");

      advancedRatingReview.setTotalStar1(stars.optInt("1"));
      advancedRatingReview.setTotalStar2(stars.optInt("2"));
      advancedRatingReview.setTotalStar3(stars.optInt("3"));
      advancedRatingReview.setTotalStar4(stars.optInt("4"));
      advancedRatingReview.setTotalStar5(stars.optInt("5"));

      ratingReviews.setDate(session.getDate());
      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumberOfReviews);
      ratingReviews.setTotalWrittenReviews(totalNumberOfReviews);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

}
