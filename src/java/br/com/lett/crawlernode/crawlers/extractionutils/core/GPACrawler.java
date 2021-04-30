package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
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
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.net.URISyntaxException;
import java.util.*;

public class GPACrawler extends Crawler {

   protected String homePageHttps;
   protected String storeId;
   protected String store;
   protected String cep;
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   private static final String END_POINT_REQUEST = "https://api.gpa.digital/";


   public GPACrawler(Session session) {
      super(session);
      inferFields();
      super.config.setFetcher(FetchMode.FETCHER);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      if (this.cep != null) {
         fetchStoreId();
         BasicClientCookie cookie = new BasicClientCookie("ep.selected_store", this.storeId);
         cookie.setDomain(
            homePageHttps.substring(homePageHttps.indexOf("www"), homePageHttps.length() - 1));
         cookie.setPath("/");
         this.cookies.add(cookie);
      }
   }

   /**
    * Given a CEP it send a request to an API then returns the id used by GPA.
    */
   private void fetchStoreId() {

      String url = END_POINT_REQUEST + this.store + "/v2/delivery/ecom/" + this.cep.replace("-", "");

      Request request =
         RequestBuilder.create()
            .setUrl(url)
            .setCookies(cookies)
            .build();

      String response = this.dataFetcher.get(session, request).getBody();
      JSONObject jsonObjectGPA = JSONUtils.stringToJson(response);
      this.storeId = JSONUtils.getValueRecursive(jsonObjectGPA, "content.deliveryTypes.0.storeid", Integer.class).toString();
   }

   /**
    * Infers classes' fields by reflection
    */
   private void inferFields() {
      String className = this.getClass().getSimpleName().toLowerCase();
      if (className.contains("paodeacucar")) {
         this.store = "pa";
         this.homePageHttps = "https://www.paodeacucar.com/";
      } else if (className.contains("extra")) {
         this.store = "ex";
         this.homePageHttps = "https://www.clubeextra.com.br/";
      }
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      String productUrl = session.getOriginalURL();
      JSONObject jsonSku = crawlProductInformatioFromGPAApi(productUrl);

      if (jsonSku.has("id")) {
         Logging.printLogDebug(
            logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject data = JSONUtils.getValueRecursive(jsonSku, "sellInfos.0", JSONObject.class);

         String internalId = crawlInternalId(jsonSku);
         String internalPid = crawlInternalPid(jsonSku);
         CategoryCollection categories = crawlCategories(jsonSku);
         String description = jsonSku.optString("description");
         boolean available = data != null && crawlAvailability(data);
         Offers offers = available ? scrapOffers(data) : new Offers();

         String primaryImage = crawlPrimaryImage(jsonSku);
         String name = crawlName(jsonSku);
         RatingsReviews ratingsReviews = extractRatingAndReviews(internalId);
         List<String> secondaryImages = crawlSecondaryImages(jsonSku);

         String redirectedToURL = session.getRedirectedToURL(productUrl);
         if (internalPid != null && redirectedToURL != null && !redirectedToURL.isEmpty()) {
            productUrl = redirectedToURL;
         }

         Product product =
            ProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setOffers(offers)
               .setCategory1(categories.getCategory(0))
               .setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2))
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setRatingReviews(ratingsReviews)
               .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private String crawlInternalId(JSONObject json) {
      String internalId = null;

      if (json.has("id")) {
         internalId = json.get("id").toString();
      }

      return internalId;
   }

   private String crawlInternalPid(JSONObject json) {
      String internalPid = null;

      if (json.has("id")) {
         internalPid = json.getString("sku");
      }

      return internalPid;
   }

   private String crawlName(JSONObject json) {
      String name = null;

      if (json.has("name")) {
         name = json.getString("name");
      }

      return name;
   }

   private boolean crawlAvailability(JSONObject data) {
      return data.has("stock") && data.getBoolean("stock");
   }

   private String crawlPrimaryImage(JSONObject json) throws URISyntaxException {
      return new URIBuilder(homePageHttps).setPath((String) json.optQuery("/mapOfImages/0/BIG")).toString();

   }

   private List<String> crawlSecondaryImages(JSONObject json) throws URISyntaxException {
      List<String> secondaryImagesArray = new ArrayList<>();
      JSONObject array = JSONUtils.getJSONValue(json, "mapOfImages");
      if (array != null && array.length() > 1) {
         for (int i = 1; i < array.length(); i++) {
            JSONObject jsonObject = array.optJSONObject(Integer.toString(i));
            String image = jsonObject.optString("BIG");
            String imageUrl = new URIBuilder(homePageHttps).setPath(image).toString();
            secondaryImagesArray.add(imageUrl);
         }
      }


      return secondaryImagesArray;
   }


   private CategoryCollection crawlCategories(JSONObject json) {
      CategoryCollection categories = new CategoryCollection();

      if (json.has("shelfList")) {
         JSONArray shelfList = json.getJSONArray("shelfList");

         Set<String> listCategories =
            new HashSet<>(); // It is a "set" because it has been noticed that there are repeated
         // categories

         // The category fetched by crawler can be in a different ordination than showed on the website
         // and
         // its depends of each product.
         if (shelfList.length() > 0) {
            JSONObject cat1 = shelfList.getJSONObject(0);
            JSONObject cat2 = shelfList.getJSONObject(shelfList.length() - 1);

            if (cat1.has("name")) {
               listCategories.add(cat1.getString("name"));
            }

            if (cat2.has("name")) {
               listCategories.add(cat2.getString("name"));
            }
         }
         categories.addAll(listCategories);
      }

      return categories;
   }

   /**
    * Get the json of gpa api, this api has all info of product
    */
   private JSONObject crawlProductInformatioFromGPAApi(String productUrl) {
      JSONObject productsInfo = new JSONObject();

      String id = "";
      if (productUrl.startsWith(homePageHttps)) {
         id = productUrl.replace(homePageHttps, "").split("/")[1];
      }

      String url =
         END_POINT_REQUEST
            + this.store
            + "/v4/products/ecom/"
            + id
            + "/bestPrices"
            + "?isClienteMais=false";

      if (this.storeId != null) {
         url += "&storeId=" + this.storeId;
      }

      Request request = RequestBuilder.create()
         .setUrl(url).setCookies(cookies).build();
      String res = this.dataFetcher.get(session, request).getBody();

      JSONObject apiGPA = JSONUtils.stringToJson(res);
      if (apiGPA.optJSONObject("content") != null) {
         productsInfo = apiGPA.optJSONObject("content");
      }

      return productsInfo;
   }

   /**
    * Number of ratings appear in key rating in json
    */
   private Integer getTotalNumOfReviews(JSONObject rating) {
      int totalReviews = 0;

      if (rating.has("rating")) {
         JSONObject ratingValues = rating.getJSONObject("rating");

         totalReviews = 0;

         for (int i = 1; i <= ratingValues.length(); i++) {
            if (ratingValues.has(Integer.toString(i))) {
               totalReviews += ratingValues.getInt(Integer.toString(i));
            }
         }
      }
      return totalReviews;
   }

   private boolean isProductPage(String url) {
      return url.contains(this.homePageHttps + "produto/");
   }

   protected RatingsReviews extractRatingAndReviews(String internalId) {
      RatingsReviews ratingReviews = new RatingsReviews();
      Request request =
         RequestBuilder.create()
            .setUrl(END_POINT_REQUEST + store + "/products/" + internalId + "/review")
            .build();
      JSONObject jsonObject = JSONUtils.stringToJson(dataFetcher.get(session, request).getBody());

      if (jsonObject.has("content")) {
         JSONObject rating = jsonObject.optJSONObject("content");

         if (isProductPage(session.getOriginalURL())) {

            ratingReviews.setDate(session.getDate());
            Integer totalNumOfEvaluations = rating.optInt("total", 0);
            Integer totalReviews = getTotalNumOfReviews(rating);
            Double avgRating = rating.optDouble("average", 0D);

            ratingReviews.setTotalRating(totalNumOfEvaluations);
            ratingReviews.setTotalWrittenReviews(totalReviews);
            ratingReviews.setAverageOverallRating(avgRating);
            ratingReviews.setInternalId(crawlInternalId(session.getOriginalURL()));

            AdvancedRatingReview advancedRatingReview = getTotalStarsFromEachValue(rating);
            ratingReviews.setAdvancedRatingReview(advancedRatingReview);
         }
      }
      return ratingReviews;
   }

   public static AdvancedRatingReview getTotalStarsFromEachValue(JSONObject rating) {
      int star1 = 0;
      int star2 = 0;
      int star3 = 0;
      int star4 = 0;
      int star5 = 0;

      if (rating.has("rating")) {

         JSONObject histogram = rating.getJSONObject("rating");

         star1 = histogram.optInt("1");
         star2 = histogram.optInt("2");
         star3 = histogram.optInt("3");
         star4 = histogram.optInt("4");
         star5 = histogram.optInt("5");

      }

      return new AdvancedRatingReview.Builder()
         .totalStar1(star1)
         .totalStar2(star2)
         .totalStar3(star3)
         .totalStar4(star4)
         .totalStar5(star5)
         .build();
   }

   private String crawlInternalId(String productUrl) {
      return CommonMethods.getLast(productUrl.replace(this.homePageHttps, "").split("produto/"))
         .split("/")[0];
   }

   private Offers scrapOffers(JSONObject data) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      if (data != null) {
         Pricing pricing = scrapPricing(data);
         String sales = CrawlerUtils.calculateSales(pricing);
         String sellerName = JSONUtils.getStringValue(data, "name");

         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(sellerName)
            .setSales(Collections.singletonList(sales))
            .setMainPagePosition(1)
            .setSellersPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .build());
      }

      return offers;
   }

   private Pricing scrapPricing(JSONObject data) throws MalformedPricingException {
      Double spotlightPrice = null;
      Double priceFrom = null;

      if (data.has("productPromotions")) {
         JSONArray promotions = data.optJSONArray("productPromotions");
         for (Object e : promotions) {
            if (e instanceof JSONObject && ((JSONObject) e).optInt("ruleId") == 51241) {
               spotlightPrice = ((JSONObject) e).optDouble("unitPrice");
               priceFrom = data.optDouble("currentPrice");
            }
         }
      }
      if (spotlightPrice == null) {
         spotlightPrice = data.optDouble("currentPrice");
      }

      if (priceFrom == null && data.has("priceFrom")) {
         priceFrom = data.optDouble("priceFrom");
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(BankSlipBuilder.create()
            .setFinalPrice(spotlightPrice)
            .build())
         .build();

   }

   protected CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      for (String brand : cards) {
         creditCards.add(CreditCardBuilder.create()
            .setBrand(brand)
            .setIsShopCard(false)
            .setInstallments(installments)
            .build());
      }

      return creditCards;
   }
}
