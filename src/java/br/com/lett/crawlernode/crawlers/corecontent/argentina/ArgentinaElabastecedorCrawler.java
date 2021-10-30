package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArgentinaElabastecedorCrawler extends Crawler {

   private static final String SELLER_FULL_NAME= "El Abastecedor";
   private static final List<String> cards = Arrays.asList(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString());

   public ArgentinaElabastecedorCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (doc.selectFirst(".product-details-area") != null) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String internalId = getInternalId();
         String internalPid = internalId;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc,".product-details-area .product-details-content h2",true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc,"#product-zoom", Arrays.asList("src"),"https:","www.elabastecedor.com.ar");
         Boolean available = true;
         Offers offers = available? scrapOffers(doc): new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setOffers(offers)
            .build();

         products.add(product);
   } else {
      Logging.printLogDebug(logger, session, "Not a product page:   " + this.session.getOriginalURL());
   }
      return  products;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {

      Offers offers = new Offers();

      Pricing pricing = scrapPricing(doc);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double formatedPrice = 0.0;
      Double formatedPriceFrom = 0.0;
      String displayedPrice = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-details-area .old-price", false);
      String newPrice = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-details-content .price", false);
      String priceSale = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-details-content .new-label", false);

      if (newPrice != null && priceSale != null) {
         newPrice = newPrice.replaceAll("\\s+","");
         priceSale = priceSale.replaceAll("\\s+","");

         if (newPrice.equals(priceSale)) {
            formatedPriceFrom = null;
            formatedPrice = Double.parseDouble(displayedPrice.replaceAll("[^0-9.]", ""));
         } else {
            formatedPriceFrom = Double.parseDouble(displayedPrice.replaceAll("[^0-9.]", ""));
            formatedPrice = Double.parseDouble(newPrice.replaceAll("[^0-9.]", ""));
         }
      } else {
         formatedPriceFrom = null;
         formatedPrice = Double.parseDouble(displayedPrice.replaceAll("[^0-9.]", ""));
      }

      CreditCards creditCards = scrapCreditcard(formatedPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(formatedPriceFrom)
         .setSpotlightPrice(formatedPrice)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditcard(Double price) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(price)
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

   private String getInternalId() {
      String [] urlId = this.session.getOriginalURL().split("_");

      return urlId[0].replaceAll("[^0-9]", "");
   }
}
