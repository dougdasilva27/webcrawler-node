package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
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
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class MexicoSingerCrawler extends Crawler {

   private static final Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.DINERS.toString(), Card.AMEX.toString(), Card.ELO.toString());

   public MexicoSingerCrawler(Session session) {
      super(session);
      config.setFetcher(FetchMode.FETCHER);
   }

   public static void waitForElement(WebDriver driver, String cssSelector) {
      WebDriverWait wait = new WebDriverWait(driver, 20);
      wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)));
   }

   protected Document fetch() {
      Document doc = null;

      try {
         List<String> proxies = Arrays.asList(ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY, ProxyCollection.BUY_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_MX);
         int attemp = 0;
         do {
            if (attemp != 0) {
               webdriver.terminate();
            }
            webdriver = DynamicDataFetcher
               .fetchPageWebdriver(session.getOriginalURL(), proxies.get(attemp), session);
            webdriver.waitLoad(30000);
            doc = Jsoup.parse(webdriver.getCurrentPageSource());
         } while (doc.select(".col-xs-12.col-sm-12.col-md-6.pdp-content-wrapper.no-padding-left.no-padding-right > div > .price > span.current").isEmpty() && attemp++ < 3);

         webdriver.waitLoad(2000);
         if (doc.selectFirst(".col-xs-12.col-sm-12.col-md-6.pdp-content-wrapper.no-padding-left.no-padding-right > div > div.price > span.current") != null) {
            WebElement finalScearch = webdriver.driver.findElement(By.cssSelector(".col-xs-12.col-sm-12.col-md-6.pdp-content-wrapper.no-padding-left.no-padding-right > div > div.price > span.current"));
            webdriver.clickOnElementViaJavascript(finalScearch);
            waitForElement(webdriver.driver, ".col-xs-12.col-sm-12.col-md-6.pdp-content-wrapper.no-padding-left.no-padding-right > div > div.price > span.current");
            webdriver.waitLoad(5000);
            doc = Jsoup.parse(webdriver.getCurrentPageSource());

            webdriver.terminate();

         }


      } catch (Exception e) {
         Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
         Logging.printLogWarn(logger, "Parse não realizado");
         webdriver.terminate();
      }
      return doc;
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#vue-pdp-page > div > div > div", "data-sku");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".col-xs-12.col-sm-12.col-md-6.pdp-content-wrapper.no-padding-left.no-padding-right > div > h1", false);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#collapseOne > div > p:first-child", "#collapseOne > div > p:last-child"));
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".simpleLens-container > div > a > img", Arrays.asList("src"), "http://", "www.singer.com/mx");
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".simpleLens-thumbnails-container > div > a", Arrays.asList("data-big-image"), "http://", "www.singer.com/mx", primaryImage);
         String availableHolder = getStock(doc);
         boolean available = availableHolder.contains("In stock");
         Offers offers = available ? scrapOffers(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
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

   private String getStock(Document doc) {
      String finalStock = null;
      Elements stock = doc.select("head > meta");
      for (Element e : stock) {
         String stockHolder = CrawlerUtils.scrapStringSimpleInfo(e,"head > meta",false);
         if (stockHolder!=null){
            if (stockHolder.contains("In stock")||stockHolder.contains("Out of stock")){
               finalStock = stockHolder;
            }
         }
      }
      return finalStock;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("#vue-pdp-page > div > div > div") != null;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);

      if (pricing != null) {
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName("Poli-Pet")
            .setSellersPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .build());
      }

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {

      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".col-xs-12.col-sm-12.col-md-6.pdp-content-wrapper.no-padding-left.no-padding-right > div > div.price > span.current", null, false, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".col-xs-12.col-sm-12.col-md-6.pdp-content-wrapper.no-padding-left.no-padding-right > div > div.price > span.percent.d-md-inline", null, false, ',', session);

      if (spotlightPrice == null) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".col-xs-12.col-sm-12.col-md-6.pdp-content-wrapper.no-padding-left.no-padding-right > div > div.price > span.percent.d-md-inline", null, false, ',', session);
      }

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

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
