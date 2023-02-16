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
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BrasilSvicenteCrawler extends Crawler {
   public BrasilSvicenteCrawler(Session session) {
      super(session);
   }

   private final static String SELLER_FULL_NAME = "SVicente";

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".productQuantity", "data-pid");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1", true);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadCrumb__option a");
         String primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".productDetails__images__principal__img", "src");
         boolean availableToBuy = scrapAvailability(doc);
         Offers offers = availableToBuy ? scrapOffer(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean scrapAvailability(Document doc) {
      String availableString = CrawlerUtils.scrapStringSimpleInfo(doc, ".svAddToCartButton.primaryButton", true);
      if (availableString != null && !availableString.isEmpty()) {
         return availableString.equals("Comprar");
      }
      return false;
   }

   private Offers scrapOffer(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      String saleDiscount = CrawlerUtils.scrapStringSimpleInfo(doc, ".productPrice.productPrice--flag", false);
      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setSales(saleDiscount != null ? List.of(saleDiscount) : null)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".productPrice__lineThrough", null, true, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".productPrice__price", null, true, ',', session);

      if (spotlightPrice == null) {
         spotlightPrice = priceFrom;
         priceFrom = null;
      }

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
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.GOOD_CARD.toString(), Card.ELO.toString(), Card.HIPERCARD.toString(), Card.MASTERCARD.toString(),
         Card.DINERS.toString(), Card.AMEX.toString(), Card.ALELO.toString(), Card.VR_CARD.toString(), Card.FACIL.toString());

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".productDetails") != null;
   }
}
