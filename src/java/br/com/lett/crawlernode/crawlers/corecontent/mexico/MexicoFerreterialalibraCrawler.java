package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MexicoFerreterialalibraCrawler extends Crawler {

   private static final String MAIN_SELLER_NAME = "Ferreteria La Libra";
   private final Set<String> cards = Sets.newHashSet(Card.VISA.toString(),
      Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.ELO.toString(), Card.DINERS.toString());

   public MexicoFerreterialalibraCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {

         String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".sku_wrapper .sku", true);
         String internalPid = crawlInternalPid(doc);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product_title.entry-title", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".ct-image-container", Collections.singletonList("href"), "https", "ferreterialalibra.com");
         List<String> images = CrawlerUtils.scrapSecondaryImages(doc, ".flexy-pills li span img", Collections.singletonList("src"), "https", "ferreterialalibra.com", primaryImage);
         String description = CrawlerUtils.scrapElementsDescription(doc, Collections.singletonList(".woocommerce-tabs.wc-tabs-wrapper"));
         boolean availability = doc.selectFirst(".summary.entry-summary .stock.in-stock") != null;
         Offers offers = availability ? scrapOffers(doc) : new Offers();
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".ct-breadcrumbs span a span", true);

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setCategories(categories)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);

      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".ct-container-full .product-entry-wrapper") != null;
   }

   private String crawlInternalPid(Element e) {
      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product.type-product", "id");
      if (internalId != null && internalId.contains("-")) {
         internalId = internalId.split("-")[1];
      }
      return internalId;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);

      if (pricing != null) {
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(MAIN_SELLER_NAME)
            .setSellersPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .build());
      }
      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".summary.entry-summary .price .woocommerce-Price-amount.amount bdi", null, false, '.', this.session);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(null)
         .setCreditCards(scrapCreditCards(spotlightPrice))
         .setBankSlip(scrapBankSlip(spotlightPrice))
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

   private BankSlip scrapBankSlip(Double spotlightPrice) throws MalformedPricingException {
      return BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();
   }
}
