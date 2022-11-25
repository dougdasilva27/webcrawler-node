package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
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
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilShopeeCrawler extends Crawler {
   public BrasilShopeeCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.JSON);
   }

   public List<Product> extractInformation(JSONObject productObj) throws Exception {

      List<Product> products = new ArrayList<>();
      if (productObj != null) {
         JSONObject data = productObj.optJSONObject("data");
         JSONArray models = data.optJSONArray("models");
         if (data != null && models != null) {
            String internalPid = data.optString("itemid");
            String description = data.getString("description");
            String name = data.optString("name");
            CategoryCollection categories = scrapCategories(data);
            RatingsReviews ratingsReviews = scrapRatingsAlternativeWay(data);
            for (Object obj : models) {
               JSONObject variation = (JSONObject) obj;
               String internalId = variation.optString("modelid");
               String variantName = mountName(variation, name);
               String primaryImage = getPrimaryImg(data, variation, JSONUtils.getValueRecursive(data, "tier_variations.0.images", JSONArray.class));
               Integer stock = scrapStock(variation);
               List<String> secondaryImages = scrapSecondaryImages(data, primaryImage);
               Offers offers = stock > 0 ? scrapOffers(variation) : new Offers();
               Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(variantName)
                  .setOffers(offers)
                  .setRatingReviews(ratingsReviews)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setDescription(description)
                  .setCategories(categories)
                  .setStock(stock)
                  .build();

               products.add(product);
            }


         } else {
            Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());

         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private String mountName(JSONObject variation, String name) {
      String namevariation = variation.getString("name");
      if (namevariation != null && !namevariation.isEmpty()) {
        return name + " " + namevariation;
      }
      return name;

   }

   private String getPrimaryImg(JSONObject data, JSONObject variation, JSONArray tier_variations) {
      if (tier_variations != null && !tier_variations.isEmpty()) {
         JSONArray index = JSONUtils.getValueRecursive(variation, "extinfo.tier_index", JSONArray.class);
         if (index != null && !index.isEmpty()) {
            String cod = tier_variations.getString((Integer) index.get(0));
            return mountUrl(cod);
         }
      } else {
         String cod = data.optString("image");
         return mountUrl(cod);
      }
      return null;
   }

   private String mountUrl(String cod) {
      if (cod != null && !cod.isEmpty()) {
         return "https://cf.shopee.com.br/file/" + cod;
      }
      return null;
   }

   private Integer scrapStock(JSONObject data) {
      int stock = data.optInt("stock");
      if (stock == 0) {
         stock = data.optInt("other_stock");
      }
      return stock;
   }

   private RatingsReviews scrapRatingsAlternativeWay(JSONObject data) {
      RatingsReviews ratingReviews = new RatingsReviews();
      JSONObject itemRating = data.optJSONObject("item_rating");
      ratingReviews.setDate(session.getDate());
      JSONArray ratingCount = itemRating.optJSONArray("rating_count");
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(ratingCount);
      //por padrão a posição 0 é o total de avaliações
      ratingReviews.setTotalRating(ratingCount.getInt(0));
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);
      ratingReviews.setAverageOverallRating(itemRating.getDouble("rating_star"));


      return ratingReviews;
   }


   private AdvancedRatingReview scrapAdvancedRatingReview(JSONArray data) {
      Integer star1 = (Integer) data.get(1);
      Integer star2 = (Integer) data.get(2);
      Integer star3 = (Integer) data.get(3);
      Integer star4 = (Integer) data.get(4);
      Integer star5 = (Integer) data.get(5);

      return new AdvancedRatingReview.Builder()
         .totalStar1(star1)
         .totalStar2(star2)
         .totalStar3(star3)
         .totalStar4(star4)
         .totalStar5(star5)
         .build();
   }


   private CategoryCollection scrapCategories(JSONObject data) {
      JSONArray arr = data.optJSONArray("categories");
      CategoryCollection categories = new CategoryCollection();
      int cont = 0;
      for (Object obj : arr) {
         JSONObject category = (JSONObject) obj;
         categories.add(category.optString("display_name"));
         cont++;
         if (cont == 3) {
            return categories;
         }
      }

      return categories;
   }

   private List<String> scrapSecondaryImages(JSONObject data, String primaryImage) {
      List<String> list = new ArrayList<>();
      JSONArray arr = data.optJSONArray("images");
      for (Integer i = 0; i < arr.length(); i++) {
         String image = "https://cf.shopee.com.br/file/" + arr.get(i);
         if (!image.equals(primaryImage)) {
            list.add(image);
         }
      }

      return list;
   }

   @Override
   protected Response fetchResponse() {
      JSONObject data = null;
      String regex = "-i(.*)\\?sp";
      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      final Matcher matcher = pattern.matcher(this.session.getOriginalURL());
      String ids = "";
      if (matcher.find()) {
         ids = matcher.group(1);
      } else {
         ids = this.session.getOriginalURL();
      }
      String[] arr = ids.split("\\.");
      String itemId = arr[arr.length - 1];
      String shopId = arr[arr.length - 2];
      String newUrl = "https://shopee.com.br/api/v4/item/get?itemid=" + itemId + "&shopid=" + shopId;
      Request request = Request.RequestBuilder.create()
         .setUrl(newUrl)
         .build();

      Response response = this.dataFetcher.get(session, request);
      return response;

   }

   private Offers scrapOffers(JSONObject data) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();

      Pricing pricing = scrapPricing(data);
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSales(sales)
         .setSellerFullName(session.getOptions().optString("sellerName"))
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject data) throws MalformedPricingException {
      Integer spotlightPriceInt = data.optInt("price");
      Integer priceFromInt = data.getInt("price_before_discount");
      Double spotlightPrice = spotlightPriceInt / 100000.0;
      Double priceFrom;
      if (priceFromInt != null && priceFromInt != -1 && priceFromInt != 0) {
         priceFrom = priceFromInt / 100000.0;
      } else {
         priceFrom = null;
      }

      if (spotlightPrice == null) {
         spotlightPrice = priceFrom;
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

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }
}
