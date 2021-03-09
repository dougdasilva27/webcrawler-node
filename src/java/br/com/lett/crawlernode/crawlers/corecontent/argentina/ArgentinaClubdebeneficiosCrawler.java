package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class ArgentinaClubdebeneficiosCrawler extends Crawler {

   public ArgentinaClubdebeneficiosCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
   }

   private String password = "meg3BRAP*dih5huh";
   private String username = "datacapture@lett.digital";
   private String homePage = "https://clubdebeneficios.com/usuario/ingresar";

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      String homePage = "https://clubdebeneficios.com/";
      return !FILTERS.matcher(href).matches() && (href.startsWith(homePage));
   }

   @Override
   protected Object fetch() {
      if (username == null || password == null) {
         return super.fetch();
      }
      try {
         webdriver = DynamicDataFetcher.fetchPageWebdriver(homePage, ProxyCollection.LUMINATI_SERVER_BR_HAPROXY, session);

         Logging.printLogDebug(logger, session, "awaiting product page without login");

         waitForElement(webdriver.driver, "button.action-close");
         WebElement closePopUp = webdriver.driver.findElement(By.cssSelector("button.action-close"));
         webdriver.clickOnElementViaJavascript(closePopUp);

         webdriver.waitLoad(2000);
         waitForElement(webdriver.driver, "input[id=customer-email]");
         WebElement email = webdriver.driver.findElement(By.cssSelector("input[id=customer-email]"));
         email.sendKeys(username);

         webdriver.waitLoad(2000);
         waitForElement(webdriver.driver, "input[id=pass]");
         WebElement pass = webdriver.driver.findElement(By.cssSelector("input[id=pass]"));
         pass.sendKeys(password);

         Logging.printLogDebug(logger, session, "awaiting login button");
         webdriver.waitLoad(10000);

         waitForElement(webdriver.driver, "button[id=send2]");
         WebElement login = webdriver.driver.findElement(By.cssSelector("button[id=send2]"));
         webdriver.clickOnElementViaJavascript(login);

         Logging.printLogDebug(logger, session, "awaiting product page");
         webdriver.waitLoad(6000);

         Document doc = Jsoup.parse(webdriver.getCurrentPageSource());

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
}
