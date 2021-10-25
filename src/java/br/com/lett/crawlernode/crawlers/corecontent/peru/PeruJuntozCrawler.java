package br.com.lett.crawlernode.crawlers.corecontent.peru;

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

import java.util.*;

public class PeruJuntozCrawler extends Crawler {

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public PeruJuntozCrawler(Session session) {
      super(session);
      this.config.setParser(Parser.JSON);
   }

   @Override
   protected Response fetchResponse() {
      String productSlug = session.getOriginalURL().substring(28).replaceAll("\\?ref=.*", "");
      String url = "https://juntoz.com/proxy/products/" + productSlug + "/variations";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .build();

      return this.dataFetcher.get(session, request);
   }

   protected JSONObject fetchRatings(String internalPid) {
      String url = "https://sdk-widgets.juntoz.com/summaries/0/" + internalPid + "/product?take=5";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .build();

      Response response = this.dataFetcher.get(session, request);
      return JSONUtils.stringToJson(response.getBody());
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();

      JSONArray arrayProductVariations = json.optJSONArray("variations");

      if (arrayProductVariations != null && !arrayProductVariations.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = "";

         for (Object o : arrayProductVariations) {
            JSONObject jsonProduct = (JSONObject) o;

            if (internalPid.equals("")) {
               internalPid = jsonProduct.optString("ProductId");
            }

            String internalId = jsonProduct.optString("SKU");
            String name = jsonProduct.optString("Name");
            CategoryCollection categories = scrapCategories(jsonProduct);
            List<String> images = scrapImages(jsonProduct);
            String primaryImage = !images.isEmpty() ? images.remove(0) : null;
            String description = jsonProduct.optString("FullDescription") + "\n" + jsonProduct.optString("BrandDescription");
            RatingsReviews ratings = scrapRatings(internalPid);
            Offers offers = scrapOffers(jsonProduct);

            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setCategories(categories)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(images)
               .setDescription(description)
               .setRatingReviews(ratings)
               .setOffers(offers)
               .build();

            products.add(product);
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   protected CategoryCollection scrapCategories(JSONObject json) {
      CategoryCollection categories = new CategoryCollection();

      JSONArray arrayCategories = json.optJSONArray("Categories");

      for (int i = 0; i < arrayCategories.length(); i++) {
         JSONObject category = arrayCategories.optJSONObject(i);

         if (!category.optString("ParentId").equals("")) {
            categories.add(category.optString("Name"));
            continue;
         }

         if (i == 0) {
            categories.add(category.optString("Name"));
         }
      }

      return categories;
   }

   protected List<String> scrapImages(JSONObject json) {
      List<String> images = new ArrayList<>();
      JSONArray arrayImages = json.optJSONArray("Files");

      for (Object o : arrayImages) {
         JSONObject img = (JSONObject) o;
         images.add(img.optString("Name"));
      }

      return images;
   }

   private RatingsReviews scrapRatings(String internalPid) {
      RatingsReviews ratingReviews = new RatingsReviews();
      JSONObject json = fetchRatings(internalPid);

      ratingReviews.setDate(session.getDate());
      ratingReviews.setTotalRating(json.optInt("total"));
      ratingReviews.setAverageOverallRating(json.optDouble("score"));
      ratingReviews.setAdvancedRatingReview(scrapAdvancedRating(json));
      ratingReviews.setTotalWrittenReviews(json.optInt("total"));

      return ratingReviews;
   }

   private AdvancedRatingReview scrapAdvancedRating(JSONObject json) {
      AdvancedRatingReview advancedRating = new AdvancedRatingReview();

      advancedRating.setTotalStar1(json.optInt("stars1"));
      advancedRating.setTotalStar2(json.optInt("stars2"));
      advancedRating.setTotalStar3(json.optInt("stars3"));
      advancedRating.setTotalStar4(json.optInt("stars4"));
      advancedRating.setTotalStar5(json.optInt("stars5"));

      return advancedRating;
   }

   private Offers scrapOffers(JSONObject jsonObject) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();

      Pricing pricing = scrapPricing(jsonObject);
      sales.add(CrawlerUtils.calculateSales(pricing));

      boolean isMainSeller = jsonObject.optString("StoreName").toLowerCase(Locale.ROOT).contains("juntoz");
      String sellerName = jsonObject.optString("StoreName");

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(sellerName)
         .setMainPagePosition(1)
         .setIsBuybox(true)
         .setIsMainRetailer(isMainSeller)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(json, "SpecialPrice", false);
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(json, "BasePrice", false);

      if (priceFrom.equals(spotlightPrice)) {
         priceFrom = null;
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      if (installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

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
