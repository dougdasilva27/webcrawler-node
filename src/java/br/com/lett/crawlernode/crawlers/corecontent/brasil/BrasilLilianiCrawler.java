package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class BrasilLilianiCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.liliani.com.br/";
   private static final String SELLER_FULL_NAME = "Liliani";
   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString());


   public BrasilLilianiCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      JSONObject jsonObject = CrawlerUtils.selectJsonFromHtml(doc, "#content-wrapper script", "var product = ", "};", false, false);

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = jsonObject.optString("IntegrationID");
         String name = jsonObject.optString("Name");

         String description = JSONUtils.getValueRecursive(jsonObject, "Descriptions.0.Title", String.class);
         String primaryImage = CrawlerUtils.completeUrl(jsonObject.optString("MediaSmall"), "https", "dleyjack4mlu0.cloudfront.net/");
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, "#scroll-media li img", Collections.singletonList("src"), "https", "dleyjack4mlu0.cloudfront.net/", primaryImage);
         String availabilityText = jsonObject.optString("AvailabilityText");
         boolean available = availabilityText.equals("Em estoque");
         Offers offers = available ? scrapOffers(jsonObject) : new Offers();
         RatingsReviews ratingsReviews = ratingsReviews(doc);

         JSONArray variations = jsonObject.optJSONArray("Options");

         if (!variations.isEmpty()) {

            for (Object o : variations) {
               if (o instanceof JSONObject) {
                  JSONObject dataVariations = (JSONObject) o;
                  if (dataVariations.has("PropertyName") && dataVariations.optString("PropertyName").equals("Voltagem")) {

                     JSONArray valuesVariations = dataVariations.optJSONArray("Values");
                     for (Object obj : valuesVariations) {
                        if (obj instanceof JSONObject) {
                           JSONObject values = (JSONObject) o;
                           int internalIdToConvert = values.optInt("OptionID");
                           String internalId = Integer.toString(internalIdToConvert);

                           // Creating the product
                           Product product = ProductBuilder.create()
                              .setUrl(session.getOriginalURL())
                              .setInternalId(internalId)
                              .setInternalPid(internalPid)
                              .setName(name)
                              .setPrimaryImage(primaryImage)
                              .setSecondaryImages(secondaryImages)
                              .setDescription(description)
                              .setRatingReviews(ratingsReviews)
                              .setOffers(offers)
                              .build();

                           products.add(product);
                        }
                     }
                  }
               }
            }
         } else {

            String internalId = jsonObject.optString("ProductID");

            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setRatingReviews(ratingsReviews)
               .setOffers(offers)
               .build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }


   private boolean isProductPage(Document doc) {
      return doc.select(".product-detail").first() != null;
   }


   private Offers scrapOffers(JSONObject jsonObject) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(jsonObject);
      List<String> sales = scrapSales(pricing);


      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setSales(sales)
         .setPricing(pricing)
         .build());

      return offers;

   }

   private Pricing scrapPricing(JSONObject jsonObject) throws MalformedPricingException {
      Double spotlightPrice = jsonObject.optDouble("RetailPriceMin");
      Double priceFrom = jsonObject.optDouble("RetailPriceMax");
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(!priceFrom.equals(spotlightPrice) ? priceFrom : null)
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


      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();

      String saleDiscount = CrawlerUtils.calculateSales(pricing);

      if (saleDiscount != null) {
         sales.add(saleDiscount);
      }

      return sales;
   }

   private RatingsReviews ratingsReviews(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();

      Elements totalNumOfEvaluations = doc.select(".wd-review-item .rating-attributes li");
      Double avgRating = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".rating-attributes .rating", null, true, '.', session);

      ratingReviews.setTotalRating(totalNumOfEvaluations.size());
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations.size());

      return ratingReviews;
   }
}
