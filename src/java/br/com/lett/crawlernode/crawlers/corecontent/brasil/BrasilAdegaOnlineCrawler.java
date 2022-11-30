package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.*;
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

public class BrasilAdegaOnlineCrawler extends Crawler {
   private static String SELLER_NAME = "";

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.ELO.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilAdegaOnlineCrawler(Session session) {
      super(session);
      SELLER_NAME = session.getMarket().getName();
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String countryName = scrapCountry(doc);
         String title = CrawlerUtils.scrapStringSimpleInfo(doc, ".ProductMeta__Title", false);
         String name = title != null && countryName != null ? title + " " + countryName : title;
         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=\"store_product_id\"]", "value");
         String description = CrawlerUtils.scrapStringSimpleInfo(doc, ".box-short-description", false);
         String primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "[property=\"og:image\"]", "content");
         String available = CrawlerUtils.scrapStringSimpleInfo(doc, ".ProductForm__AddToCart.Button.Button--secondary", false);
         Offers offers = available != null && !available.contains("Sold Out") ? scrapOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setOffers(offers)
            .build();
         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page:   " + this.session.getOriginalURL());
      }
      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".Product__Wrapper") != null;
   }

   private String scrapCountry(Document doc) {
      Elements box = doc.select(".box-specification strong");

      for (Element info : box) {
         String infoType = CrawlerUtils.scrapStringSimpleInfo(info, null, true);
         if (infoType != null && infoType.equals("PAÍS:")) {
            return CrawlerUtils.scrapStringSimpleInfo(info, "span", false);
         }
      }

      return null;
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
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".Price--highlight", null, true, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".Price--compareAt", null, true, ',', session);

      if (spotlightPrice == null) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span.Price", null, true, ',', session);
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Double price) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(price)
         .setFinalPrice(price)
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
}
