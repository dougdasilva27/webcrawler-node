package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilFastshopCrawlerUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.prices.Prices;
import models.pricing.BankSlip;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;
import java.util.Map.Entry;

public class BrasilFastshopCrawler extends Crawler {

   private static final String SELLER_NAME = "Fastshop";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
           Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


   public BrasilFastshopCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();
      String internalPid = BrasilFastshopCrawlerUtils.crawlPartnerId(session);
      JSONObject productAPIJSON = BrasilFastshopCrawlerUtils.crawlApiJSON(internalPid, session, cookies, dataFetcher);

      if (productAPIJSON.length() > 0) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         CategoryCollection categories = crawlCategories(internalPid);
         StringBuilder description = crawlDescription(internalPid);

         // sku data in json
         JSONArray arraySkus = productAPIJSON.has("skus") ? productAPIJSON.optJSONArray("skus") : new JSONArray();
         //productAPIJSON != null  ja feito na linha 61;
         for (int i = 0; i < arraySkus.length(); i++) {
            JSONObject variationJson = arraySkus.optJSONObject(i);

            String internalId = crawlInternalId(variationJson);
            JSONObject skuAPIJSON = productAPIJSON;

            if (arraySkus.length() > 1 && productAPIJSON.has("buyable") && productAPIJSON.optBoolean("buyable")) { // In case the array only has 1 sku.
               skuAPIJSON = BrasilFastshopCrawlerUtils.crawlApiJSON(variationJson.has("partNumber") ? variationJson.optString("partNumber") : null,
                       session, cookies, dataFetcher);

               if (skuAPIJSON.length() < 1) {
                  skuAPIJSON = productAPIJSON;
               }
            }

            String primaryImage = crawlPrimaryImage(skuAPIJSON);
            String name = crawlName(skuAPIJSON, variationJson) + " - " + internalPid;
            List<String> secondaryImages = crawlSecondaryImages(skuAPIJSON);
            description.append(skuAPIJSON.has("longDescription") ? skuAPIJSON.get("longDescription") : "");
            boolean pageAvailability = crawlAvailability(skuAPIJSON);
            JSONObject jsonPrices =
                    pageAvailability ? BrasilFastshopCrawlerUtils.fetchPrices(internalPid, true, session, logger, dataFetcher) : new JSONObject();
            Offers offers = pageAvailability ? scrapOffers(skuAPIJSON, jsonPrices, variationJson) : new Offers();
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
         internalId = json.optString("catEntry");
      }

      return internalId;
   }

   private CategoryCollection crawlCategories(String partnerId) {
      CategoryCollection category = new CategoryCollection();
      String apiUrl = "https://www.fastshop.com.br/wcs/resources/v1/categories/breadcrumb/" + partnerId;
      Request request = RequestBuilder.create().setUrl(apiUrl).setCookies(cookies).build();
      JSONObject apiJson = CrawlerUtils.stringToJson(dataFetcher.get(session, request).getBody());
      JSONArray jsonArray = JSONUtils.getJSONArrayValue(apiJson, "result");
      if (jsonArray.length() > 0) {
         jsonArray = jsonArray.optJSONArray(jsonArray.length() - 1);
         for (int i = 0; i < jsonArray.length(); i++) {
            category.add(jsonArray.optJSONObject(i).optString("name", ""));
         }
      }
      return category;
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

   private Offers scrapOffers(JSONObject apiSku, JSONObject jsonPrices, JSONObject jsonSku) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(jsonPrices, apiSku);

      if (apiSku.has("marketPlace") && apiSku.optBoolean("marketPlace") && apiSku.has("marketPlaceText") && apiSku.has("priceOffer")) {
         String sellerName = apiSku.optString("marketPlaceText");

         offers.add(OfferBuilder.create()
                 .setUseSlugNameAsInternalSellerId(true)
                 .setSellerFullName(sellerName)
                 .setMainPagePosition(1)
                 .setIsBuybox(false)
                 .setIsMainRetailer(sellerName.equalsIgnoreCase(SELLER_NAME))
                 .setPricing(pricing)
                 .build());

      } else if (apiSku.has("priceOffer")) {
         offers.add(OfferBuilder.create()
                 .setUseSlugNameAsInternalSellerId(true)
                 .setSellerFullName(SELLER_NAME)
                 .setMainPagePosition(1)
                 .setIsBuybox(false)
                 .setIsMainRetailer(true)
                 .setPricing(pricing)
                 .build());
      }

      return offers;
   }


   private Pricing scrapPricing(JSONObject jsonPrices, JSONObject apiSku) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(apiSku, "priceOffer", true);
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(apiSku, "priceTag", true);

      if (spotlightPrice == null) {
         spotlightPrice = MathUtils.parseDoubleWithDot(JSONUtils.getValueRecursive(jsonPrices, "priceData.offerPriceValue", String.class));
      }

      CreditCards creditCards = scrapCreditCards(jsonPrices, spotlightPrice);
      BankSlip bankSlip = scrapBankslip(jsonPrices, spotlightPrice);


      return PricingBuilder.create()
              .setPriceFrom(priceFrom)
              .setSpotlightPrice(spotlightPrice)
              .setCreditCards(creditCards)
              .setBankSlip(bankSlip)
              .build();
   }

   private CreditCards scrapCreditCards(JSONObject jsonPrices, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      Map<String, Double> promotionPrice = scrapDiscountPrice(jsonPrices, "CreditCard");
      for (Entry<String, Double> entry : promotionPrice.entrySet()) {
         String text = entry.getKey().replaceAll("[^0-9]", "");
         if (!text.isEmpty() && entry.getValue() > 0d) {
            Integer installment = Integer.parseInt(text);

            installments.add(InstallmentBuilder.create()
                    .setInstallmentNumber(installment)
                    .setInstallmentPrice(entry.getValue())
                    .build());
         }
      }

      if (installments.getInstallment(1) == null) {
         installments.add(InstallmentBuilder.create()
                 .setInstallmentNumber(1)
                 .setInstallmentPrice(spotlightPrice)
                 .build());
      }

      JSONObject pricingJson = jsonPrices.has("priceData") && !jsonPrices.isNull("priceData") ? jsonPrices.getJSONObject("priceData") : new JSONObject();
      if (pricingJson.has("installmentPrice")) {
         String text = pricingJson.getString("installmentPrice").toLowerCase();

         Double interest = 0d;
         String interestText = pricingJson.optString("interestPrice", "");
         if (interestText.contains("de") && interestText.contains("%")) {
            int x = interestText.indexOf("de");
            int y = interestText.indexOf("%", x);

            Double percentage = MathUtils.parseDoubleWithComma(interestText.substring(x, y));
            interest = percentage == null ? 0d : percentage;
         }

         if (text.contains("x")) {
            int x = text.indexOf('x');
            String installmentText = text.substring(0, x).replaceAll("[^0-9]", "");
            Double value = MathUtils.parseDoubleWithComma(text.substring(x));

            if (!installmentText.isEmpty() && value != null) {
               installments.add(InstallmentBuilder.create()
                       .setInstallmentNumber(Integer.parseInt(installmentText))
                       .setInstallmentPrice(value)
                       .setAmOnPageInterests(interest)
                       .build());
            }
         }
      }

      for (String brand : cards) {
         creditCards.add(CreditCardBuilder.create()
                 .setBrand(brand)
                 .setIsShopCard(false)
                 .setInstallments(installments)
                 .build());
      }

      return creditCards;
   }

   private BankSlip scrapBankslip(JSONObject jsonPrices, Double spotlightPrice) throws MalformedPricingException {
      Double bankPrice = spotlightPrice;

      Map<String, Double> promotionPrice = scrapDiscountPrice(jsonPrices, "Boleto");
      for (Entry<String, Double> entry : promotionPrice.entrySet()) {
         if (entry.getValue() > 0d) {
            bankPrice = entry.getValue();
            break;
         }
      }

      return BankSlipBuilder.create()
              .setFinalPrice(bankPrice)
              .build();
   }

   private Map<String, Double> scrapDiscountPrice(JSONObject jsonPrices, String method) {
      Map<String, Double> map = new HashMap<>();

      JSONArray promotionData = jsonPrices.optJSONArray("promotionData");
      if (promotionData != null && promotionData.length() > 0) {
         for (Object o : promotionData) {
            JSONObject promo = o instanceof JSONObject ? (JSONObject) o : new JSONObject();

            String paymentMethod = promo.optString("promotionPaymentMethodID", null);
            if (paymentMethod != null && paymentMethod.contains(method)) {
               map.put(paymentMethod, promo.optDouble("price", 0d));
            }
         }
      }

      return map;
   }

   private boolean crawlAvailability(JSONObject json) {
      if (json.has("buyable")) {
         return json.optBoolean("buyable");
      }
      return false;
   }

   private String crawlPrimaryImage(JSONObject json) {
      String primaryImage = null;

      if (json.has("images")) {
         JSONArray images = json.optJSONArray("images");

         if (images.length() > 0) {
            JSONObject imageJson = images.optJSONObject(0);

            if (imageJson.has("path")) {
               primaryImage = imageJson.optString("path");

               if (!primaryImage.startsWith("http")) {
                  primaryImage = "https://prdresources1-a.akamaihd.net/wcsstore/" + primaryImage;
               }
            }
         }
      }

      return primaryImage;
   }

   private List<String> crawlSecondaryImages(JSONObject json) {
      List<String> secondaryImagesArray = new ArrayList<String>();

      if (json.has("images")) {
         JSONArray images = json.optJSONArray("images");

         for (int i = 1; i < images.length(); i++) {
            JSONObject imageJson = images.optJSONObject(i);

            if (imageJson.has("path")) {
               String image = imageJson.optString("path");

               if (!image.startsWith("http")) {
                  image = "https://prdresources1-a.akamaihd.net/wcsstore/" + image;
               }

               secondaryImagesArray.add(image);
            }
         }
      }

      return secondaryImagesArray;
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
         JSONArray ratingDistribution = JsonRating.optJSONArray("RatingDistribution");

         for (int i = 0; i < ratingDistribution.length(); i++) {
            JSONObject rV = ratingDistribution.optJSONObject(i);


            int val1 = rV.optInt("RatingValue");
            int val2 = rV.optInt("Count");

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
         avgOverallRating = reviewStatistics.optDouble("AverageOverallRating");
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
         JSONObject includes = ratingReviewsEndpointResponse.optJSONObject("Includes");

         if (includes.has("Products")) {
            JSONObject products = includes.optJSONObject("Products");

            if (products.has(skuInternalPid)) {
               JSONObject product = products.optJSONObject(skuInternalPid);

               if (product.has("ReviewStatistics")) {
                  return product.optJSONObject("ReviewStatistics");
               }
            }
         }
      }

      return new JSONObject();
   }
}
