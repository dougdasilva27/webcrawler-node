package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.*;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
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
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class BrasilMercatoemcasaCrawler extends Crawler {
   private static String SELLER_NAME = "Mercato em Casa";

   private String getCep() {
      return session.getOptions().optString("cep");
   }

   public Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.ELO.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString(), Card.JCB.toString(),
      Card.DISCOVER.toString());

   public BrasilMercatoemcasaCrawler(Session session) {
      super(session);
   }

   @Override
   protected Object fetch() {
      Document doc;
      int attempts = 0;
      List<String> proxies = List.of(ProxyCollection.BUY_HAPROXY, ProxyCollection.SMART_PROXY_BR_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY);
      do {
         try {
            webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), proxies.get(attempts), session);
            webdriver.waitLoad(5000);
            Logging.printLogDebug(logger, "Clicando no modal");
            waitForElement(webdriver.driver, ".ACTION > .SECONDARY");
            WebElement modal = webdriver.driver.findElement(By.cssSelector(".ACTION > .SECONDARY"));
            webdriver.clickOnElementViaJavascript(modal);
            webdriver.waitLoad(10000);

            Logging.printLogDebug(logger, "Alterando CEP");
            waitForElement(webdriver.driver, "#cep-eccomerce-header > a");
            WebElement changeCep = webdriver.driver.findElement(By.cssSelector("#cep-eccomerce-header > a"));
            webdriver.clickOnElementViaJavascript(changeCep);
            webdriver.waitLoad(10000);

            Logging.printLogDebug(logger, "Digitando CEP");
            waitForElement(webdriver.driver, "#cep");
            WebElement cep = webdriver.driver.findElement(By.cssSelector("#cep"));
            cep.sendKeys(getCep());
            waitForElement(webdriver.driver, "button[onclick=\"setCepClickHandler();\"]");
            WebElement send = webdriver.driver.findElement(By.cssSelector("button[onclick=\"setCepClickHandler();\"]"));
            webdriver.clickOnElementViaJavascript(send);
            webdriver.waitLoad(10000);

            doc = Jsoup.parse(webdriver.getCurrentPageSource());
         } catch (Exception e) {
            Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
            return super.fetch();
         }
      } while (doc == null && attempts++ < 3);

      return doc;
   }

   public static void waitForElement(WebDriver driver, String cssSelector) {
      WebDriverWait wait = new WebDriverWait(driver, 10);
      wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)));
   }

   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(document)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String productName = CrawlerUtils.scrapStringSimpleInfo(document, ".PRODUCT_TITLE", true);
         String internalId = CrawlerUtils.scrapStringSimpleInfo(document, ".PRODUCT_DETAIL_DESC__sku", true).replaceAll("SKU: ", "");
         String primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, ".iv-image.iv-large-image", "src");
         String internalPid = CommonMethods.getLast(primaryImage.split("="));
         List<String> secondaryImages = getSecondaryImages(document);
         String description = CrawlerUtils.scrapElementsDescription(document, List.of(".PRODUCT_DETAIL_INFO_CONTENT > span"));
         String available = CrawlerUtils.scrapStringSimpleInfo(document, ".button.button--rounded", true);
         Offers offers = available != null && !available.isEmpty() ? scrapOffers(document) : new Offers();
         RatingsReviews ratingsReviews = ratingsReviews(document);

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setName(productName)
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setOffers(offers)
            .setRatingReviews(ratingsReviews)
            .build();
         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document document) {
      return document.selectFirst(".row.PRODUCT_DETAIL") != null;
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
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".PRODUCT_DETAIL_PRICE > #PRODUCT_PRICE_CONTROL", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".PRODUCT_DETAIL_PRICE > small", null, false, ',', session);

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();

   }

   private CreditCards scrapCreditCards(Document doc, Double price) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = scrapInstallments(doc);

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

   public Installments scrapInstallments(Document doc, String selector) throws MalformedPricingException {
      Installments installments = new Installments();

      Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(selector, doc, false);
      if (!pair.isAnyValueNull()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(pair.getFirst())
            .setInstallmentPrice(MathUtils.normalizeTwoDecimalPlaces(((Float) pair.getSecond()).doubleValue()))
            .build());
      }

      return installments;
   }

   public Installments scrapInstallments(Document doc) throws MalformedPricingException {

      Installments installments = scrapInstallments(doc, ".PRODUCT_DETAIL_PRICE_INFO");
      if (installments != null || installments.getInstallments().isEmpty()) {
         return installments;
      }
      return null;
   }

   private List<String> getSecondaryImages(Document doc) {
      List<String> secondaryImages = new ArrayList<>();

      Elements imagesLi = doc.select(".iv-image.iv-large-image");
      for (Element imageLi : imagesLi) {
         secondaryImages.add(imageLi.attr("src"));
      }
      if (secondaryImages.size() > 0) {
         secondaryImages.remove(0);
      }
      return secondaryImages;
   }

   private RatingsReviews ratingsReviews(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      String ratting = CrawlerUtils.scrapStringSimpleInfo(doc,"#PRODUCT_RATING_CONTAINER > div.PRODUCT_RATING_RESUME_CONTAINER > div > div:nth-child(3) > span",true);
      Integer totalNumOfEvaluations = CommonMethods.stringPriceToIntegerPrice(ratting,',', null);
      Integer avgRating = CrawlerUtils.scrapIntegerFromHtml(doc, "#PRODUCT_RATING_CONTAINER > div.PRODUCT_RATING_RESUME_CONTAINER > div > div:nth-child(3) > span", false, null);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating.doubleValue());
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);

      return ratingReviews;
   }
}
