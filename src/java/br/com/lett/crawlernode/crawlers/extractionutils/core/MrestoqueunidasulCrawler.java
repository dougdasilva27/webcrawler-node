package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
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

/**
 * 27/04/2021
 *
 * @author Thainá Aguiar
 */

public abstract class MrestoqueunidasulCrawler extends Crawler {


   private static final String SELLER_FULL_NAME = "Mr Estoque Unidasul";
   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString());


   public MrestoqueunidasulCrawler(Session session) {
      super(session);
   }

   private final String password = getPassword();
   private final String login = getLogin();

   protected String getPassword() {
      return null;
   }

   protected String getLogin() {
      return null;
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      String homePage = "https://www.mrestoque.com.br/";
      return !FILTERS.matcher(href).matches() && (href.startsWith(homePage));
   }

   @Override
   protected Object fetch() {
      if (login == null || password == null) {
         return super.fetch();
      }
      try {
         webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), ProxyCollection.BUY_HAPROXY, session);

         Logging.printLogDebug(logger, session, "waiting product page without login");

         waitForElement(webdriver.driver, ".btn-login");
         WebElement clickLogin = webdriver.driver.findElement(By.cssSelector(".btn-login"));
         webdriver.clickOnElementViaJavascript(clickLogin);

         waitForElement(webdriver.driver, "#username");
         WebElement email = webdriver.driver.findElement(By.cssSelector("#username"));
         email.sendKeys(login);

         waitForElement(webdriver.driver, "#password");
         WebElement pass = webdriver.driver.findElement(By.cssSelector("#password"));
         pass.sendKeys(password);

         Logging.printLogDebug(logger, session, "awaiting login button");
         webdriver.waitLoad(1000);

         waitForElement(webdriver.driver, ".btn.btn-login-ajax");
         WebElement finishLogin = webdriver.driver.findElement(By.cssSelector(".btn.btn-login-ajax"));
         webdriver.clickOnElementViaJavascript(finishLogin);

         Logging.printLogDebug(logger, session, "waiting product page");
         webdriver.waitLoad(2000);

         Document doc = Jsoup.parse(webdriver.getCurrentPageSource());

         if (!isProductPage(doc)) {
            doc = (Document) super.fetch();
         }

         return doc;
      } catch (Exception e) {
         Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
         return super.fetch();
      }
   }

   public static void waitForElement(WebDriver driver, String cssSelector) {
      WebDriverWait wait = new WebDriverWait(driver, 20);
      wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(
            logger, session, "Product page identified: " + session.getOriginalURL());
         String internalId = CommonMethods.getLast(session.getOriginalURL().split("-"));
         String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, ".infos .right .descricao", true);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".tt-product-name", true);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs li:not(:nth-child(2)):not(:first-child) a");
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".thumb.active img", Arrays.asList("src"),
            "https",
            "www.mrestoque.com.br");
         //site hasn't any second image
         String description = scrapDescription(doc);
         boolean available = !doc.select(".add-to-cart").isEmpty();
         Offers offers = available ? scrapOffers(doc) : new Offers();


         // Creating the product
         Product product =
            ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setCategory1(categories.getCategory(0))
               .setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2))
               .setPrimaryImage(primaryImage)
               .setOffers(offers)
               .setDescription(description)
               .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select("#product-detail").isEmpty();
   }

   private String scrapDescription(Document doc) {
      StringBuilder description = new StringBuilder();

      Elements specificationsTittle = doc.select(".technical-specifications dt");
      Elements specificationsValue = doc.select(".technical-specifications dd");

      if (specificationsTittle != null && specificationsValue != null) {
         for (Element specification : specificationsTittle) {
            description.append(specification);
            for (Element specificationValue : specificationsValue) {
               description.append(specificationValue);
               specificationsValue.remove(0);
               break;
            }
         }
      }

      return description.toString();

   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      //Site hasn't any sale

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
      Double spotlightPrice = scrapPrice(doc);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      //Site hasn't any product with old price

      return Pricing.PricingBuilder.create()
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

   private Double scrapPrice(Document doc) {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-detail-message", null, true, ',', session);
      if (spotlightPrice == null) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-price strong", null, true, ',', session);

      }

      return spotlightPrice;
   }


}

