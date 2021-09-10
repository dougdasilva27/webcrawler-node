package br.com.lett.crawlernode.crawlers.corecontent.argentina;

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
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ArgentinaHipermaniaCrawler extends Crawler {

   public ArgentinaHipermaniaCrawler(Session session) {
      super(session);
   }

   private static final String SELLER_FULL_NAME = "Hipermania";
   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString());


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = doc.selectFirst(".product.type-product").attr("id").replaceAll("[^0-9]", "");

         String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, ".sku_wrapper .sku", true);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product_title.entry-title", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".images a", Collections.singletonList("href"), "https", "santahelenacenter.com.br");
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".woocommerce-breadcrumb a", true);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList(".woocommerce-tabs.wc-tabs-wrapper"));
         boolean available = doc.select(".stock .out-of-stock").isEmpty();
         Offers offers = available ? scrapOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setCategories(categories)
            .setOffers(offers)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("#tab-description") != null;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
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

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice;
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".summary .price del .woocommerce-Price-amount.amount", null, false, '.', session);

      if (priceFrom != null) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price ins .woocommerce-Price-amount.amount", null, false, '.', session);

      } else {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price .woocommerce-Price-amount.amount", null, false, '.', session);
      }
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
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


}
