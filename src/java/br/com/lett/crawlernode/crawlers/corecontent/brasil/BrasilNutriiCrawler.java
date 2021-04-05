package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip;
import models.pricing.CreditCard;
import models.pricing.CreditCards;
import models.pricing.Installment;
import models.pricing.Installments;
import models.pricing.Pricing;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

/**
 * Date: 11/08/2017
 *
 * @author Gabriel Dornelas
 */
public class BrasilNutriiCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Nutrii Brasil";
   private final String HOME_PAGE = "http://www.nutrii.com.br/";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilNutriiCrawler(Session session) {
      super(session);
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      if (doc.selectFirst("meta[content=product]") != null) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject jsonInfo = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"application/ld+json\"]", null, null, false, false);
         if (jsonInfo != null && !jsonInfo.isEmpty()) {
            JSONObject jsonOffers = JSONUtils.getJSONValue(jsonInfo, "offers");


            String internalId = jsonInfo.optString("sku");
            String name = jsonInfo.optString("name");
            CategoryCollection categories = crawlCategories(name);
            String primaryImage = jsonInfo.optString("image");
            String description = jsonInfo.optString("description");

            String availability = jsonOffers.optString("availability");
            boolean available = availability.equals("http://schema.org/InStock");
            Offers offers = available ? scrapOffer(jsonOffers) : new Offers();
            RatingsReviews ratingReviews = scrapRatingReviews(jsonInfo);

            // Creating the product
            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalId)
               .setName(name)
               .setCategories(categories)
               .setPrimaryImage(primaryImage)
               .setDescription(description)
               .setOffers(offers)
               .setRatingReviews(ratingReviews)
               .build();

            products.add(product);
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private CategoryCollection crawlCategories(String name) {
      CategoryCollection categoryCollection = new CategoryCollection();

      categoryCollection.add("home");
      categoryCollection.add(name);

      return categoryCollection;
   }


   private Offers scrapOffer(JSONObject jsonOffers) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(jsonOffers);
      List<String> sales = new ArrayList<>();

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private Pricing scrapPricing(JSONObject jsonOffers) throws MalformedPricingException {
      Double spotlightPrice = jsonOffers.optDouble("price");
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
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


   private RatingsReviews scrapRatingReviews(JSONObject jsonInfo) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      JSONObject aggregateRating = JSONUtils.getJSONValue(jsonInfo, "aggregateRating");

      if (aggregateRating != null && !aggregateRating.isEmpty()) {

         Integer totalNumOfEvaluations = aggregateRating.optInt("reviewCount");
         Double avgRating = aggregateRating.optDouble("ratingValue");
         AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(jsonInfo);

         ratingReviews.setTotalRating(totalNumOfEvaluations);
         ratingReviews.setAverageOverallRating(avgRating);
         ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
         ratingReviews.setAdvancedRatingReview(advancedRatingReview);
      }

      return ratingReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(JSONObject jsonInfo) {
      int star1 = 0;
      int star2 = 0;
      int star3 = 0;
      int star4 = 0;
      int star5 = 0;

      JSONArray reviews = JSONUtils.getJSONArrayValue(jsonInfo, "review");

      if (reviews != null && !reviews.isEmpty()) {

         for (Object review : reviews) {

            JSONObject reviewbyclient = (JSONObject) review;
            JSONObject reviewRating = JSONUtils.getJSONValue(reviewbyclient, "reviewRating");

            switch (reviewRating.optInt("ratingValue")) {
               case 5:
                  star5 += 1;
                  break;
               case 4:
                  star4 += 1;
                  break;
               case 3:
                  star3 += 1;
                  break;
               case 2:
                  star2 += 1;
                  break;
               case 1:
                  star1 += 1;
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
}
