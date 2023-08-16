package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;

import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class BrasilPlataformaLorealCrawler extends Crawler {
   private String sellerName = this.session.getOptions().optString("sellerName");

   public Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.ELO.toString(), Card.AMEX.toString());

   public BrasilPlataformaLorealCrawler(Session session) {
      super(session);
   }

   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String internalPid = CommonMethods.getLast(this.session.getOriginalURL().split("/")).replaceAll(".html", "");
         String productName = CrawlerUtils.scrapStringSimpleInfo(doc, ".c-product-main__name", false);
         String description = crawlDescription(doc);
         List<String> categories = CrawlerUtils.crawlCategories(doc, ".l-pdp .c-breadcrumbs .c-breadcrumbs__list .c-breadcrumbs__item .c-breadcrumbs__text", true);
         List<String> secondaryImages = getSecondaryImages(doc);
         RatingsReviews ratingsReviews = ratingsReviews(doc);

         Elements variations = doc.select(".c-variation-section .c-carousel__inner ul li");

         if (variations.isEmpty()) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".c-product-main", "data-js-pid");
            boolean isAvailable = doc.selectFirst(".c-product-main__quantity .c-stepper-input.m-disabled") != null;
            Offers offers = isAvailable ? scrapOffers(doc) : new Offers();

            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(productName)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setCategories(categories)
               .setRatingReviews(ratingsReviews)
               .setOffers(offers)
               .build();
            products.add(product);
         } else {
            for (Element element : variations) {
               String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".c-product-main", "data-js-pid");
               String available = CrawlerUtils.scrapStringSimpleInfoByAttribute(element, ".c-swatch", "aria-disabled");
               String variationName = productName + " " + CrawlerUtils.scrapStringSimpleInfo(element, ".c-variations-carousel__value", false);;
//               String variationName = element.hasText() ? productName + " " + element.text().split(" ")[0].trim() : productName;
               Offers offers = available == null ? scrapOffers(doc) : new Offers();

               Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(variationName)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setDescription(description)
                  .setCategories(categories)
                  .setRatingReviews(ratingsReviews)
                  .setOffers(offers)
                  .build();
               products.add(product);
            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".c-product-main") != null;
   }

   public String crawlDescription(Document doc) {
      StringBuilder description = new StringBuilder();
      Element prodInfoElement = doc.selectFirst(".l-section__row");
      String subtitle = CrawlerUtils.scrapStringSimpleInfo(doc, ".c-product-main__subtitle", false);
      String descriptionTypeOne = CrawlerUtils.scrapStringSimpleInfo(doc, ".l-section .c-tabs .c-tabs__content", false);
      String descriptionTypeTwo = CrawlerUtils.scrapStringSimpleInfo(doc, ".l-column.h-text-self-align-center-for-large", false);
      String descriptionTypeThree = CrawlerUtils.scrapStringSimpleInfo(prodInfoElement, ".l-row div:nth-child(1)", false);

      if (prodInfoElement != null) {
         description.append(prodInfoElement);
      }
      if (descriptionTypeOne != null && !descriptionTypeOne.isEmpty()) {
         descriptionTypeOne = subtitle + " " + descriptionTypeOne;
         description.append(descriptionTypeOne);
      }
      if (descriptionTypeTwo != null && !descriptionTypeTwo.isEmpty()) {
         descriptionTypeTwo = subtitle + " " + descriptionTypeTwo;
         description.append(descriptionTypeTwo);
      }
      if (descriptionTypeThree != null && !descriptionTypeThree.isEmpty()) {
         descriptionTypeThree = subtitle + " " + descriptionTypeThree;
         description.append(descriptionTypeThree);
      }
      return description.toString();
   }

   private List<String> getSecondaryImages(Document doc) {
      List<String> secondaryImages = new ArrayList<>();

      Elements imagesLi = doc.select(".c-product-detail-image__alternatives .c-carousel .c-carousel__content .c-carousel__item button img");
      for (Element imageLi : imagesLi) {
         secondaryImages.add(imageLi.attr("src"));
      }
      if (secondaryImages.size() > 0) {
         secondaryImages.remove(0);
      }
      return secondaryImages;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(new Offer.OfferBuilder()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(this.sellerName)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {

      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(
         doc, ".c-product-price .c-product-price__value.m-new", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc,
         ".c-product-price .c-product-price__value.m-old", null, false, ',', session);
      if (spotlightPrice == null) {
         Element spanSelected = doc.selectFirst(".c-product-price span:nth-child(4)");
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(spanSelected, ".c-product-price__value", null, false, ',', session);
      }

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private CreditCards scrapCreditCards(Double price) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(price)
         .setFinalPrice(price)
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

   private RatingsReviews ratingsReviews(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();

      Integer totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtml(doc, ".c-product-main__review .c-product-main__review-total", false, null);
      Double avgRating = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".c-product-main__review .c-product-main__review-count", null, false, '.', session);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);

      return ratingReviews;
   }
}

