package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
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
   protected String storeId = getStoreId();
   protected String store;


   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   private static final String END_POINT_REQUEST = "https://api.gpa.digital/";


   public String getStoreId() {
      return session.getOptions().optString("storeId");
   }

   public GPACrawler(Session session) {
      super(session);
      inferFields();
      super.config.setFetcher(FetchMode.APACHE);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      BasicClientCookie cookie = new BasicClientCookie("STORE_ID", this.storeId);
      cookie.setDomain(
         homePageHttps.substring(homePageHttps.indexOf("www"), homePageHttps.length() - 1));
      cookie.setPath("/");
      this.cookies.add(cookie);
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
   protected Response fetchResponse() {
      Request request = Request.RequestBuilder.create()
         .setUrl(this.session.getOriginalURL())
         .setProxyservice(List.of(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY))
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(dataFetcher, new ApacheDataFetcher(), new JsoupDataFetcher()), session);
      return response;
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      String productUrl = session.getOriginalURL();


      JSONObject jsonSku = crawlProductInformatioFromGPAApi(productUrl);
      if (jsonSku.has("id")) {
         Logging.printLogDebug(
            logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawlInternalId(jsonSku);
         String internalPid = crawlInternalPid(jsonSku);
         List<String> categories = getCategories(jsonSku);
         String primaryImage = crawlPrimaryImage(jsonSku);
         String name = crawlName(jsonSku);
         RatingsReviews ratingsReviews = session.getMarket().getName().contains("extramarketplace") ? extractRatingAndReviews(internalId) : null;
         List<String> secondaryImages = crawlSecondaryImages(jsonSku);

         String redirectedToURL = session.getRedirectedToURL(productUrl);
         if (internalPid != null && redirectedToURL != null && !redirectedToURL.isEmpty()) {
            productUrl = redirectedToURL;
         }

         String description = crawlDescription(jsonSku, doc);

         Offers offers = scrapOffers(jsonSku, doc);

         Product product =
            ProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setOffers(offers)
               .setCategories(categories)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setRatingReviews(ratingsReviews)
               .build();

         products.add(product);

      } else if (jsonSku.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         JSONObject pageJson = CrawlerUtils.selectJsonFromHtml(doc, "#__NEXT_DATA__", null, null, false, false);
         JSONObject product = JSONUtils.getValueRecursive(pageJson, "props.initialProps.componentProps.product", JSONObject.class, new JSONObject());

         if (product != null && !product.isEmpty()) {
            String internalId = product.optString("id");
            String internalPid = product.optString("sku");
            List<String> categories = getCategories(product);
            String primaryImage = CrawlerUtils.completeUrl(product.optString("thumbPath"), "https", "static.paodeacucar.com");
            String name = product.optString("name");
            RatingsReviews ratingsReviews = session.getMarket().getName().contains("extramarketplace") ? extractRatingAndReviews(internalId) : null;
            List<String> secondaryImages = crawlSecondaryImages(product);
            String description = crawlDescription(product, doc);
            Offers offers = scrapOffers(product, doc);

            Product product1 =
               ProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setOffers(offers)
                  .setCategories(categories)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setDescription(description)
                  .setRatingReviews(ratingsReviews)
                  .build();

            products.add(product1);
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private String crawlDescription(JSONObject jsonSku, Document doc) {
      StringBuilder description = new StringBuilder();

      description.append(jsonSku.optString("description"));

      description.append(CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("div > div.MuiGrid-root.gridstyles-sc-6scn59-0.jAFMvU.MuiGrid-container")));

      return description.toString();
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
         for (String s : array.keySet()) {
            if (!s.equals("0")) {
               JSONObject jsonObject = array.optJSONObject(s);
               String image = jsonObject.optString("BIG");
               String imageUrl = new URIBuilder(homePageHttps).setPath(image).toString();
               secondaryImagesArray.add(imageUrl);
            }
         }
      }


      return secondaryImagesArray;
   }


   private List<String> getCategories(JSONObject object) {
      List<String> categories = new ArrayList<>();
      JSONArray arrayCategories = JSONUtils.getValueRecursive(object, "shelfList", JSONArray.class);
      if (arrayCategories != null && !arrayCategories.isEmpty()) {
         for (Object e : arrayCategories) {
            JSONObject objectCategory = (JSONObject) e;
            String category = objectCategory.optString("name");
            if (category != null && !category.isEmpty()) {
               categories.add(category);
            }
         }
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
      System.out.println(url);
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

   private String scrapSales(Pricing pricing, Document doc) {
      List<String> sales = new ArrayList<>();

      String diff = CrawlerUtils.calculateSales(pricing);

      if (diff != null && !diff.isEmpty()) {
         sales.add(diff);
      }

      String salesFromDoc = CrawlerUtils.scrapStringSimpleInfo(doc, ".seal-sale-box-divided__Label1-pf7r6x-1", true);

      if (salesFromDoc != null) {
         sales.add(salesFromDoc);
      }

      return sales.toString();
   }

   private Offers scrapOffers(JSONObject jsonSku, Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      JSONArray data = JSONUtils.getValueRecursive(jsonSku, "sellInfos", JSONArray.class);

      if (data != null) {
         for (Object o : data) {
            JSONObject sellerinfo = (JSONObject) o;
            Pricing pricing = scrapPricing(sellerinfo);
            String sales = scrapSales(pricing, doc);
            String sellerName = JSONUtils.getStringValue(sellerinfo, "name");
            boolean isMainRetailersMainRetailer = sellerinfo.optString("sellType", "").equals("1P");


            Offer offer = Offer.OfferBuilder.create()
               .setUseSlugNameAsInternalSellerId(true)
               .setSellerFullName(sellerName)
               .setSales(Collections.singletonList(sales))
               .setMainPagePosition(1)
               .setSellersPagePosition(1)
               .setIsBuybox(true)
               .setIsMainRetailer(isMainRetailersMainRetailer)
               .setPricing(pricing)
               .build();

            offers.add(offer);
         }
      }

      return offers;
   }

   private Pricing scrapPricing(JSONObject data) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(data, "currentPrice", true);
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(data, "priceFrom", true);

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
