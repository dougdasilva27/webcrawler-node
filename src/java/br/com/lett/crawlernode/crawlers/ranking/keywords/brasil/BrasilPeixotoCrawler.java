package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
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
import java.util.*;

public class BrasilPeixotoCrawler extends CrawlerRankingKeywords {
   public BrasilPeixotoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {

      String url = "https://www.peixoto.com.br/catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordEncoded.replace(" ", "+");
      this.currentDoc = fetch(url);

      Elements products = this.currentDoc.select(".products .product-item");
      if (!products.isEmpty()) {

         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-item-link", "href");

            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div.price-final_price", "data-product-id");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product-item-link", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".main-image img", Arrays.asList("src"), "https", "www.peixoto.com.br");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "span.price", null, false, ',', session, null);

            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(null)
               .setInternalPid(internalPid)
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

   public void getCookiesFromWD() {
      List<String> proxies = Arrays.asList(ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY, ProxyCollection.BUY_HAPROXY, ProxyCollection.SMART_PROXY_BR_HAPROXY, ProxyCollection.LUMINATI_SERVER_BR_HAPROXY);

      int attempt = 0;
      do {
         try {
            Logging.printLogDebug(logger, session, "Fetching page with webdriver...");

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--headless");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");

            webdriver = DynamicDataFetcher.fetchPageWebdriver("https://www.peixoto.com.br/customer/account/login/", proxies.get(attempt), session);

            webdriver.waitLoad(1000);

            waitForElement(webdriver.driver, ".page-main #email");
            WebElement username = webdriver.driver.findElement(By.cssSelector(".page-main #email"));
            username.sendKeys(session.getOptions().optString("user"));

            webdriver.waitLoad(2000);
            waitForElement(webdriver.driver, ".page-main #pass");
            WebElement pass = webdriver.driver.findElement(By.cssSelector(".page-main #pass"));
            pass.sendKeys(session.getOptions().optString("pass"));

            waitForElement(webdriver.driver, ".page-main button.login");
            webdriver.findAndClick(".page-main button.login", 10000);

            //chose catalão - GO = 5
            waitForElement(webdriver.driver, "#branch-select option[value='5']");
            webdriver.findAndClick("#branch-select option[value='5']", 10000);

            waitForElement(webdriver.driver, "button.b2b-choices");
            webdriver.findAndClick("button.b2b-choices", 10000);

            Set<Cookie> cookiesResponse = webdriver.driver.manage().getCookies();

            for (Cookie cookie : cookiesResponse) {
               BasicClientCookie basicClientCookie = new BasicClientCookie(cookie.getName(), cookie.getValue());
               basicClientCookie.setDomain(cookie.getDomain());
               basicClientCookie.setPath(cookie.getPath());
               basicClientCookie.setExpiryDate(cookie.getExpiry());
               this.cookies.add(basicClientCookie);
            }

         } catch (Exception e) {
            Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));

            Logging.printLogWarn(logger, "login não realizado");
         }
      } while (this.cookies.isEmpty() && attempt++ < proxies.size());

   }

   public static void waitForElement(WebDriver driver, String cssSelector) {
      WebDriverWait wait = new WebDriverWait(driver, 10);
      wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)));
   }

   protected Document fetch(String url) {
      List<String> proxies = Arrays.asList(ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY, ProxyCollection.BUY_HAPROXY, ProxyCollection.SMART_PROXY_BR_HAPROXY, ProxyCollection.LUMINATI_SERVER_BR_HAPROXY);
      getCookiesFromWD();
      Map<String, String> headers = new HashMap<>();
      headers.put("authority", "www.peixoto.com.br");
      headers.put("accept-language", "en-US,en;q=0.9,pt;q=0.8,pt-PT;q=0.7");
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(this.cookies)
         .setHeaders(headers)
         .setProxyservice(proxies)
         .setSendUserAgent(false)
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session);

      return Jsoup.parse(response.getBody());
   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select(".pages .item.pages-item-next .action.next").isEmpty();
   }
}
