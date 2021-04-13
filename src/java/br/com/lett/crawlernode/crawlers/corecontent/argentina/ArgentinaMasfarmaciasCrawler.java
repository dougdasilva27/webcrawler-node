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

import java.util.*;

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
         Logging.printLogInfo(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".aws-container", "data-page-id");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product_title.entry-title", true);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".woocommerce-breadcrumb a:not(:first-child)");
         String primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "[property=og:image]", "content");
         //site hasn't secondary images
         String description = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList(".elementor-tabs-content-wrapper"));
         boolean available = crawlAvailability(doc);
         Offers offers = available ? scrapOffers(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);
         Logging.printLogInfo(logger, session, "Product added.");

      } else {
         Logging.printLogInfo(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("[data-elementor-type=product]") != null;
   }

   private boolean crawlAvailability(Document doc) {
      Elements cartButtonElements = doc.select("span.elementor-button-content-wrapper > span.elementor-button-text");

      for (Element e : cartButtonElements) {
         if (e.text().equals("Comprar")) {
            return true;
         }
      }
      return false;
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

   private List<String> scrapSales(Pricing pricing, Document doc) {
      List<String> sales = new ArrayList<>();

      if(pricing.getPriceFrom() != null){
         sales.add(CrawlerUtils.calculateSales(pricing));
      }
      String urlPromoImg = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".elementor-animation-skew-forward.descuento-img", "src");

      if(urlPromoImg != null){
         String[] firstArray = urlPromoImg.split("/");

         if(firstArray.length > 0){
            String promo = firstArray[firstArray.length - 1];
            if(promo.contains("x")) sales.add(promo.replace(".", ""));
         }
      }

      return sales;
   }

   //site hasn't rating

}
