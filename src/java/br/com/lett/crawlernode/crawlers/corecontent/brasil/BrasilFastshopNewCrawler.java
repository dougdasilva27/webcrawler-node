package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilFastshopCrawlerUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.Seller;
import models.Util;
import models.prices.Prices;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

public class BrasilFastshopNewCrawler {

   private Logger logger;
   private Session session;
   private List<Cookie> cookies;
   private DataFetcher dataFetcher;

   public BrasilFastshopNewCrawler(Session session, Logger logger2, List<Cookie> cookies, DataFetcher dataFetcher) {
      this.session = session;
      this.logger = logger2;
      this.cookies = cookies;
      this.dataFetcher = dataFetcher;
   }

   private static final String SELLER_NAME_LOWER = "fastshop";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public List<Product> crawlProductsNewWay() throws Exception {
      List<Product> products = new ArrayList<>();
      String internalPid = BrasilFastshopCrawlerUtils.crawlPartnerId(session);
      JSONObject productAPIJSON = BrasilFastshopCrawlerUtils.crawlApiJSON(internalPid, session, cookies, dataFetcher);
      if (productAPIJSON.length() > 0) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         CategoryCollection categories = new CategoryCollection(); // was not found categories in this market
         StringBuilder description = crawlDescription(internalPid);
         Integer stock = null;

         // sku data in json
         JSONArray arraySkus = productAPIJSON != null && productAPIJSON.has("voltage") ? productAPIJSON.getJSONArray("voltage") : new JSONArray();

         for (int i = 0; i < arraySkus.length(); i++) {
            JSONObject variationJson = arraySkus.getJSONObject(i);

            String internalId = crawlInternalId(variationJson);
            JSONObject skuAPIJSON = productAPIJSON;

            if (arraySkus.length() > 1 && productAPIJSON.has("buyable") && productAPIJSON.getBoolean("buyable")) { // In case the array only has 1 sku.
               skuAPIJSON = BrasilFastshopCrawlerUtils.crawlApiJSON(variationJson.has("partNumber") ? variationJson.getString("partNumber") : null,
                     session, cookies, dataFetcher);

               if (skuAPIJSON.length() < 1) {
                  skuAPIJSON = productAPIJSON;
               }
            }

            String primaryImage = crawlPrimaryImage(skuAPIJSON);
            String name = crawlName(skuAPIJSON, variationJson) + " - " + internalPid;
            String secondaryImages = crawlSecondaryImages(skuAPIJSON);
            description.append(skuAPIJSON.has("longDescription") ? skuAPIJSON.get("longDescription") : "");
            boolean pageAvailability = crawlAvailability(skuAPIJSON);
            JSONObject jsonPrices =
                  pageAvailability ? BrasilFastshopCrawlerUtils.fetchPrices(internalPid, true, session, logger, dataFetcher) : new JSONObject();
            Offers offers = scrapOffers(jsonPrices, internalPid);
            RatingsReviews ratingReviews = crawlRatingReviews(internalPid);

            // Creating the product
            Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setCategory1(categories.getCategory(0))
                  .setCategory2(categories.getCategory(1))
                  .setCategory3(categories.getCategory(2))
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setDescription(description.toString())
                  .setStock(stock)
                  .setRatingReviews(ratingReviews)
                  .setOffers(offers)
                  .build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   /*******
    * General methods *
    *******/

   private String crawlInternalId(JSONObject json) {
      String internalId = null;

      if (json.has("catEntry")) {
         internalId = json.getString("catEntry");
      }

      return internalId;
   }

   private String crawlName(JSONObject apiJson, JSONObject skuJson) {
      StringBuilder name = new StringBuilder();

      if (apiJson.has("shortDescription")) {
         name.append(apiJson.get("shortDescription"));

         if (apiJson.has("parentPartNumber")) {
            name.append(" - " + apiJson.get("parentPartNumber"));
         }

         if (skuJson.has("name")) {
            String variationName = skuJson.getString("name");

            if (!name.toString().contains(variationName)) {
               name.append(" " + variationName);
            }
         }
      }

      return name.toString();
   }

   public Float crawlMainPagePrice(Prices prices) {
      Float price = null;

      if (prices != null && !prices.isEmpty()) {
         Double priceDouble = prices.getBankTicketPrice();
         price = priceDouble.floatValue();
      }

      return price;
   }

   public Marketplace assembleMarketplaceFromMap(Map<String, Prices> marketplaceMap) {
      Marketplace marketplace = new Marketplace();

      for (Entry<String, Prices> seller : marketplaceMap.entrySet()) {
         String sellerName = seller.getKey();
         if (!sellerName.equalsIgnoreCase(SELLER_NAME_LOWER)) {
            Prices prices = seller.getValue();

            JSONObject sellerJSON = new JSONObject();
            sellerJSON.put("name", sellerName);
            sellerJSON.put("prices", prices.toJSON());

            if (!prices.isEmpty() && prices.getCardPaymentOptions(Card.VISA.toString()).containsKey(1)) {
               Double price = prices.getCardPaymentOptions(Card.VISA.toString()).get(1);
               Float priceFloat = MathUtils.normalizeTwoDecimalPlaces(price.floatValue());

               sellerJSON.put("price", priceFloat);

               try {
                  Seller s = new Seller(sellerJSON);
                  marketplace.add(s);
               } catch (Exception e) {
                  Logging.printLogError(logger, session, Util.getStackTraceString(e));
               }
            }
         }
      }

      return marketplace;
   }

   private RatingsReviews crawlRatingReviews(String partnerId) {
      RatingsReviews ratingReviews = new RatingsReviews();

      ratingReviews.setDate(session.getDate());

      final String bazaarVoicePassKey = "caw1ZMlxPTUHLUFtjzQeE602umnQqFlKyTwhRjlDvuTac";
      String endpointRequest = assembleBazaarVoiceEndpointRequest(partnerId, bazaarVoicePassKey, 0, 50);

      Request request = RequestBuilder.create().setUrl(endpointRequest).setCookies(cookies).build();
      JSONObject ratingReviewsEndpointResponse = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
      JSONObject reviewStatistics = getReviewStatisticsJSON(ratingReviewsEndpointResponse, partnerId);
      AdvancedRatingReview advRating = scrapAdvancedRatingReview(reviewStatistics);
      Integer total = getTotalReviewCount(ratingReviewsEndpointResponse);

      ratingReviews.setTotalRating(total);
      ratingReviews.setTotalWrittenReviews(total);
      ratingReviews.setAverageOverallRating(getAverageOverallRating(reviewStatistics));
      ratingReviews.setAdvancedRatingReview(advRating);

      return ratingReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(JSONObject JsonRating) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;


      if (JsonRating.has("RatingDistribution")) {
         JSONArray ratingDistribution = JsonRating.getJSONArray("RatingDistribution");

         for (int i = 0; i < ratingDistribution.length(); i++) {
            JSONObject rV = ratingDistribution.getJSONObject(i);


            int val1 = rV.getInt("RatingValue");
            int val2 = rV.getInt("Count");

            switch (val1) {
               case 5:
                  star5 = val2;
                  break;
               case 4:
                  star4 = val2;
                  break;
               case 3:
                  star3 = val2;
                  break;
               case 2:
                  star2 = val2;
                  break;
               case 1:
                  star1 = val2;
                  break;
               default:
                  break;
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

   private Integer getTotalReviewCount(JSONObject reviewStatistics) {
      Integer totalReviewCount = 0;

      JSONArray results = JSONUtils.getJSONArrayValue(reviewStatistics, "Results");
      for (Object result : results) {
         JSONObject resultObject = (JSONObject) result;

         String locale = JSONUtils.getStringValue(resultObject, "ContentLocale");
         if (locale != null && locale.equalsIgnoreCase("pt_BR")) { // this happen because fastshop only show reviews from brasil
            totalReviewCount++;
         }
      }

      return totalReviewCount;
   }

   private Double getAverageOverallRating(JSONObject reviewStatistics) {
      Double avgOverallRating = 0d;
      if (reviewStatistics.has("AverageOverallRating")) {
         avgOverallRating = reviewStatistics.getDouble("AverageOverallRating");
      }
      return avgOverallRating;
   }

   private String assembleBazaarVoiceEndpointRequest(String skuInternalPid, String bazaarVoiceEnpointPassKey, Integer offset, Integer limit) {

      StringBuilder request = new StringBuilder();

      request.append("http://api.bazaarvoice.com/data/reviews.json?apiversion=5.5");
      request.append("&passkey=" + bazaarVoiceEnpointPassKey);
      request.append("&Offset=" + offset);
      request.append("&Limit=" + limit);
      request.append("&Sort=SubmissionTime:desc");
      request.append("&Filter=ProductId:" + skuInternalPid);
      request.append("&Include=Products");
      request.append("&Stats=Reviews");

      return request.toString();
   }


   private JSONObject getReviewStatisticsJSON(JSONObject ratingReviewsEndpointResponse, String skuInternalPid) {
      if (ratingReviewsEndpointResponse.has("Includes")) {
         JSONObject includes = ratingReviewsEndpointResponse.getJSONObject("Includes");

         if (includes.has("Products")) {
            JSONObject products = includes.getJSONObject("Products");

            if (products.has(skuInternalPid)) {
               JSONObject product = products.getJSONObject(skuInternalPid);

               if (product.has("ReviewStatistics")) {
                  return product.getJSONObject("ReviewStatistics");
               }
            }
         }
      }

      return new JSONObject();
   }


   private boolean crawlAvailability(JSONObject json) {
      if (json.has("buyable")) {
         return json.getBoolean("buyable");
      }
      return false;
   }

   private String crawlPrimaryImage(JSONObject json) {
      String primaryImage = null;

      if (json.has("images")) {
         JSONArray images = json.getJSONArray("images");

         if (images.length() > 0) {
            JSONObject imageJson = images.getJSONObject(0);

            if (imageJson.has("path")) {
               primaryImage = imageJson.getString("path");

               if (!primaryImage.startsWith("http")) {
                  primaryImage = "https://prdresources1-a.akamaihd.net/wcsstore/" + primaryImage;
               }
            }
         }
      }

      return primaryImage;
   }

   private String crawlSecondaryImages(JSONObject json) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      if (json.has("images")) {
         JSONArray images = json.getJSONArray("images");

         for (int i = 1; i < images.length(); i++) {
            JSONObject imageJson = images.getJSONObject(i);

            if (imageJson.has("path")) {
               String image = imageJson.getString("path");

               if (!image.startsWith("http")) {
                  image = "https://prdresources1-a.akamaihd.net/wcsstore/" + image;
               }

               secondaryImagesArray.put(image);
            }
         }
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   private StringBuilder crawlDescription(String partnerId) {
      StringBuilder description = new StringBuilder();

      String url = "https://www.fastshop.com.br/webapp/wcs/stores/servlet/SpotsContentView?type=content&hotsite=fastshop&catalogId=11052"
            + "&langId=-6&storeId=10151&emsName=SC_" + partnerId + "_Conteudo";
      Request request = RequestBuilder.create().setUrl(url).build();
      Document doc = Jsoup.parse(this.dataFetcher.get(session, request).getBody());

      String urlDESC = "https://www.fastshop.com.br/wcs/resources/v2/spots/ProductDetail_" + partnerId;
      Request requestDesc = RequestBuilder.create().setUrl(urlDESC).build();
      Document docDesc = Jsoup.parse(this.dataFetcher.get(session, requestDesc).getBody());

      if (!docDesc.toString().contains("errorCode")) {
         description.append(docDesc);
      }

      Element iframe = doc.select("iframe").first();

      if (iframe != null) {
         Request requestIframe = RequestBuilder.create().setUrl(iframe.attr("src")).build();
         description.append(Jsoup.parse(this.dataFetcher.get(session, requestIframe).getBody()));
      }

      return description;
   }

   // new model of offers

   private Offers scrapOffers(JSONObject jsonPrice, String internalPid) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(jsonPrice, internalPid);
      List<String> sales = new ArrayList<>(); // When this new offer model was implemented, no sales was found

      offers.add(OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_NAME_LOWER)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
            .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject jsonPrices, String internalPid) throws MalformedPricingException {
      Double priceFrom = scrapPriceFrom(internalPid);
      Double spotlightPrice = spotlightPrice(internalPid);
      CreditCards creditcards = scrapcreditCards(jsonPrices, internalPid);

      return PricingBuilder.create()
            .setPriceFrom(priceFrom)
            .setSpotlightPrice(spotlightPrice)
            .setCreditCards(creditcards)
            .build();
   }


   private Double scrapPriceFrom(String internalPid) {
      Double priceFrom = 0d;
      JSONObject jsonPrices = BrasilFastshopCrawlerUtils.fetchPrices(internalPid, true, session, logger, dataFetcher);
      JSONObject jsonPriceFrom = JSONUtils.getJSONValue(jsonPrices, "priceData");
      priceFrom = jsonPriceFrom.has("offerPriceValue") ? jsonPriceFrom.getDouble("offerPriceValue") : 0d;
      return priceFrom;
   }


   private Double spotlightPrice(String internalPid) {
      Double spotlightPrice = 0d;
      JSONObject jsonPrices = BrasilFastshopCrawlerUtils.fetchPrices(internalPid, true, session, logger, dataFetcher);
      JSONArray jsonspotlightPrice = jsonPrices.getJSONArray("promotionData");
      spotlightPrice = jsonspotlightPrice.getJSONObject(0).optDouble("price");
      return spotlightPrice;
   }

   private CreditCards scrapcreditCards(JSONObject jsonPrice, String internalPid) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = scrapInstallment(internalPid);

      for (String card : cards) {
         creditCards.add(CreditCardBuilder.create()
               .setBrand(card)
               .setInstallments(installments)
               .setIsShopCard(false)
               .build());
      }

      return creditCards;
   }

   private Installments scrapInstallment(String internalPid) throws MalformedPricingException {
      Installments installments = new Installments();
      JSONObject jsonPrices = BrasilFastshopCrawlerUtils.fetchPrices(internalPid, true, session, logger, dataFetcher);
      JSONObject priceData = JSONUtils.getJSONValue(jsonPrices, "priceData");

      if (priceData.has("installmentPrice")) {
         String text = priceData.getString("installmentPrice").toLowerCase();

         if (text.contains("x")) {
            int x = text.indexOf('x');

            Integer installment = Integer.parseInt(text.substring(0, x));
            Double value = MathUtils.parseDoubleWithComma(text.substring(x));

            installments.add(InstallmentBuilder.create()
                  .setInstallmentNumber(installment)
                  .setInstallmentPrice(value)
                  .build());
         }
      }

      return installments;
   }

}