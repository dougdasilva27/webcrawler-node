package br.com.lett.crawlernode.crawlers.corecontent.argentina;

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
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Date: 08/10/2018
 *
 * @author Gabriel Dornelas
 *
 */
public class ArgentinaRibeiroCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.ribeiro.com.ar/";
   private static final String SELLER_FULL_NAME = "Ribeiro Argentina";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public ArgentinaRibeiroCrawler(Session session) {
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

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc,"#ArtId","value");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".row.no-gutters .name-prod", false);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "#atg_store_breadcrumbs_mod li:not(:first-child) > a", false);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".col-10.text-center #zoom_03f", Arrays.asList("src"), "https:", "minicuotas.ribeiro.com.ar");
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "#imgAux a",
            Arrays.asList("data-zoom-image", "data-image"), "https:", "minicuotas.ribeiro.com.ar", primaryImage);
         String description = CrawlerUtils.scrapSimpleDescription(doc,Arrays.asList("#description", "#ContenedorDescripciones"));
         boolean availableToBuy = doc.selectFirst(".preboton") != null;
         Offers offers = availableToBuy ? scrapOffer(doc): new Offers();


         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
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
      return !doc.select("#atg_store_main").isEmpty();
   }

   private Offers scrapOffer(Document doc ) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(doc);

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

   private List<String> scrapSales(Document doc) {
      List<String> sales = new ArrayList<>();

      Element salesOneElement = doc.selectFirst(".first_price_discount_container");
      String firstSales = salesOneElement != null ? salesOneElement.text() : null;

      if (firstSales != null && !firstSales.isEmpty()) {
         sales.add(firstSales);
      }

      return sales;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc,"#info_Art #priceOnline s",null,false,'.',session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc,".row.no-gutters .precio1pagoFormat",null,false,'.',session);

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
