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

   private static final String SELLER_FULL_NAME = "El Abastecedor";
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
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-details-area .product-details-content h2", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".zoompro-border.zoompro-span .zoompro", Arrays.asList("src"), "https:", "www.elabastecedor.com.ar");
         List<String> productSecondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".product-dec-slider-2.swiper-container .swiper-wrapper .swiper-slide .active", Arrays.asList("data-image"), "https:", "www.elabastecedor.com.ar", primaryImage);
         Boolean available = !doc.select(".BUTTON_VERDE").isEmpty();
         Offers offers = available ? scrapOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setSecondaryImages(productSecondaryImages)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page:   " + this.session.getOriginalURL());
      }

      return products;
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
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".old-price", null, true, '.', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".current-price", null, true, '.', session);

      CreditCards creditCards = scrapCreditcard(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
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
      String[] urlId = this.session.getOriginalURL().split("_");
      return urlId[0].replaceAll("[^0-9]", "");
   }
}
