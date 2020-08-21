package br.com.lett.crawlernode.crawlers.corecontent.romania;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class RomaniaEmagCrawler extends Crawler {


   private final String HOME_PAGE = "https://www.emag.ro/";
   private static final String SELLER_FULL_NAME = "Emag";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public RomaniaEmagCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (doc.selectFirst(".container .page-title") != null) {

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-highlight button", "data-offer-id");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".page-header.has-subtitle-info h1", false);
         String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".mrg-sep-sm", ".container.pad-btm-lg"));
         boolean available = doc.selectFirst(".label.label-in_stock") != null;
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".thumbnail-wrapper .product-gallery-image", Arrays.asList("href"), "https:", "www.emag.ro/");
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".thumbnail-wrapper .product-gallery-image", Arrays.asList("href"), "https:", "www.emag.ro/", primaryImage);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li a");
         RatingsReviews ratingReviews = crawlRating(doc);
         Offers offers = available ? scrapOffer(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setRatingReviews(ratingReviews)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }


   private Offers scrapOffer(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(doc);

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

   private List<String> scrapSales(Document doc) {
      List<String> sales = new ArrayList<>();

      Element salesOneElement = doc.selectFirst(".page-skin-inner .product-this-deal");
      String firstSales = salesOneElement != null ? salesOneElement.text() : null;

      if (firstSales != null && !firstSales.isEmpty()) {
         sales.add(firstSales);
      }

      return sales;
   }

   private Double concatPrice(Document doc, String cssSelector1, String cssSelector2) {
      Double price = 0D;

      String firstDecimalPlace = doc.selectFirst(cssSelector1).ownText();
      String secondDecimalPlace = doc.selectFirst(cssSelector2).text();

      if (firstDecimalPlace != null && secondDecimalPlace != null) {
         String priceConcat = firstDecimalPlace + "," + secondDecimalPlace;
         price = MathUtils.parseDoubleWithComma(priceConcat);
      }

      return price;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = 0D;

      if (doc.selectFirst(".w-50.mrg-rgt-xs .product-old-price s") != null) {
         priceFrom = concatPrice(doc, ".w-50.mrg-rgt-xs .product-old-price s", "sup");
      } else {
         priceFrom = null;
      }

      Double spotlightPrice = concatPrice(doc, ".w-50.mrg-rgt-xs .product-new-price", "sup");
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

   private RatingsReviews crawlRating(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtml(doc, ".mrg-btm-xxs .star-rating.star-rating-read", false, 0);
      Double avgRating = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".review-rating-data", null, false, '.', session);
      AdvancedRatingReview adRating = scrapAdvancedRatingReview(doc);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAdvancedRatingReview(adRating);

      return ratingReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
      Integer stars = 0;
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Elements reviews = doc.select(".reviews-summary-container .reviews-summary-bars .js-rating-bar:last-child");

      for (Element review : reviews) {

         String stringStarNumber = review.attr("data-value");
         Integer numberOfStars = !stringStarNumber.isEmpty() ? Integer.parseInt(stringStarNumber) : 0;

         String elementVoteNumber = review.text();

         if (elementVoteNumber != null) {

            String vN = elementVoteNumber.replaceAll("[^0-9]", "").trim();
            Integer numberOfVotes = !vN.isEmpty() ? Integer.parseInt(vN) : 0;

            switch (numberOfStars) {
               case 5:
                  star5 = numberOfVotes;
                  break;
               case 4:
                  star4 = numberOfVotes;
                  break;
               case 3:
                  star3 = numberOfVotes;
                  break;
               case 2:
                  star2 = numberOfVotes;
                  break;
               case 1:
                  star1 = numberOfVotes;
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
