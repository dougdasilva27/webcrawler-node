package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BrasilCasaDaBebidaCrawler extends Crawler {

   private static final String SELLER_NAME = "Casa da Bebida";
   private static final List<String> cards = Arrays.asList(Card.DISCOVER.toString(), Card.ELO.toString(),
      Card.AMEX.toString(), Card.MASTERCARD.toString(), Card.VISA.toString());

   public BrasilCasaDaBebidaCrawler(Session session) {
      super(session);
//      super.config.setFetcher(FetchMode.APACHE);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (doc.selectFirst(".product-info") != null) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, "[id=product-id]", false);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-info h2 [itemprop=name]", false);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "img[itemprop=\"image\"]", Arrays.asList("src"), "https", "");
         boolean available = doc.selectFirst("button.add-cart") != null;
         Offers offers = available ? scrapOffers(doc) : new Offers();
         String description = CrawlerUtils.scrapStringSimpleInfo(doc, ".produto-descricao", false);

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setOffers(offers)
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page:   " + this.session.getOriginalURL());
      }
      return products;
   }


   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(new Offer.OfferBuilder()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(this.SELLER_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".prices .displaynone", null, true, '.', session);
      Double priceFrom;

      String priceFromString = CrawlerUtils.scrapStringSimpleInfo(doc, ".prices .price-of", false);
      priceFrom = formatPrice(priceFromString);
      CreditCards creditCards = scrapCreditCards(spotlightPrice, doc);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Double price, Document doc) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      String installmentPriceScrap = CrawlerUtils.scrapStringSimpleInfo(doc, "#dropdownMenuButton :nth-child(2)", false);
      String installmentNumbers = CrawlerUtils.scrapStringSimpleInfo(doc, "#dropdownMenuButton span:first-of-type", false);
      String finalPriceString = CrawlerUtils.scrapStringSimpleInfo(doc, ".price span span", false);

      int installmentNumber = formatPrice(installmentNumbers).intValue();
      Double finalPrice = formatPrice(finalPriceString);
      Double installmentPrice = formatPrice(installmentPriceScrap);

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(installmentNumber)
         .setInstallmentPrice(installmentPrice)
         .setFinalPrice(finalPrice)
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

   private Double formatPrice(String priceString) {
      String priceFormated;
      if (priceString != null) {
         priceFormated = priceString.replaceAll("[^0-9-,]+", "");
         return Double.parseDouble(priceFormated.replace(',', '.'));
      }

      return 0.0;
   }

}
