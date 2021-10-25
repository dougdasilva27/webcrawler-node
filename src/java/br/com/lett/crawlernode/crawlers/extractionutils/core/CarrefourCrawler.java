package br.com.lett.crawlernode.crawlers.extractionutils.core;

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
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip;
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.yaml.snakeyaml.util.UriEncoder;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CarrefourCrawler extends VTEXNewScraper {

   private static final List<String> SELLERS = Collections.singletonList("Carrefour");
   public static final String HOME_PAGE = "https://mercado.carrefour.com.br/";
   private JSONArray crawlerApi;
   private JSONObject productObject;

   public CarrefourCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
   }

   @Override
   protected String getHomePage() {
      return homePage;
   }

   /**
    * Usually it is the "vtex_segment" cookie
    *
    * @return location token string
    */
   protected String getLocationToken(){
      return session.getOptions().optString("vtex_segment");
   }

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
            ProxyCollection.BUY,
            ProxyCollection.LUMINATI_SERVER_BR,
            ProxyCollection.NO_PROXY)
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
   protected String scrapPidFromApi(Document doc) {
      String internalPid = super.scrapPidFromApi(doc);
      if (internalPid == null) {
         JSONObject json = crawlProductApi(internalPid, null);
         internalPid = json.optString("id");
      }
      return internalPid;
   }


   @Override
   protected JSONObject crawlProductApi(String internalPid, String parameters) {


      JSONObject productApi = new JSONObject();

      if (productObject != null && !productObject.isEmpty()) {
         productApi = productObject;
      } else {
         if (session.getOriginalURL().contains("carrefour.com.ar")) {
            session.setOriginalURL(session.getOriginalURL().replace("supermercado", "www"));


            String path = session.getOriginalURL().replace(homePage, "").toLowerCase();

            String url = homePage + "api/catalog_system/pub/products/search/" + path;

            if (crawlerApi == null) {
               String body = fetchPage(url);
               crawlerApi = CrawlerUtils.stringToJsonArray(body);
            }

            if (!crawlerApi.isEmpty()) {
               productApi = crawlerApi.optJSONObject(0) == null ? new JSONObject() : crawlerApi.optJSONObject(0);
            } else {
               productApi = super.crawlProductApi(internalPid, parameters);
            }
         } else {

            String hash = "b082ca6ae008539025025f4d5d400982b7ca216375467a82bad61395d389a5f2";
            String productSlug = regex("//.*[/](.*)/p", session.getOriginalURL());

            String extensions = "{\"persistedQuery\":{\"sha256Hash\":\"" + hash + "\"}}";
            String variables = "{\"slug\":\"" + productSlug + "\"}";
            String variablesBase64 = Base64.getEncoder().encodeToString(variables.getBytes());

            StringBuilder url = new StringBuilder();
            url.append("https://mercado.carrefour.com.br/graphql/?operationName=BrowserProductPageQuery&extensions=");
            url.append(extensions);
            url.append("&variables=");
            url.append(variablesBase64);

            String urlEncoded = UriEncoder.encode(url.toString());

            String body = fetchPage(urlEncoded);
            JSONObject api = CrawlerUtils.stringToJSONObject(body);
            productApi = JSONUtils.getValueRecursive(api, "data.vtex.product", JSONObject.class);
            productObject = productApi;
         }
      }
      return productApi;
   }

   @Override
   protected Offers scrapOffer(Document doc, JSONObject jsonSku, String internalId, String internalPid) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      JSONArray sellers = jsonSku.optJSONArray("sellers");
      if (sellers != null) {
         int position = 1;
         for (Object o : sellers) {
            JSONObject offerJson = o instanceof JSONObject ? (JSONObject) o
               : new JSONObject();
            JSONObject commertialOffer = offerJson.optJSONObject("commercialOffer");
            String sellerFullName = "Carrefour";
            boolean isDefaultSeller = offerJson.optBoolean("sellerDefault", true);

            if (commertialOffer != null && sellerFullName != null) {
               Integer stock = commertialOffer.optInt("availableQuantity");

               if (stock > 0) {
                  JSONObject discounts = scrapDiscounts(commertialOffer);

                  boolean isBuyBox = sellers.length() > 1;
                  boolean isMainRetailer = isMainRetailer(sellerFullName);

                  Pricing pricing = scrapPricing(doc, internalId, commertialOffer, discounts);
                  List<String> sales = isDefaultSeller ? scrapSales(doc, offerJson, internalId, internalPid, pricing) : new ArrayList<>();

                  offers.add(Offer.OfferBuilder.create()
                     .setUseSlugNameAsInternalSellerId(true)
                     .setSellerFullName(sellerFullName)
                     .setMainPagePosition(position)
                     .setIsBuybox(isBuyBox)
                     .setIsMainRetailer(isMainRetailer)
                     .setPricing(pricing)
                     .setSales(sales)
                     .build());

                  position++;
               }
            }
         }
      }

      return offers;
   }

   @Override
   protected Pricing scrapPricing(Document doc, String internalId, JSONObject comertial, JSONObject discountsJson) throws MalformedPricingException {
      Double principalPrice = comertial.optDouble("spotPrice");
      Double priceFrom = comertial.optDouble("listPrice");

      CreditCards creditCards = scrapCreditCards(comertial, discountsJson, true);
      BankSlip bankSlip = scrapBankSlip(principalPrice, comertial, discountsJson, true);

      Double spotlightPrice = scrapSpotlightPrice(doc, internalId, principalPrice, comertial, discountsJson);
      if (priceFrom != null && spotlightPrice != null && spotlightPrice.equals(priceFrom)) {
         priceFrom = null;
      }

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private String regex(String pathern, String str) {

      final Pattern pattern = Pattern.compile(pathern, Pattern.MULTILINE);
      final Matcher matcher = pattern.matcher(str);

      String result = null;
      if (matcher.find()) {
         result = matcher.group(1);
      }
      return result;
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
