package br.com.lett.crawlernode.crawlers.corecontent.chile;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.Pair;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.jsoup.nodes.Document;

import java.util.*;

public class ChileFerretekCrawler extends Crawler {

   private static final String HOME_PAGE = "https://herramientas.cl/";
   private static final String SELLER_FULL_NAME = "Ferretek";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AMEX.toString(), Card.DINERS.toString());

   public ChileFerretekCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if(isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc,".product_id", "value");
         String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".js_main_product div:first-child", true);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "#product_details > h1", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".carousel-inner .active img", Collections.singletonList("src"), "https", "herramientas.cl");
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".carousel-inner .carousel-item:not(:first-child) img", Collections.singletonList("src"), "https", "herramientas.cl", primaryImage);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb-item:not(:first-child, :last-child)", false);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList("#product_full_description"));
         boolean availableToBuy = checkIfIsAvailable(doc);

         Offers offers = availableToBuy ? scrapOffer(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setCategories(categories)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean checkIfIsAvailable(Document doc) {
     return doc.select(".out-stock-msg").isEmpty();
   }

   private Offers scrapOffer(Document doc) throws MalformedPricingException, OfferException {
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
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".oe_currency_value", null, true, ',', session);
      CreditCards creditCards = scrapCreditCards(doc);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Document doc) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Double cardPrice;
      Integer installmentsNumber = 1;

      if(hasInstallments(doc)) {
         cardPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".text-muted > .oe_currency_value", null, true, ',', session);
         Pair<Integer, Float> installment = CrawlerUtils.crawlSimpleInstallment(".text-muted", doc, false, "x", ", sin", true, ',');
         if (!installment.isAnyValueNull()) {
            installmentsNumber = installment.getFirst();
         }
      } else {
         cardPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".oe_currency_value", null, true, ',', session);
      }

      Installments installments = new Installments();
      if(installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(installmentsNumber)
            .setInstallmentPrice(cardPrice)
            .build());
      }

      for(String card: cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private boolean hasInstallments(Document doc) {
      return !doc.select("h4 .text-muted > .oe_currency_value").isEmpty();
   }

   private boolean isProductPage(Document doc) {
      return doc.select("#product_details") != null;
   }
}
