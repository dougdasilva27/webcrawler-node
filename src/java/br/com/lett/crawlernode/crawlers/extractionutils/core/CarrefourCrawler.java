package br.com.lett.crawlernode.crawlers.extractionutils.core;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import models.AdvancedRatingReview;
import models.RatingsReviews;
import models.pricing.BankSlip;

public abstract class CarrefourCrawler extends VTEXNewScraper {

   private static final List<String> SELLERS = Collections.singletonList("Carrefour");
   private JSONArray crawlerApi;

   public CarrefourCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.JAVANET);
   }

   protected abstract String getLocationToken();

   protected String getCep() {
      return null;
   }

   protected String fetchPage(String url) {

      Map<String, String> headers = new HashMap<>();

      String token = getLocationToken();

      String userLocationData = getCep();
      headers.put("accept", "*/*");

      StringBuilder cookiesBuilder = new StringBuilder();
      if (token != null) {
         cookiesBuilder.append("vtex_segment=").append(token).append(";");
      }
      if (userLocationData != null) {
         cookiesBuilder.append("userLocationData=").append(userLocationData).append(";");
      }
      headers.put("cookie", cookiesBuilder.toString());

      Request request = RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setSendUserAgent(false)
         .mustSendContentEncoding(false)
         .setFetcheroptions(
            FetcherOptionsBuilder.create()
               .mustUseMovingAverage(false)
               .mustRetrieveStatistics(true)
               .build())
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.INFATICA_RESIDENTIAL_BR,
            ProxyCollection.LUMINATI_SERVER_BR)
         )
         .build();

      Response response = alternativeFetch(request);

      return response.getBody();
   }

   protected Response alternativeFetch(Request request) {
      List<DataFetcher> dataFetchers = Arrays.asList(new ApacheDataFetcher(), new JsoupDataFetcher());

      Response response = null;

      for (DataFetcher localDataFetcher : dataFetchers) {
         response = localDataFetcher.get(session, request);
         if (checkResponse(response)) {
            return response;
         }
      }

      return response;
   }

   boolean checkResponse(Response response) {
      int statusCode = response.getLastStatusCode();

      return (Integer.toString(statusCode).charAt(0) == '2'
         || Integer.toString(statusCode).charAt(0) == '3'
         || statusCode == 404);
   }

   @Override
   protected Object fetch() {

      return Jsoup.parse(fetchPage(session.getOriginalURL()));
   }

   @Override
   protected String scrapInternalpid(Document doc) {
      String internalPid = super.scrapInternalpid(doc);
      if (internalPid == null) {
         JSONObject json = crawlProductApi(internalPid, null);
         internalPid = json.optString("productId");
      }
      return internalPid;
   }

   @Override
   protected JSONObject crawlProductApi(String internalPid, String parameters) {
      JSONObject productApi = new JSONObject();

      String path = session.getOriginalURL().replace(homePage, "").toLowerCase();

      String url = homePage + "api/catalog_system/pub/products/search/" + path;

      if (crawlerApi == null) {
         String body = fetchPage(url);
         crawlerApi = CrawlerUtils.stringToJsonArray(body);
      }

      if (!crawlerApi.isEmpty()) {
         productApi = crawlerApi.optJSONObject(0) == null ? new JSONObject() : crawlerApi.optJSONObject(0);
      }

      return productApi;
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
   protected String scrapDescription(Document doc, JSONObject productJson) throws UnsupportedEncodingException {
      return (JSONUtils.getStringValue(productJson, "description") + "\n" + scrapSpecsDescriptions(productJson)).trim();
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
      JSONObject response = CrawlerUtils.stringToJson(new ApacheDataFetcher().get(session, request).getBody());

      int totalNumberOfReviews = JSONUtils.getIntegerValueFromJSON(response, "total", 0);
      JSONObject aggregateRating = JSONUtils.getJSONValue(response, "aggregateRating");
      Double avgRating = JSONUtils.getDoubleValueFromJSON(aggregateRating, "ratingValue", false);
      JSONObject stars = JSONUtils.getJSONValue(aggregateRating, "ratingComposition");

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
