package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.jsoup.nodes.Document;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
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

public class BrasilCasavegCrawler extends Crawler {


   private static final String SELLER_FULL_NAME = "Casa Veg";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.ELO.toString(), Card.AMEX.toString());

   public BrasilCasavegCrawler(Session session) {
      super(session);
   }

   private static final String PRODUCT_SELECTOR = "#center_column";

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, PRODUCT_SELECTOR + " input[name=id_product]", "value");
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "span[itemprop=sku]", "content");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, PRODUCT_SELECTOR + " [itemprop=name]", false);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb a:not(.home)");
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#image-block a.jqzoom, #image-block img", Arrays.asList("href", "src"),
               "https", "casaveg.com.br");
         String secondaryImages = null; // at the time this crawler was made, i have not found secondary images
         String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList("#prazo_validade", "section.page-product-box"));
         boolean available = doc.selectFirst(".info-stock.success") != null;

         Offers offers = available ? scrapOffers(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setCategory1(categories.getCategory(0))
               .setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2))
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
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
      return doc.selectFirst(PRODUCT_SELECTOR + " [itemprop=name]") != null;
   }

   /* Start capturing offers for products without variation. */

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, PRODUCT_SELECTOR + " #old_price_display", null, false, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, PRODUCT_SELECTOR + " #our_price_display", null, false, ',', session);
      Pricing pricing = scrapPricing(priceFrom, spotlightPrice);
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

      String sale = CrawlerUtils.scrapStringSimpleInfo(doc, PRODUCT_SELECTOR + " #reduction_percent_display", true);
      if (sale != null && !sale.isEmpty()) {
         sales.add(sale);
      }

      return sales;
   }

   private Pricing scrapPricing(Double priceFrom, Double spotlightPrice) throws MalformedPricingException {

      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlipBuilder.create()
            .setFinalPrice(spotlightPrice)
            .build();

      return PricingBuilder.create()
            .setPriceFrom(priceFrom)
            .setSpotlightPrice(spotlightPrice)
            .setCreditCards(creditCards)
            .setBankSlip(bankSlip)
            .build();

   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallments(spotlightPrice);
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

   public Installments scrapInstallments(Double spotlightPrice) throws MalformedPricingException {
      Installments installments = new Installments();

      installments.add(InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());

      return installments;
   }
}
