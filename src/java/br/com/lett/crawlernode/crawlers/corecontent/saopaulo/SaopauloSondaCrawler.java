package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

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

public class SaopauloSondaCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.sondadelivery.com.br/";

   private static final String SELLER_FULL_NAME = "Sonda Sao Paulo";
   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString(),
      Card.DINERS.toString());

   public SaopauloSondaCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawlInternalId(session.getOriginalURL());
         String internalPid = internalId;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h3.product--title_in", false);
         boolean available = crawlAvailability(doc);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li > a");
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#owl-carousel--gallery li img", Arrays.asList("src"), "hhtps", "www.sondadelivery.com.br");
         List<String> images = CrawlerUtils.scrapSecondaryImages(doc, "#owl-carousel--gallery li img", Arrays.asList("src"), "hhtps", "www.sondadelivery.com.br", primaryImage);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".product-details"));
         Offers offers = available ? scrapOffers(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(doc, pricing);

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

   private List<String> scrapSales(Document doc, Pricing pricing) {
      List<String> sales = new ArrayList<>();
      if (scrapSalePromo(doc) != null) {
         sales.add(scrapSalePromo(doc));
      }

      if (scrapSaleDiscount(pricing) != null) {
         sales.add(scrapSaleDiscount(pricing));
      }

      return sales;
   }

   private String scrapSalePromo(Document doc) {
      return CrawlerUtils.scrapStringSimpleInfo(doc, ".product-gift p", false);
   }

   private String scrapSaleDiscount(Pricing pricing) {
      return CrawlerUtils.calculateSales(pricing);
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product--price .price strong", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product--price .price-old", null, false, ',', session);
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

   private boolean isProductPage(Document doc) {
      return !doc.select("h3.product--title_in").isEmpty();
   }

   private String crawlInternalId(String url) {
      return CommonMethods.getLast(url.split("/"));
   }

   private boolean crawlAvailability(Document document) {
      return document.select("strong.noestoque").first() == null;
   }

   //Site hasn't ratings

}
