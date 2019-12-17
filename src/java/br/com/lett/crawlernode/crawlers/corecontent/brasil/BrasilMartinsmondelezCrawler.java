package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

public class BrasilMartinsmondelezCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.martinsatacado.com.br";

   public BrasilMartinsmondelezCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   private static final String EMAIL_LOGIN = "erika.rosa@mdlz.com";
   private static final String PASSWORD = "monica08";


   @Override
   protected Object fetch() {
      try {
         this.webdriver = DynamicDataFetcher.fetchPageWebdriver("https://www.martinsatacado.com.br/login/topo/request", session);
         this.webdriver.waitLoad(5000);

         this.webdriver.clickOnElementViaJavascript(this.webdriver.driver.findElement(By.cssSelector("#go-login")));
         this.webdriver.waitLoad(5000);

         WebElement email = this.webdriver.driver.findElement(By.cssSelector("#js_username_login"));
         email.sendKeys(EMAIL_LOGIN);
         this.webdriver.waitLoad(2000);

         WebElement cnpj = this.webdriver.driver.findElement(By.cssSelector("#jsSelectCNPJ"));
         this.webdriver.clickOnElementViaJavascript(cnpj);
         this.webdriver.waitLoad(2000);

         WebElement pass = this.webdriver.driver.findElement(By.cssSelector("#j_password[required]"));
         pass.sendKeys(PASSWORD);
         this.webdriver.waitLoad(2000);

         WebElement login = this.webdriver.driver.findElement(By.cssSelector(".c-login__button"));
         this.webdriver.clickOnElementViaJavascript(login);
         this.webdriver.waitLoad(6000);

         this.webdriver.loadUrl(session.getOriginalURL());
         this.webdriver.waitLoad(6000);

         return Jsoup.parse(this.webdriver.getCurrentPageSource());
      } catch (Exception e) {
         Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
         return super.fetch();
      }
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CommonMethods.getLast(CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input#id", "value").split("_"));
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".qdDetails .title", true);
         Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".qdValue .value", null, true, ',', session);
         Prices prices = scrapPrices(price);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li:not(:first-child) > a", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".imagePrincipal img", Arrays.asList("src"), "https", "imgprd.martins.com.br");
         String secondaryImages =
               CrawlerUtils.scrapSimpleSecondaryImages(doc, ".galeryImages img", Arrays.asList("src"), "https", "imgprd.martins.com.br", primaryImage);
         String description =
               CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".qdDetails .cods", ".details", "#especfication", ".body #details"));
         List<String> eans = Arrays.asList(CrawlerUtils.scrapStringSimpleInfo(doc, ".cods .col-2 p", true));

         // Creating the product
         Product product = ProductBuilder.create()
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
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setMarketplace(new Marketplace())
               .setEans(eans).build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
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
