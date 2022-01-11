package br.com.lett.crawlernode.core.fetcher;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import org.apache.poi.ss.formula.functions.T;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * This class encapsulates an instance of a Remote WebDriver that uses a Chromium as backend for
 * Selenium WebDriver. This class also provide methods to manipulate web elements and take web pages
 * screenshots.
 *
 * @author Samir Leao
 */
public class CrawlerWebdriver {

   protected static final Logger logger = LoggerFactory.getLogger(CrawlerWebdriver.class);

   private static final Semaphore SEMAPHORE = new Semaphore(3, true);

   public WebDriver driver;

   private final Session session;

   public CrawlerWebdriver(ChromeOptions caps, Session session) {
      acquireLock();
      driver = new ChromeDriver(caps);
      driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
      driver.manage().window().maximize();
      this.session = session;
   }

   public CrawlerWebdriver(ChromeOptions caps, Session session, Set<Cookie> cookies, String domain) {
      acquireLock();
      driver = new ChromeDriver(caps);
      driver.get(domain);
      for (Cookie cookie : cookies){
         driver.manage().addCookie(cookie);
      }
      driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
      driver.manage().window().maximize();
      this.session = session;
   }


   public void acquireLock() {
      try {
         SEMAPHORE.acquire();
      } catch (InterruptedException e) {
         Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
         Thread.currentThread().interrupt();
      }
   }

   public WebElement findElementByCssSelector(String selector) {
      return driver.findElement(By.cssSelector(selector));
   }

   public List<WebElement> findElementsByCssSelector(String selector) {
      return driver.findElements(By.cssSelector(selector));
   }

   /**
    * Get the html source of the current page loaded in the webdriver.
    */
   public String getCurrentPageSource() {
      return driver.getPageSource();
   }

   /**
    * Loads a webpage without any explicit wait.
    */
   public String loadUrl(String url) {
      driver.get(url);

      return driver.getPageSource();
   }

   public void waitLoad(int timeInMs) {
      try {
         Thread.sleep(timeInMs);
      } catch (InterruptedException e) {
         Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
         Thread.currentThread().interrupt();
      }
   }

   public void waitPageLoad(long timeOutInSeconds) {

      try {
         Wait<WebDriver> wait = new WebDriverWait(driver, timeOutInSeconds);
         wait.until((ExpectedCondition<Boolean>) wd ->
            ((JavascriptExecutor) wd).executeScript("return document.readyState").equals("complete"));
         Logging.printLogDebug(logger,"Page load complete");
      } catch (Exception e) {
         Logging.printLogInfo(logger, "Timeout no carregamento da página após " + timeOutInSeconds + " segundos");
      }

   }

   public WebElement executeJavascript(String javascript) {
      JavascriptExecutor jse = (JavascriptExecutor) driver;
      return (WebElement) jse.executeScript(javascript);
   }

   public void mouseOver(String cssSelector) {
      Actions action = new Actions(driver);
      WebElement we = driver.findElement(By.cssSelector(cssSelector));
      action.moveToElement(we).build().perform();
   }

   public void clickOnElementViaJavascript(WebElement element) {
      JavascriptExecutor jse = (JavascriptExecutor) driver;
      jse.executeScript("arguments[0].click();", element);
   }

   public void clickOnElementViaJavascript(String selector, int waitTime) {
      WebElement element = findElementByCssSelector(selector);
      clickOnElementViaJavascript(element);
      waitLoad(waitTime);
   }

   /**
    * Get the current loaded page on the webdriver instance.
    */
   public String getCurURL() {
      return driver.getCurrentUrl();
   }

   /**
    * Terminate the web driver.
    */
   public void terminate() {
      try {
         driver.close();
         driver.quit();
      } catch (Exception e) {
         Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
      } finally {
         releaseLock();
      }
   }

   public void releaseLock() {
      SEMAPHORE.release();
   }

   public void sendToInput(String selector, String inputText, int waitTime) {
      WebElement input = findElementByCssSelector(selector);
      input.sendKeys(inputText);
      waitLoad(waitTime);
   }

   public void findAndClick(String selector, int waitTime) {
      WebElement el = findElementByCssSelector(selector);
      if (el != null) {
         el.click();
         waitLoad(waitTime);
      }
   }

   public void waitForElement(String cssSelector, int timeOutInSeconds) {
      WebDriverWait wait = new WebDriverWait(driver, timeOutInSeconds);
      wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)));
   }

   public void waitForElement(ExpectedCondition<T> condition, int timeOutInSeconds) {
      WebDriverWait wait = new WebDriverWait(driver, timeOutInSeconds);
      wait.until(condition);
   }
}
