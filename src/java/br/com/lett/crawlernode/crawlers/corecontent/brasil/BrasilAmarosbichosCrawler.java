package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

public class BrasilAmarosbichosCrawler extends Crawler {

   private static final String HOME_PAGE = "www.petshopamarosbichos.com.br";
   private static final String MAIN_SELLER_NAME = "Amaro's Bichos";
   private Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), 
         Card.ELO.toString(), Card.DINERS.toString(), Card.AMEX.toString());
   
   public BrasilAmarosbichosCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".cod-sku [itemprop=\"sku\"]", true);
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-container[data-product-id]", "data-product-id");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".container .title-page-product", true);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrump-container > ul > li span[itemprop=\"title\"]", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-sku-image .image-highlight a.main-product img",
               Arrays.asList("data-zoom-image", "src"), "https:", HOME_PAGE);
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".product-sku-image .image-highlight a.main-product img",
               Arrays.asList("data-zoom-image", "src"), "https:", HOME_PAGE, primaryImage);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".descriptions"));
         Offers offers = scrapOffers(doc);
         RatingsReviews ratingsReviews = scrapRatingReviews(doc);

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
               .setDescription(description)
               .setOffers(offers)
               .setRatingReviews(ratingsReviews)
               .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }
   
   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);

      if(pricing != null) {
        offers.add(OfferBuilder.create()
              .setUseSlugNameAsInternalSellerId(true)
              .setSellerFullName(MAIN_SELLER_NAME)
              .setSellersPagePosition(1)
              .setIsBuybox(false)
              .setIsMainRetailer(true)
              .setPricing(pricing)
              .build());
      }

      return offers;
   }
   
   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product .product-sku-information meta[itemprop=\"price\"]", "content", false, ',', session);
      
      if(spotlightPrice != null) {
        Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product .promotion", null, true, ',', session);
        CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);
  
        return PricingBuilder.create()
              .setSpotlightPrice(spotlightPrice)
              .setPriceFrom(priceFrom)
              .setCreditCards(creditCards)
              .setBankSlip(BankSlipBuilder.create().setFinalPrice(spotlightPrice).setOnPageDiscount(0d).build())
              .build();
      }
      
      return null;
   }
   
   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      installments.add(InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());

      Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(".parcel-price .cash-payment span", doc, true, "x de");
      if (!pair.isAnyValueNull()) {
         installments.add(InstallmentBuilder.create()
               .setInstallmentNumber(pair.getFirst())
               .setInstallmentPrice(MathUtils.normalizeTwoDecimalPlaces(pair.getSecond().doubleValue()))
               .build());
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

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("#frontend-product .product-container") != null;
   }

   private RatingsReviews scrapRatingReviews(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtmlAttr(doc, ".comments-wrapper [itemprop=\"ratingCount\"]", "content", 0);
      Double avgRating = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".comments-wrapper [itemprop=\"ratingValue\"]", "content", false, '.', session);
      Integer totalWrittenReviews = CrawlerUtils.scrapIntegerFromHtmlAttr(doc, ".comments-wrapper [itemprop=\"reviewCount\"]", "content", 0);
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(doc);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalWrittenReviews);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Elements reviews = doc.select(".product-comment-list > .customer-comments .stars > .rating-stars");

      for (Element review : reviews) {
         if (review.hasAttr("data-score")) {
            Integer val = Integer.parseInt(review.attr("data-score").replaceAll("[^0-9]+", ""));

            switch (val) {
               case 1:
                  star1 += 1;
                  break;
               case 2:
                  star2 += 1;
                  break;
               case 3:
                  star3 += 1;
                  break;
               case 4:
                  star4 += 1;
                  break;
               case 5:
                  star5 += 1;
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
