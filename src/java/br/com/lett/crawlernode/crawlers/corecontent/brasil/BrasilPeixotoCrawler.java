package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Parser;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class BrasilPeixotoCrawler extends Crawler {

   public BrasilPeixotoCrawler(Session session) {
      super(session);
      config.setFetcher(FetchMode.JSOUP);
      config.setParser(Parser.HTML);
   }

   public void getCookiesFromWD(String proxy) {
      try {
         Logging.printLogDebug(logger, session, "Fetching page with webdriver...");

         ChromeOptions chromeOptions = new ChromeOptions();
         chromeOptions.addArguments("--window-size=1920,1080");
         chromeOptions.addArguments("--headless");
         chromeOptions.addArguments("--no-sandbox");
         chromeOptions.addArguments("--disable-dev-shm-usage");

         webdriver = DynamicDataFetcher.fetchPageWebdriver("https://www.peixoto.com.br/User/Login", proxy, session, this.cookiesWD, "https://www.peixoto.com.br", chromeOptions);

         webdriver.waitLoad(10000);

         waitForElement(webdriver.driver, "#login_username");
         WebElement username = webdriver.driver.findElement(By.cssSelector("#login_username"));
         username.sendKeys(session.getOptions().optString("user"));

         webdriver.waitLoad(2000);
         waitForElement(webdriver.driver, "#login_password");
         WebElement pass = webdriver.driver.findElement(By.cssSelector("#login_password"));
         pass.sendKeys(session.getOptions().optString("pass"));

         waitForElement(webdriver.driver, ".button.submit");
         webdriver.findAndClick(".button.submit", 15000);


         waitForElement(webdriver.driver, ".account-link.trocar-filial");
         webdriver.findAndClick(".account-link.trocar-filial", 15000);

         waitForElement(webdriver.driver, "#popup_content .table-scrollable .row0.first.gradeX.odd .modal-window.blue");
         webdriver.findAndClick("#popup_content .table-scrollable .row0.first.gradeX.odd .modal-window.blue", 15000);

         waitForElement(webdriver.driver, "#popup_content .table-scrollable .row0.first.gradeX.odd  .enviar.blue");
         webdriver.findAndClick("#popup_content .table-scrollable .row0.first.gradeX.odd  .enviar.blue", 15000);

         Set<Cookie> cookiesResponse = webdriver.driver.manage().getCookies();

         for (Cookie cookie : cookiesResponse) {
            BasicClientCookie basicClientCookie = new BasicClientCookie(cookie.getName(), cookie.getValue());
            basicClientCookie.setDomain(cookie.getDomain());
            basicClientCookie.setPath(cookie.getPath());
            basicClientCookie.setExpiryDate(cookie.getExpiry());
            this.cookies.add(basicClientCookie);
         }
         webdriver.terminate();

      } catch (Exception e) {
         Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
         webdriver.terminate();

         Logging.printLogWarn(logger, "login não realizado");
      }
   }

   @Override
   protected Response fetchResponse() {

      List<String> proxies = Arrays.asList(ProxyCollection.LUMINATI_SERVER_BR_HAPROXY, ProxyCollection.BUY_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY);

      int attemp = 0;

      while (this.cookies.isEmpty() && attemp < 3) {
         getCookiesFromWD(proxies.get(attemp));
         attemp++;
      }

      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setCookies(cookies)
         .build();

      return this.dataFetcher.get(session, request);

   }

   public static void waitForElement(WebDriver driver, String cssSelector) {
      WebDriverWait wait = new WebDriverWait(driver, 20);
      wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      JSONArray arr = new JSONArray();
      List<Product> products = new ArrayList<>();
      String script = CrawlerUtils.scrapScriptFromHtml(doc, "head > script:nth-child(5n-1)");
      if(script != null){
         script = script.replaceAll("window.data_layer = true;", "");
         script = script.replaceAll("dataLayer = ", "");
         script = script.replaceAll("\r", "");
         script = script.replaceAll("\n", "");
         script = script.replaceAll("\t", "");
         script = script.substring(1, script.length() - 2);
         arr = JSONUtils.stringToJsonArray(script);
      }
      if (arr.length() > 0) {
         JSONObject data = (JSONObject) arr.get(0);
         String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".product_code span", true);
         Integer id = JSONUtils.getValueRecursive(data, "transactionProducts.0.id", Integer.class);
         String internalPid = id != null ? id.toString() : null;
         String name = JSONUtils.getValueRecursive(data, "transactionProducts.0.name", String.class);
         String primaryImage = JSONUtils.getValueRecursive(data, "transactionProducts.0.fullImage", String.class);
         String description = JSONUtils.getValueRecursive(data, "transactionProducts.0.description", String.class);
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".thumbnails .list a", Arrays.asList("data-src"), "https", "https://www.peixoto.com.br/", primaryImage);
         Boolean stock = JSONUtils.getValueRecursive(data, "transactionProducts.0.available", Boolean.class);
         List<String> eans = Arrays.asList(JSONUtils.getValueRecursive(data, "transactionProducts.0.sku", String.class));
         Offers offers = stock ? scrapOffers(data) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setOffers(offers)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setEans(eans)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page:   " + this.session.getOriginalURL());
      }
      return products;
   }

   private Offers scrapOffers(JSONObject data) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();

      Pricing pricing = scrapPricing(data);
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSales(sales)
         .setSellerFullName("peixoto")
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject data) throws MalformedPricingException {

      Double spotlightPrice = JSONUtils.getValueRecursive(data, "transactionProducts.0.fullPrice", Double.class);

      Double priceFrom = spotlightPrice;

      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
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

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString());

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
