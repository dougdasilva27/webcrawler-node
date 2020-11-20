package br.com.lett.crawlernode.crawlers.extractionutils.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;


/**
 * 
 * @author Marcos Moura date: 2020-06-23
 */

public class BrasilMinhacooper extends Crawler {

   private String store_name;

   public void setStore_name(String store_name) {
      this.store_name = store_name;
   }

   private final String HOME_PAGE = "https://www.minhacooper.com.br/loja/" + store_name;

   public BrasilMinhacooper(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   private static final Set<String> cards = Sets.newHashSet(Card.DINERS.toString(), Card.VISA.toString(),
         Card.MASTERCARD.toString(), Card.ELO.toString());
   private static final String SELLER_FULL_NAME = "Minha Cooper";


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (doc.selectFirst(".product-media-details") != null) {

         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-ref", false).replace("REF. ", "");
         String internalPid = internalId;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-detail-section h4", false);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-image #product-zoom", Arrays.asList("src"), "http:", "");
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".col-xs-3 .gallery", Arrays.asList("data-zoom-image"), "http:", "", primaryImage);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs p a");
         boolean availableToBuy = doc.selectFirst(".product-variation__add-actions.product-add-actions") != null;
         Offers offers = availableToBuy ? scrapOffer(doc) : new Offers();

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
               .setOffers(offers)
               .build();

         products.add(product);


      } else {
         Logging.printLogDebug(logger, session, "Not a product page:   " + this.session.getOriginalURL());
      }

      return products;
   }

   private Offers scrapOffer(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
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

      Element salesOneElement = doc.selectFirst(".details-discout-money");
      String firstSales = salesOneElement != null ? salesOneElement.text() : null;

      if (firstSales != null && !firstSales.isEmpty()) {
         sales.add(firstSales);
      }

      return sales;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price .full-price", null, false, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price", null, true, ',', session);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);

      return PricingBuilder.create()
            .setPriceFrom(priceFrom)
            .setSpotlightPrice(spotlightPrice)
            .setCreditCards(creditCards)
            .setBankSlip(bankSlip)
            .build();

   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
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



}
