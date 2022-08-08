package br.com.lett.crawlernode.crawlers.corecontent.costarica;

import br.com.lett.crawlernode.core.models.Card;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CostaricaPeridomicilio extends Crawler {

   private static final Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.DINERS.toString(), Card.AMEX.toString(), Card.ELO.toString());

   public CostaricaPeridomicilio(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = scrapId(doc);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".ty-product-bigpicture > .ty-product-bigpicture__left > .ty-product-bigpicture__left-wrapper > h1", false);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".ty-wysiwyg-content.content-description > div"));
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".ty-pict.cm-image", Arrays.asList("src"), "http://", "www.peridomicilio.com");
         boolean isAvailable = CrawlerUtils.scrapStringSimpleInfo(doc, ".ty-btn__primary.ty-btn__big.ty-btn__add-to-cart.cm-form-dialog-closer.ty-btn", true) != null;
         Offers offers = isAvailable ? scrapOffers(doc, internalId) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
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

   private String scrapId(Document doc) {
      String id = null;
      String placeHolder = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".ty-product-bigpicture > .ty-product-bigpicture__right > form", "name");
      if (placeHolder != null) {
         String regex = "form_(\\d+)";
         Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
         Matcher matcher = pattern.matcher(placeHolder);
         while (matcher.find()) {
            id = matcher.group(1);
         }
      }
      return id;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".ty-product-bigpicture__left-wrapper") != null;
   }

   private Offers scrapOffers(Document doc, String internalId) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc, internalId);

      if (pricing != null) {
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName("peridomicilio")
            .setSellersPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .build());
      }

      return offers;
   }

   private Pricing scrapPricing(Document doc, String internalId) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#sec_old_price_" + internalId, null, false, '.', session);
      ;
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#sec_discounted_price_" + internalId, null, false, '.', session);
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
      if (installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

}
