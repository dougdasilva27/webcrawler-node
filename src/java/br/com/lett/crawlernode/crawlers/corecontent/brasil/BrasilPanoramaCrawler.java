package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilPanoramaCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Panorama";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString());

   public BrasilPanoramaCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document document) throws Exception {

      List<Product> products = new ArrayList<>();

      if (isProductPage(document)) {

         String name = CrawlerUtils.scrapStringSimpleInfo(document, ".product-summary h1", true);
         String internalId = CrawlerUtils.scrapStringSimpleInfo(document, ".sku-and-brand .sku", true);
         String internalPid = internalId;
         CategoryCollection categories = CrawlerUtils.crawlCategories(document, ".breadcrumb li:not(:last-child) a span", false);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(document, "#main-image", Arrays.asList("data-src"), "https", "www.panoramamoveis.com.br");
         List<String> secondaryImage = CrawlerUtils.scrapSecondaryImages(document, "li:not(:first-child) .capa.ar-content img", Arrays.asList("data-src"), "https", "www.panoramamoveis.com.br", primaryImage);
         String description = CrawlerUtils.scrapSimpleDescription(document, Arrays.asList(".content.technical-description.product-description"));
         boolean available = !document.select(".icon.icon-cart-light").isEmpty();
         Offers offers = available ? scrapOffers(document) : new Offers();
         RatingsReviews ratingsReviews = crawlRating(document);

         products.add(ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setName(name)
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImage)
            .setDescription(description)
            .setRatingReviews(ratingsReviews)
            .setOffers(offers)
            .build());

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }


   private boolean isProductPage(Document document) {
      return !document.select(".product-summary").isEmpty();
   }

   private Offers scrapOffers(Document document) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(document);
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

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();

      String salesOnPrice = CrawlerUtils.calculateSales(pricing);
      if (salesOnPrice != null) {
         sales.add(salesOnPrice);
      }
      return sales;
   }


   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-from span", null, true, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-option:not(:first-child) .price-value", null, true, ',', session);
      Double bankSlipValue = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-with-tag .price-value", null, true, ',', session);

      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(bankSlipValue)
         .build();

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .setPriceFrom(priceFrom)
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

   private RatingsReviews crawlRating(Document document) {

      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
      RatingsReviews ratingReviews = new RatingsReviews();

      ratingReviews.setDate(session.getDate());
      Integer totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtml(document,
         ".rating-count", true, 0);
      if (totalNumOfEvaluations < 0) {
         Double avgRating = getAvarage(document);

         ratingReviews.setTotalRating(totalNumOfEvaluations);
         ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
         ratingReviews.setAverageOverallRating(avgRating);
         ratingReviews.setAdvancedRatingReview(scrapAdvancedRatingReviews(document, totalNumOfEvaluations));
      }


      return ratingReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReviews(Document doc, Integer totalNumOfEvaluations) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      if (totalNumOfEvaluations > 0) {
         for (Element starElement : doc.select(".sum-stars")) {
            Integer star = CrawlerUtils.scrapIntegerFromHtmlAttr(starElement, ".sum-stars .rating .c-rating", "data-rating-value", 0);
            Integer total = getCountStars(starElement);
            if (star == 1) {
               star1 = total;
            } else if (star == 2) {
               star2 = total;
            } else if (star == 3) {
               star3 = total;
            } else if (star == 4) {
               star4 = total;
            } else if (star == 5) {
               star5 = total;
            }
         }
      }


      return new AdvancedRatingReview.Builder().totalStar1(star1).totalStar2(star2).totalStar3(star3).totalStar4(star4).totalStar5(star5).build();
   }

   private Double getAvarage(Document document) {
      String avgRatingStr = CrawlerUtils.scrapStringSimpleInfo(document, ".resumo label", true);
      String value = null;
      if (avgRatingStr != null) {
         value = CrawlerUtils.getStringBetween(avgRatingStr, "Nota ", " de");
      }

      return value != null ? Double.parseDouble(value) : 0;

   }

   private Integer getCountStars(Element element) {
      String count = null;
      Pattern pattern = Pattern.compile("\\<span>(.[0-9]*)\\<\\/span>");
      Matcher matcher = pattern.matcher(element.toString());
      if (matcher.find()) {
         count = matcher.group(1);
      }
      return count != null ? Integer.parseInt(count) : null;
   }


}
