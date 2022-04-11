package br.com.lett.crawlernode.crawlers.corecontent.unitedstates;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Parser;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
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
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class UnitedstatesLowesCrawler extends Crawler {
   private static final Set<String> cards = Sets.newHashSet(Card.DISCOVER.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString());
   private static final String MAIN_SELLER_NAME = "Lowes";
   private static final String HOME_PAGE = "https://www.lowes.com/";

   private String storeId = getStoreId();
   private String region = getRegion();

   private String getStoreId() {
      return session.getOptions().optString("storeId");
   }

   private String getRegion() {
      return session.getOptions().optString("region");
   }


   public UnitedstatesLowesCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.JSON);
      super.config.setFetcher(FetchMode.JSOUP);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      Request request = Request.RequestBuilder.create()
         .setProxyservice(Arrays.asList(ProxyCollection.NETNUT_RESIDENTIAL_US_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_UK, ProxyCollection.NETNUT_RESIDENTIAL_US, ProxyCollection.BUY_HAPROXY))
         .setUrl(HOME_PAGE)
         .setSendUserAgent(true)
         .build();

      Response response = dataFetcher.get(session, request);

      this.cookies.addAll(response.getCookies());

      BasicClientCookie storeIdCookie = new BasicClientCookie("sn", storeId);
      storeIdCookie.setDomain("www.lowes.com");
      storeIdCookie.setPath("/");
      this.cookies.add(storeIdCookie);

      BasicClientCookie regionCookie = new BasicClientCookie("region", region);
      regionCookie.setDomain("www.lowes.com");
      regionCookie.setPath("/");
      this.cookies.add(regionCookie);
   }

   @Override
   protected Response fetchResponse() {
      String productId = CommonMethods.getLast(session.getOriginalURL().split("/"));
      String url = "https://www.lowes.com/pd/" + productId + "/productdetail/" + storeId + "/Guest";
      return fetchProductResponse(url);
   }

   protected Response fetchProductResponse(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("authority", HOME_PAGE);
      headers.put("accept", "application/json, text/plain, */*");
      headers.put("referer", session.getOriginalURL());

      Request request = Request.RequestBuilder.create()
         .setProxyservice(Arrays.asList(ProxyCollection.NETNUT_RESIDENTIAL_US_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_UK, ProxyCollection.NETNUT_RESIDENTIAL_US, ProxyCollection.BUY_HAPROXY))
         .setUrl(url)
         .setSendUserAgent(true)
         .setHeaders(headers)
         .setCookies(cookies)
         .build();

      return dataFetcher.get(session, request);
   }

   protected JSONObject fetchProductJson(String internalId) {
      String url = "https://www.lowes.com/pd/" + internalId + "/productdetail/" + storeId + "/Guest";
      Response response = fetchProductResponse(url);

      JSONObject json = new JSONObject();

      try {
         json = JSONUtils.stringToJson(response.getBody());
      } catch (Exception e) {
         Logging.printLogWarn(logger, CommonMethods.getStackTrace(e));
         return json;
      }

      JSONObject inventory = json.optJSONObject("inventory");

      if (inventory == null || inventory.isEmpty()) {
         Logging.printLogInfo(logger, session, "Fetched to fetch complete product json");
      }

      return json;
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      List<Product> products = new ArrayList<>();

      if (json != null) {
         String internalId = json.optString("productId");

         Product product = extractProduct(json, internalId);
         if (product != null) products.add(product);

         List<String> variationsIds = scrapVariationsIds(json, internalId);

         if (!variationsIds.isEmpty()) {
            for (String variationId : variationsIds) {
               if (!Objects.equals(variationId, internalId)) {
                  JSONObject variationJson = fetchProductJson(variationId);
                  Product variationProduct = extractProduct(variationJson, variationId);
                  if (variationProduct != null) products.add(variationProduct);
               }
            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page:   " + this.session.getOriginalURL());
      }

      return products;
   }

   private List<String> scrapVariationsIds(JSONObject json, String internalId) {
      List<String> variationsIds = new ArrayList<>();

      Object variantsArray = json.optQuery("/productDetails/" + internalId + "/product/normalizeVariant/Manufacturer Color~1Finish/values");

      if (variantsArray instanceof JSONArray) {
         JSONArray variants = (JSONArray) variantsArray;
         for (int i = 0; i < variants.length(); i++) {
            JSONObject variant = variants.getJSONObject(i);
            String variationId = variant.optString("id");
            variationsIds.add(variationId);
         }
         variationsIds.remove(internalId);
      }

      return variationsIds;
   }

   private Product extractProduct(JSONObject json, String internalId) throws MalformedProductException, OfferException, MalformedPricingException {
      if (json != null) {
         JSONObject productDetails = (JSONObject) json.optQuery("/productDetails/" + internalId);

         if (productDetails == null) {
            return null;
         }

         JSONObject productJSON = productDetails.optJSONObject("product");

         String internalPid = productJSON.optString("omniItemId");
         String name = productJSON.optString("title");
         String url = scrapURL(productJSON);
         String description = scrapDescription(productJSON);
         List<String> images = scrapImages(productJSON);
         String primaryImage = images.remove(0);
         Integer stock = scrapStock(json);
         List<String> eans = Collections.singletonList(productJSON.optString("barcode"));
         Offers offers = stock > 0 ? scrapOffers(productDetails) : new Offers();
         RatingsReviews ratingsReviews = scrapRatingReviews(json, internalId);

         return ProductBuilder.create()
            .setUrl(url)
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setStock(stock)
            .setDescription(description)
            .setEans(eans)
            .setOffers(offers)
            .setRatingReviews(ratingsReviews)
            .build();
      }
      return null;
   }

   private String scrapURL(JSONObject productJSON) {
      String url = productJSON.optString("pdURL");
      if (url.isEmpty()) {
         return null;
      }
      return HOME_PAGE + url;
   }

   private List<String> scrapImages(JSONObject productJSON) {
      List<String> images = new ArrayList<>();

      Object primaryImage = productJSON.optQuery("/imageUrls/0/value");
      if (primaryImage instanceof String) {
         images.add(CrawlerUtils.completeUrl((String) primaryImage, "https", "mobileimages.lowes.com") + "?size=pdhism");
      }

      Object secondaryImages = productJSON.optQuery("/epc/additionalImages");
      if (secondaryImages instanceof JSONArray) {
         JSONArray secondaryImagesArray = (JSONArray) secondaryImages;
         for (int i = 0; i < secondaryImagesArray.length(); i++) {
            JSONObject secondaryImage = secondaryImagesArray.getJSONObject(i);
            String image = secondaryImage.optString("baseUrl");
            images.add(CrawlerUtils.completeUrl(image, "https", "mobileimages.lowes.com") + "?size=pdhism");
         }
      }

      return images;
   }

   private Integer scrapStock(JSONObject json) {
      int stock = 0;

      Object stockObj = json.optQuery("/inventory/totalAvailableQty");
      if (stockObj instanceof Integer) {
         stock = (Integer) stockObj;
      }

      return stock;
   }

   private String scrapDescription(JSONObject productJSON) {
      StringBuilder description = new StringBuilder();

      String romanceCopy = productJSON.optString("romanceCopy");
      if (!romanceCopy.isEmpty()) {
         ByteBuffer buffer = StandardCharsets.UTF_8.encode(romanceCopy);
         description.append(StandardCharsets.UTF_8.decode(buffer).toString()).append("\n");
      }

      JSONArray marketingBullets = productJSON.optJSONArray("marketingBullets");
      if (marketingBullets != null) {
         for (int i = 0; i < marketingBullets.length(); i++) {
            JSONObject marketingBullet = marketingBullets.getJSONObject(i);
            String bullet = marketingBullet.optString("value");
            ByteBuffer buffer = StandardCharsets.UTF_8.encode(bullet);
            description.append(StandardCharsets.UTF_8.decode(buffer).toString()).append("\n");
         }
      }

      return description.toString();
   }

   private Offers scrapOffers(JSONObject productDetails) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      Pricing pricing = scrapPricing(productDetails);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(MAIN_SELLER_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject productDetails) throws MalformedPricingException {
      JSONObject pricingJSON = productDetails.optJSONObject("price");

      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(pricingJSON, "itemPrice", false);
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(pricingJSON, "wasPrice", true);

      CreditCards creditCards = CrawlerUtils.scrapCreditCards(spotlightPrice, cards);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }

   private RatingsReviews scrapRatingReviews(JSONObject json, String internalId) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Object ratingObj = json.optQuery("/ratings/" + internalId);

      if (ratingObj instanceof JSONObject) {
         JSONObject rating = (JSONObject) ratingObj;

         Integer totalNumOfEvaluations = rating.optInt("reviewCount");
         Double avgRating = JSONUtils.getDoubleValueFromJSON(rating, "rating", true);
         AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(internalId);

         ratingReviews.setTotalRating(totalNumOfEvaluations);
         ratingReviews.setAverageOverallRating(avgRating);
         ratingReviews.setAdvancedRatingReview(advancedRatingReview);
      }

      return ratingReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(String internalId) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      JSONObject json = null;

      try {
         String url = "https://www.lowes.com/rnr/r/get-by-product/" + internalId + "/seopdp/prod";
         Response response = fetchProductResponse(url);
          json = new JSONObject(response.getBody());
      } catch (Exception e) {
         Logging.printLogWarn(logger, CommonMethods.getStackTrace(e));
      }


      if(json != null && json.has("ReviewStatistics")) {
         Object ratingDistribution = json.optQuery("/ReviewStatistics/RatingDistribution");
         if(ratingDistribution instanceof JSONArray) {
            JSONArray ratingDistributionJSON = (JSONArray) ratingDistribution;
            for (int i = 0; i < ratingDistributionJSON.length(); i++) {
               JSONObject ratingStarJSON = ratingDistributionJSON.getJSONObject(i);
               int star = ratingStarJSON.optInt("RatingValue");
               int count = ratingStarJSON.optInt("Count");
               switch (star) {
                  case 1:
                     star1 = count;
                     break;
                  case 2:
                     star2 = count;
                     break;
                  case 3:
                     star3 = count;
                     break;
                  case 4:
                     star4 = count;
                     break;
                  case 5:
                     star5 = count;
                     break;
                  default:
                     break;
               }
            }

         }
      }

      return new AdvancedRatingReview.Builder()
         .totalStar1(star1)
         .totalStar2(star2)
         .totalStar3(star3)
         .totalStar4(star4)
         .totalStar5(star5)
         .build();
   }
}
