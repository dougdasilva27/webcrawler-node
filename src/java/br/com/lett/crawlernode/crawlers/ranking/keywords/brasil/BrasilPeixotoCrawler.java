package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class BrasilPeixotoCrawler extends CrawlerRankingKeywords {
   public BrasilPeixotoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {

      String url = "https://www.peixoto.com.br/consulta/?q=" + this.keywordEncoded + "&page=" + this.currentPage;
      this.currentDoc = fetch(url);

      Elements products = this.currentDoc.select(".product_item.logged");
      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            this.totalProducts = products.size();
         }

         for (Element e : products) {
            String productUrl = "https://www.peixoto.com.br/" + CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product_image", "href");
            Integer id = CrawlerUtils.scrapSimpleInteger(e, ".product_name> strong", false);
            String internalId = id != null ? String.valueOf(id) : null;
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product_name> span", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, "a.product_image img", Arrays.asList("src"), "https", "www.peixoto.com.br");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "> div > h3", null, false, ',', session, null);

            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }

      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

   }

   public void getCookiesFromWD(String proxy) {
      try {
         Logging.printLogDebug(logger, session, "Fetching page with webdriver...");

         ChromeOptions options = new ChromeOptions();
         options.addArguments("--window-size=1920,1080");
         options.addArguments("--headless");
         options.addArguments("--no-sandbox");
         options.addArguments("--disable-dev-shm-usage");
         
         webdriver = DynamicDataFetcher.fetchPageWebdriver("https://www.peixoto.com.br/User/Login", proxy, session, this.cookiesWD, "https://www.peixoto.com.br", options);

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

   public static void waitForElement(WebDriver driver, String cssSelector) {
      WebDriverWait wait = new WebDriverWait(driver, 20);
      wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)));
   }

   protected Document fetch(String url) {
      List<String> proxies = Arrays.asList(ProxyCollection.LUMINATI_SERVER_BR_HAPROXY, ProxyCollection.BUY_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY);

      int attemp = 0;

      while (this.cookies.isEmpty() && attemp < 3) {
         getCookiesFromWD(proxies.get(attemp));
         attemp++;
      }

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(this.cookies)
         .setSendUserAgent(false)
         .build();

      return Jsoup.parse(this.dataFetcher.get(session, request).getBody());
   }

}
