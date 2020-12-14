package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

public class BrasilMartinsCrawler extends Crawler {

   public BrasilMartinsCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
   }

   private String password = getPassword();
   private String login = getLogin();

   protected String getPassword() {
      return null;
   }

   protected String getLogin() {
      return null;
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      String homePage = "https://www.martinsatacado.com.br";
      return !FILTERS.matcher(href).matches() && (href.startsWith(homePage));
   }

   @Override
   protected Object fetch() {
      if (login == null || password == null) {
         return super.fetch();
      }
      try {
         webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), ProxyCollection.LUMINATI_SERVER_BR_HAPROXY, false, session);

         Logging.printLogDebug(logger, session, "awaiting product page without login");

         webdriver.waitLoad(25000);
         webdriver.executeJavascript("$('#go-login').mouseover()");

         webdriver.waitLoad(2000);
         waitForElement(webdriver.driver, "#j_username");
         WebElement email = webdriver.driver.findElement(By.cssSelector("#j_username"));
         email.sendKeys(login);

         webdriver.waitLoad(2000);
         waitForElement(webdriver.driver, "#selectCNPJ");
         WebElement cnpj = webdriver.driver.findElement(By.cssSelector("#selectCNPJ"));
         webdriver.clickOnElementViaJavascript(cnpj);

         webdriver.waitLoad(2000);
         waitForElement(webdriver.driver, "#j_password");
         WebElement pass = webdriver.driver.findElement(By.cssSelector("#j_password"));
         pass.sendKeys(password);

         Logging.printLogDebug(logger, session, "awaiting login button");
         webdriver.waitLoad(10000);

         waitForElement(webdriver.driver, ".pt-btn-login");
         WebElement login = webdriver.driver.findElement(By.cssSelector(".pt-btn-login"));
         webdriver.clickOnElementViaJavascript(login);

         Logging.printLogDebug(logger, session, "awaiting product page");
         webdriver.waitLoad(6000);

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

         String internalId =
               CommonMethods.getLast(
                     CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input#id", "value").split("_"));
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".qdDetails .title", true);
         Float price =
               CrawlerUtils.scrapFloatPriceFromHtml(doc, ".qdValue .value", null, true, ',', session);
         Prices prices = scrapPrices(price);
         CategoryCollection categories =
               CrawlerUtils.crawlCategories(doc, ".breadcrumb li:not(:first-child) > a", true);
         String primaryImage =
               CrawlerUtils.scrapSimplePrimaryImage(
                     doc,
                     ".imagePrincipal img",
                     Collections.singletonList("src"),
                     "https",
                     "imgprd.martins.com.br");
         String secondaryImages =
               CrawlerUtils.scrapSimpleSecondaryImages(
                     doc,
                     ".galeryImages img",
                     Collections.singletonList("src"),
                     "https",
                     "imgprd.martins.com.br",
                     primaryImage);
         String description =
               CrawlerUtils.scrapSimpleDescription(
                     doc,
                     Arrays.asList(".qdDetails .cods", ".details", "#especfication", ".body #details"));
         List<String> eans =
               Collections.singletonList(
                     CrawlerUtils.scrapStringSimpleInfo(doc, ".cods .col-2 p", true));
         RatingsReviews ratingsReviews = scrapRating(doc, internalId);

         // Creating the product
         Product product =
               ProductBuilder.create()
                     .setUrl(session.getOriginalURL())
                     .setInternalId(internalId)
                     .setName(name)
                     .setPrice(price)
                     .setPrices(prices)
                     .setAvailable(price != null)
                     .setCategory1(categories.getCategory(0))
                     .setCategory2(categories.getCategory(1))
                     .setCategory3(categories.getCategory(2))
                     .setPrimaryImage(primaryImage)
                     .setRatingReviews(ratingsReviews)
                     .setSecondaryImages(secondaryImages)
                     .setDescription(description)
                     .setMarketplace(new Marketplace())
                     .setEans(eans)
                     .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
      }

      return products;
   }

   private RatingsReviews scrapRating(Document doc, String internalId) {
      RatingsReviews ratingsReviews = new RatingsReviews();
      String ratingString =
            CrawlerUtils.scrapStringSimpleInfoByAttribute(
                  doc, ".hidden-sm .rating .rating-stars", "data-rating");
      JSONObject jsonRating = JSONUtils.stringToJson(ratingString);

      double avgRating = jsonRating.opt("rating") != null ? jsonRating.optInt("rating") : 0;
      int totalReviews = CrawlerUtils.scrapIntegerFromHtml(doc, ".box-review > div:nth-child(3) u", true, 0);

      ratingsReviews.setTotalRating(totalReviews);
      ratingsReviews.setTotalWrittenReviews(totalReviews);
      ratingsReviews.setAverageOverallRating(avgRating);
      ratingsReviews.setDate(session.getDate());
      ratingsReviews.setInternalId(internalId);
      return ratingsReviews;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select("input#id").isEmpty();
   }

   private Prices scrapPrices(Float price) {
      Prices prices = new Prices();

      if (price != null) {
         Map<Integer, Float> installment = new HashMap<>();
         installment.put(1, price);

         prices.setBankTicketPrice(price);
         prices.insertCardInstallment(Card.VISA.toString(), installment);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), installment);
      }

      return prices;
   }
}
