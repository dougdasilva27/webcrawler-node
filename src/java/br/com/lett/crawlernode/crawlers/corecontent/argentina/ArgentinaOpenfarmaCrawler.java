package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.jsoup.nodes.Document;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.BankSlip;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ArgentinaOpenfarmaCrawler extends Crawler {

   private final String HOME_PAGE = "https://www.openfarma.com.ar/";
   private static final String SELLER_FULL_NAME = "Open farma";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


   public ArgentinaOpenfarmaCrawler(Session session) {
      super(session);
   }


   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".wrapper", "data-product-id");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".row .product-name", false);
         String description = CrawlerUtils.scrapStringSimpleInfo(doc, ".row article", false);

         if(checkIfHasVariation(doc)) {
            Elements variatons = doc.select(".product-variations-container .btn-group.mg-lg-b");

            if(variatons.size() > 0) {
               for (Element e : variatons) {

                  String grammage = CrawlerUtils.scrapStringSimpleInfo(e, "a", false);
                  String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a", "data-variant-id");
                  String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#zoom .item img",
                     Arrays.asList("src"), "https://", HOME_PAGE);
                  String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "#zoom .slider-for .item:not(.default-image) img",
                     Arrays.asList("src"), "https://", HOME_PAGE, primaryImage);
                  boolean availableToBuy = doc.selectFirst(".row .btn-submit") != null;
                  Offers offers = availableToBuy ? offers(doc, e) : new Offers();

                  // Creating the product
                  Product product = ProductBuilder.create()
                     .setUrl(session.getOriginalURL())
                     .setInternalId(internalId)
                     .setInternalPid(internalPid)
                     .setName(name + " " + grammage)
                     .setPrimaryImage(primaryImage)
                     .setSecondaryImages(secondaryImages)
                     .setDescription(description)
                     .setOffers(offers)
                     .build();

                  products.add(product);
               }
            }
         } else {

            String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#zoom .item img",
               Arrays.asList("src"), "https://", HOME_PAGE);
            String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "#zoom .slider-for .item:not(.default-image) img",
               Arrays.asList("src"), "https://", HOME_PAGE, primaryImage);
            boolean availableToBuy = doc.selectFirst(".row .btn-submit") != null;
            Offers offers = availableToBuy ? offers(doc, null) : new Offers();

            // Creating the product
            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalPid)
               .setInternalPid(internalPid)
               .setName(name)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setOffers(offers)
               .build();

            products.add(product);
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(Document doc) {
      return doc.select("#product").first() != null;
   }

   private boolean checkIfHasVariation(Document doc){
      return doc.selectFirst(".product-variations-container") != null;
   }
   private Offers offers(Document doc, Element variation) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc, variation);
      List<String> sales = scrapSales(doc);


      offers.add(OfferBuilder.create()
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


   private List<String> scrapSales(Document doc) {
      List<String> sales = new ArrayList<>();

      String salesString = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#zoom", "class").replaceAll("[^0-9]", "");

      if (salesString != null && !salesString.isEmpty()) {
         sales.add(salesString + "% OFF"); // On HTML this info appears like this: has-promo promo-20-off so a have to make this little
         // adjustment to show sales like client see
      }

      return sales;
   }

   private Pricing scrapPricing(Document doc, Element variation) throws MalformedPricingException {
      Double priceFrom;
      Double spotlightPrice;

      if (variation != null){

         priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(variation, "a", "data-display-price", false, '.', session);
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(variation, "a", "data-price", false, '.', session);

      } else {

         priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".prices .promo", null, false, ',', session);
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".prices .regular", null, false, ',', session);

      }

      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);
      BankSlip bankslip = BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(bankslip)
         .setCreditCards(creditCards)
         .build();

   }

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallment(spotlightPrice);
      if (installments.getInstallments().isEmpty()) {
         installments.add(InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private Installments scrapInstallment(Double spotlightPrice) throws MalformedPricingException {

      Installments installments = new Installments();

      installments.add(InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      return installments;
   }

}
