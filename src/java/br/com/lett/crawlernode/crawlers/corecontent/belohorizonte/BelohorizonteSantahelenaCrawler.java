package br.com.lett.crawlernode.crawlers.corecontent.belohorizonte;


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
import org.jsoup.nodes.Element;

import java.util.*;

public class BelohorizonteSantahelenaCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Santa Helena";
   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString());

   public BelohorizonteSantahelenaCrawler(Session session) {
      super(session);
   }

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
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".woocommerce-breadcrumb span a", true);
         boolean available = doc.select(".stock.out-of-stock").isEmpty();
         Offers offers = available ? scrapOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
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
      return doc.selectFirst(".product-template-default") != null;
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
      Double spotlightPrice = null;
      Double priceFrom = null;

      Element priceElement = doc.selectFirst(".price");

      if (priceElement != null) {
         if (priceElement.children().size() > 1) {
            spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(priceElement, "ins > .woocommerce-Price-amount.amount > bdi" , null, false, ',', session);
            priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(priceElement, "del > .woocommerce-Price-amount.amount > bdi", null, false, ',', session);
         }else{
            spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(priceElement, ".woocommerce-Price-amount.amount > bdi" , null, false, ',', session);
         }
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

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
