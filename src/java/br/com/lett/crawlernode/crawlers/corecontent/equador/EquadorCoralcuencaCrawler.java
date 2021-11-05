package br.com.lett.crawlernode.crawlers.corecontent.equador;

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
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class EquadorCoralcuencaCrawler extends Crawler {
   private static final String SELLER_FULL_NAME = "Coral";

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


   public EquadorCoralcuencaCrawler(Session session) {
      super(session);
   }

   @Override
   protected Object fetch() {

      webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), ProxyCollection.BUY_HAPROXY, session);
      Logging.printLogInfo(logger, session, "awaiting product page");


      webdriver.waitForElement(".lotties", 250);

      webdriver.waitLoad(3000);

      WebElement city = webdriver.driver.findElement(By.cssSelector(".botonSelectCiudad"));
      webdriver.clickOnElementViaJavascript(city);

      webdriver.waitPageLoad(20);

      Document doc = Jsoup.parse(webdriver.getCurrentPageSource());


      return doc;
   }

   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      List<Product> products = new ArrayList<>();

      if (document.selectFirst(".nomDescCarritoDes") != null) {
         Logging.printLogDebug(logger, "product page identify");

         String internalId = CommonMethods.getLast(session.getOriginalURL().split("/"));
         String primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, ".content-imagen-principal img", "src");


         products.add(ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(CrawlerUtils.scrapStringSimpleInfo(document, ".nomDescCarritoDes", true))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(CrawlerUtils.scrapSimpleSecondaryImages(document, ".images button img", Arrays.asList("src"), "https", "s3.amazonaws.com", primaryImage))
            .setDescription(CrawlerUtils.scrapSimpleDescription(document, Arrays.asList(".item-descripcion.row .col-xs-12 p[style]")))
            .setOffers(scrapOffer(document))
            .build());


      } else {

         Logging.printLogDebug(logger, "is not a product page");
      }
      return products;

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

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".preProductoCarDes", null, false, ',', session);
      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }


   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallments(spotlightPrice);
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

   public Installments scrapInstallments(Double spotlightPrice) throws MalformedPricingException {
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      return installments;
   }
}
