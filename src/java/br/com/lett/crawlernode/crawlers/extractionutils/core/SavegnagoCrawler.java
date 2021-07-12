package br.com.lett.crawlernode.crawlers.extractionutils.core;


import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.*;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class SavegnagoCrawler extends Crawler {

   private final String HOME_PAGE = "https://www.savegnago.com.br/";
   private final String SELLER_NAME = "Savegnago Supermercados";
   private final String CITY_CODE = getCityCode();

   //This token never changes. BUT, if necessary, we can get the token using the 'getAppToken' method
   protected String APP_TOKEN = "DWEYGZH2K4M5N7Q8R9TBUCVEXFYG2J3K4N6P7Q8SATBUDWEXFZH2J3M5N6";

   protected abstract String getCityCode();

   public SavegnagoCrawler(Session session) {
      super(session);
   }

   protected String getHomePage() {
      return HOME_PAGE;
   }

   //If the app token changes, we can use this method to get it
   protected void getAppToken() {
      String url = "https://savegnago.com.br/_next/static/chunks/950816df62fa2d85fc3ac8e13cd05335a6db61c7.1e900a1ca2420496572b.js";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .build();

      String jsResponse = this.dataFetcher.get(session, request).getBody();
      Pattern regexAppToken = Pattern.compile("APP_TOKEN:\\\"(.*?)\\\"");

      Matcher matcher = regexAppToken.matcher(jsResponse);

      if (matcher.find()) {
         APP_TOKEN = matcher.group(1);
      }
   }

   @Override
   protected Object fetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put("app-token", APP_TOKEN);
      headers.put("app-key", "betaappkey-savegnago-desktop");

      String internalId = CommonMethods.getLast(session.getOriginalURL().split("/"));

      String url = "https://api.savegnago.com.br/product?productId=" + internalId + "&salesChannel=" + CITY_CODE + "&wareHouseId=" + CITY_CODE;

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setHeaders(headers)
         .build();

      Response apiResponse = this.dataFetcher.get(session, request);

      return JSONUtils.stringToJson(apiResponse.getBody());
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();

      if (json.has("id")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = json.optString("id");
         String name = json.optString("name");
         List<String> images = scrapImages(json);
         String primaryImage = !images.isEmpty() ? images.remove(0) : "";
         String description = json.optString("description");

         boolean available = json.optString("status").equals("AVAILABLE");
         int stock = json.optInt("availableQuantity");
         Offers offers = available ? scrapOffers(json) : new Offers();

         RatingsReviews ratings = scrapRating(internalPid);

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalPid)
            .setInternalPid(internalPid)
            .setName(name)
            .setOffers(offers)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setDescription(description)
            .setStock(stock)
            .setRatingReviews(ratings)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   protected List<String> scrapImages(JSONObject json) {
      List<String> imgs = new ArrayList<>();

      JSONArray imgsArray = json.optJSONArray("images");

      for (Object img : imgsArray) {
         if (img instanceof JSONObject) {
            imgs.add(((JSONObject) img).optString("url"));
         }
      }

      return imgs;
   }

   protected Offers scrapOffers(JSONObject json) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(json);
      List<String> sales = new ArrayList<>();
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSales(sales)
         .setSellerFullName(SELLER_NAME)
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(json, "price", false);
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(json, "oldPrice", false);

      if(priceFrom.equals(0d)){
         priceFrom = null;
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlighPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlighPrice)
         .build());

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   protected RatingsReviews scrapRating(String internalPid) {
      RatingsReviews reviews = new RatingsReviews();

      YourreviewsRatingCrawler yrRC = new YourreviewsRatingCrawler(session, cookies, logger, "d23c4a07-61d5-43d3-97da-32c0680a32b8", dataFetcher);
      Document docRating = yrRC.crawlPageRatingsFromYourViews(internalPid, "d23c4a07-61d5-43d3-97da-32c0680a32b8", dataFetcher);
      Integer totalNumOfEvaluations = yrRC.getTotalNumOfRatingsFromYourViews(docRating);
      Double avgRating = yrRC.getTotalAvgRatingFromYourViews(docRating);

      AdvancedRatingReview adRating = yrRC.getTotalStarsFromEachValue(internalPid);

      reviews.setTotalRating(totalNumOfEvaluations);
      reviews.setAverageOverallRating(avgRating);
      reviews.setTotalWrittenReviews(totalNumOfEvaluations);
      reviews.setAdvancedRatingReview(adRating);
      reviews.setDate(session.getDate());

      return reviews;
   }


}
