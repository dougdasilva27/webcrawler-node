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
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ArgentinaMasfarmaciasCrawler extends Crawler {


   private static final String SELLER_FULL_NAME = "Mas Farmacias Argentina";
   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString(),
      Card.DINERS.toString());


   public ArgentinaMasfarmaciasCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-essential .no-display > input", "value");
         String internalPid = internalId;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name h2", true);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs li:not(:first-child):not(:last-child) a");
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-image img", Arrays.asList("data-zoom-image"), "https", "www.masfarmacias.com");
         //site hasn't secondary images
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".short-description > p", "section#panel1.content", "section#panel2.content", "section#panel3.content", "section#panel4.content"));
         boolean available = !doc.select(".mod_button #product-addtocart-button[title='Comprar']").isEmpty();
         Offers offers = available ? scrapOffers(doc, internalId) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".product-view") != null;
   }

   private Offers scrapOffers(Document doc, String internalId) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc, internalId);
      List<String> sales = scrapSales(pricing, doc);

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

   private List<String> scrapSales(Pricing pricing, Document doc) {
      List<String> sales = new ArrayList<>();
      if (scrapSalePromo(doc) != null) {
         sales.add(scrapSalePromo(doc));
      }
      if (scrapSaleDiscount(pricing) != null) {
         sales.add(scrapSaleDiscount(pricing));
      }
      return sales;
   }

   private String scrapSaleDiscount(Pricing pricing) {
      return CrawlerUtils.calculateSales(pricing);
   }

   private String scrapSalePromo(Document doc) {

      String sale = null;
      String firstSplit = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".amlabel-div tr td", "style");
      String[] firstArray = firstSplit != null ? firstSplit.split("\\(") : null;
      if (firstArray != null && firstArray.length > 1) {
         String[] secondArray = firstArray[1].split("\\)");
         if (secondArray.length > 1) {
            sale = secondArray[0];
         }
      }
      return sale;
   }


   private Pricing scrapPricing(Document doc, String internalId) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#product-price-" + internalId, null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#old-price-" + internalId, null, false, ',', session);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
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

   //site hasn't rating

}
