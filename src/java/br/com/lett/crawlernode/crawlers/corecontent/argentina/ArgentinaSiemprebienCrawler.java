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
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ArgentinaSiemprebienCrawler extends Crawler {


   private static final String SELLER_FULL_NAME = "Siempre Bien Argentina";
   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString(),
      Card.DINERS.toString());


   public ArgentinaSiemprebienCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawlInternalId(doc);
         String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, "tbody > tr:last-child .ficha-dato", true);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1[itemprop='name'] span", true);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "#body .container .row ul[style='height:auto;'] li:not(:first-child):not(:last-child) a span");
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#g_image img", Arrays.asList("src"), "https", "www.siemprebien.com.ar");
         List<String> secondaryImage = CrawlerUtils.scrapSecondaryImages(doc, ".allthumbs a", Arrays.asList("data-image"), "https", "www.siemprebien.com.ar", primaryImage);
         String description = crawlDescription(doc);
         boolean available = doc.select(".BUY .nonavailable .price").isEmpty();
         Offers offers = available ? scrapOffers(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImage)
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
      return doc.selectFirst("tbody > tr:last-child .ficha-dato") != null;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
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
      return CrawlerUtils.scrapStringSimpleInfo(doc, ".badge-promos > span", true);
   }

   private String crawlInternalId(Document doc) {
      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".price_wrapper a", "data-id");
      if (internalId == null || internalId.isEmpty()) {
         internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "a.button-extra.BUTTONASK", "data-id");
      }
      return internalId;
   }


   private String crawlDescription(Document doc) {
      StringBuilder descriptions = new StringBuilder();
      String description = CrawlerUtils.scrapStringSimpleInfo(doc, ".tab-pane.fade.active", false);
      if (description != null) {
         descriptions.append(description);
      }
      Elements elements = doc.select("tbody > tr");
      if (elements != null) {
         for (Element el : elements) {
            descriptions.append(el.selectFirst(".ficha-tit"));
            descriptions.append(el.selectFirst(".ficha-dato"));
         }
      }

      return descriptions.toString();
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price > span", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".tachado span", null, false, ',', session);
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
