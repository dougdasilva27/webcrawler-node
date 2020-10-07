package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.Arrays;
import java.util.List;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JavanetDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.RequestsStatistics;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.test.Test;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import models.AdvancedRatingReview;
import models.RatingsReviews;
import models.pricing.BankSlip;

public abstract class CarrefourCrawler extends VTEXNewScraper {

   private static final List<String> SELLERS = Arrays.asList("Carrefour");

   public CarrefourCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
   }

   @Override
   protected String scrapInternalpid(Document doc) {
      String internalPid = super.scrapInternalpid(doc);

      if (internalPid == null) {
         internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, ".vtex-product-identifier-0-x-product-identifier__value", true);
      }

      return internalPid;
   }

   protected abstract String getLocation();

   protected String fetchPage(String url) {

      Request request = RequestBuilder.create()
            .setUrl(url)
            .setCookies(cookies)
            .setFetcheroptions(
                  FetcherOptionsBuilder.create()
                        .mustUseMovingAverage(false)
                        .mustRetrieveStatistics(true)
                        .build())
            .setProxyservice(Arrays.asList(
                  ProxyCollection.INFATICA_RESIDENTIAL_BR,
                  ProxyCollection.NETNUT_RESIDENTIAL_BR,
                  ProxyCollection.LUMINATI_SERVER_BR))
            .build();

      int attempts = 0;
      Response response = this.dataFetcher.get(session, request);
      String body = response.getBody();

      Integer statusCode = 0;
      List<RequestsStatistics> requestsStatistics = response.getRequests();
      if (!requestsStatistics.isEmpty()) {
         statusCode = requestsStatistics.get(requestsStatistics.size() - 1).getStatusCode();
      }

      boolean retry = statusCode == null ||
            (Integer.toString(statusCode).charAt(0) != '2'
                  && Integer.toString(statusCode).charAt(0) != '3'
                  && statusCode != 404);

      // If fetcher don't return the expected response we try with apache
      // If apache do the same, we try with javanet
      if (retry) {
         do {
            if (attempts == 0) {
               body = new ApacheDataFetcher().get(session, request).getBody();
            } else if (attempts == 1) {
               body = new JavanetDataFetcher().get(session, request).getBody();
            }

            attempts++;
         } while (attempts < 2 && (body == null || body.isEmpty()));
      }

      CommonMethods.saveDataToAFile(body, Test.pathWrite + "CARREFOUR.html");

      return body;
   }

   @Override
   protected Object fetch() {
      return Jsoup.parse(fetchPage(session.getOriginalURL()));
   }

   @Override
   protected JSONObject crawlProductApi(String internalPid, String parameters) {
      JSONObject productApi = new JSONObject();

      String url = homePage + "api/catalog_system/pub/products/search?fq=productId:" + internalPid + (parameters == null ? "" : parameters);

      String body = fetchPage(url);
      JSONArray array = CrawlerUtils.stringToJsonArray(body);

      if (!array.isEmpty()) {
         productApi = array.optJSONObject(0) == null ? new JSONObject() : array.optJSONObject(0);
      }

      return productApi;
   }


   @Override
   public void handleCookiesBeforeFetch() {
      super.handleCookiesBeforeFetch();

      BasicClientCookie cookie = new BasicClientCookie("userLocationData", getLocation());
      cookie.setDomain(getHomePage().replace("https://", "").replace("/", ""));
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   @Override
   protected List<String> getMainSellersNames() {
      return SELLERS;
   }

   @Override
   protected Double scrapSpotlightPrice(Document doc, String internalId, Double principalPrice, JSONObject comertial, JSONObject discountsJson) {
      Double spotlightPrice = super.scrapSpotlightPrice(doc, internalId, principalPrice, comertial, discountsJson);

      try {
         BankSlip bank = scrapBankSlip(principalPrice, comertial, discountsJson, false);

         if (bank.getFinalPrice() < spotlightPrice) {
            spotlightPrice = bank.getFinalPrice();
         }
      } catch (MalformedPricingException e) {
         Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
      }

      return spotlightPrice;
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

               String name = paymentJson.optString("paymentName");

               if (name.toLowerCase().contains("boleto")) {
                  if (paymentJson.has("installments")) {
                     JSONArray bankSlipInstallments = paymentJson.optJSONArray("installments");
                     for (Object i : bankSlipInstallments) {
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
