package br.com.lett.crawlernode.crawlers.corecontent.panama;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
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

public class PanamaSuperunicoCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Superunico";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString());

   public PanamaSuperunicoCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document document) throws Exception {

      List<Product> products = new ArrayList<>();

      if (isProductPage(document)) {

         String name = CrawlerUtils.scrapStringSimpleInfo(document, "h1.product_title.entry-title", true);
         String internalPid = CrawlerUtils.scrapStringSimpleInfo(document, ".sku_wrapper .sku", true);
         String internalId = getInternalId(document);
         CategoryCollection categories = CrawlerUtils.crawlCategories(document, ".yoast-breadcrumb span a", true);
         String primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, ".product-image-wrap a", "href");
         String description = CrawlerUtils.scrapSimpleDescription(document, Arrays.asList(".woodmart-tab-wrapper #tab-description"));
         boolean available = !document.select(".stock.in-stock").isEmpty();
         Offers offers = available ? scrapOffers(document) : new Offers();

         products.add(ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setName(name)
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setOffers(offers)
            .build());

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private String getInternalId(Document document) {
      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, ".single-product-page", "id");
      return internalId != null ? CommonMethods.getLast(internalId.split("-")) : null;

   }

   private boolean isProductPage(Document document) {
      return !document.select(".product-image-summary").isEmpty();
   }

   private Offers scrapOffers(Document document) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(document);

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
      Double spotlightPrice = getPrice(doc);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price del .woocommerce-Price-amount bdi", null, true, '.', session);

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setPriceFrom(priceFrom)
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


   private Double getPrice(Document document) {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(document, ".price ins .woocommerce-Price-amount bdi", null, true, '.', session);
      if (spotlightPrice == null) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(document, ".summary-inner .woocommerce-Price-amount.amount bdi", null, true, '.', session);

      }
      return spotlightPrice;
   }
}
