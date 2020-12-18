package br.com.lett.crawlernode.crawlers.extractionutils.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.jsoup.nodes.Document;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip;
import models.pricing.CreditCard;
import models.pricing.CreditCards;
import models.pricing.Installment;
import models.pricing.Installments;
import models.pricing.Pricing;

public abstract class RomaniaBringoCrawler extends Crawler {

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public RomaniaBringoCrawler(Session session) {
      super(session);
   }

   protected abstract String getMainSeller();

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);

      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "#sylius-product-name", true);
         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".modal-body input", "value");
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-quantity button.decrement-quantity", "data-product_id");
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "img#main-image", Collections.singletonList("src"), "https", "https://storage.googleapis.com/");
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".bringo-product-details-tabs-menu", ".bringo-product-details-tabs-content"));
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bringo-breadcrumb a", true);
         RatingsReviews ratingsReviews = scrapRatingReviews(doc);
         boolean available = !doc.select(".bringo-product-details > .row:nth-of-type(1) .add-to-cart-btn").isEmpty();
         Offers offers = available ? scrapOffers(doc) : new Offers();

         // This site only have the internalId on url when product is unnavailable =/
         if (internalId == null && !available) {
            internalId = CommonMethods.getLast(CommonMethods.getLast(session.getOriginalURL().split("\\?")).split("/"));
         }

         Product product = ProductBuilder.create()
               .setName(name)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setPrimaryImage(primaryImage)
               .setDescription(description)
               .setCategories(categories)
               .setRatingReviews(ratingsReviews)
               .setOffers(offers)
               .setUrl(session.getOriginalURL())
               .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".bringo-product-details").isEmpty();
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {

      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = new ArrayList<>();

      offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(getMainSeller())
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setSales(sales)
            .setPricing(pricing)
            .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {

      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-price", null, true, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-price-discount", null, true, ',', session);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
            .setFinalPrice(spotlightPrice)
            .build();

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
            .setPriceFrom(priceFrom)
            .setSpotlightPrice(spotlightPrice)
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

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
               .setBrand(card)
               .setInstallments(installments)
               .setIsShopCard(false)
               .build());
      }

      return creditCards;
   }

   private RatingsReviews scrapRatingReviews(Document doc) {

      RatingsReviews ratingsReviews = new RatingsReviews();

      int totalReviews = CrawlerUtils.scrapIntegerFromHtml(doc, ".product-reviews a.item span", true, 0);
      Double avgRating = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-reviews .item #average-rating", "data-average-rating", true, '.', session);

      ratingsReviews.setTotalRating(totalReviews);
      ratingsReviews.setTotalWrittenReviews(totalReviews);
      ratingsReviews.setAverageOverallRating(avgRating);

      return ratingsReviews;
   }
}
