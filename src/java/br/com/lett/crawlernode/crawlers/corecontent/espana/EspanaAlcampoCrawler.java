package br.com.lett.crawlernode.crawlers.corecontent.espana;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.models.Card;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EspanaAlcampoCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Alcampo";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public EspanaAlcampoCrawler(Session session) {
      super(session);
   }

   @Override
   protected Object fetch() {
      Document doc = null;

      int attempts = 0;
      boolean success = false;

      List<String> proxies = List.of(ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_ANY_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY);
      do {

         try {
            webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), proxies.get(attempts), session);

            webdriver.waitLoad(30000);
            doc = Jsoup.parse(webdriver.getCurrentPageSource());
            success = !doc.select(".productDesc h1").isEmpty();

         } catch (Exception e) {
            Logging.printLogInfo(logger, session, CommonMethods.getStackTrace(e));

         } finally {
            if (webdriver != null) {
               webdriver.terminate();
            }
         }
      } while (!success && attempts++ < (proxies.size() - 1));

      return doc;

   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".productCode", "value");
         String internalPid = null;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".productDesc h1", true);
         String primaryImage = null;
         String description = null;
         boolean availableToBuy = doc.select(".outOfStock").isEmpty();
         Offers offers = availableToBuy ? scrapOffer(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
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
      return doc.selectFirst(".productDesc h1") != null;
   }

   private Offers scrapOffer(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);

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


   private Double scrapSpotlightPrice(Document doc) {
      Double decimals = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".big-price.price.right.precio", null, true, ',', session);
      Double cents = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".big-price.price.right. strong", null, true, ',', session);

      Double spotlightPrice = (decimals * 100) + cents;

      return spotlightPrice;

   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = scrapSpotlightPrice(doc);
      Double priceFrom = null;

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

}
