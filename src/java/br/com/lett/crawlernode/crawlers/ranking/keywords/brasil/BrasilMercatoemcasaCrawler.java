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
import org.apache.http.HttpHeaders;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static br.com.lett.crawlernode.util.CrawlerUtils.getRedirectedUrl;

public class BrasilMercatoemcasaCrawler extends CrawlerRankingKeywords {

   private int LAST_PRODUCT_INDEX = 0;

   private final String HOME_PAGE = "https://www.mercatoemcasa.com.br/";

   private String getCep() {
      return session.getOptions().optString("cep");
   }

   public BrasilMercatoemcasaCrawler(Session session) {
      super(session);
   }

   @Override
   protected Document fetchDocumentWithWebDriver(String url) {
      Document doc;
      List<String> proxies = List.of(ProxyCollection.BUY_HAPROXY, ProxyCollection.SMART_PROXY_BR_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY);
      int attempts = 0;
      do {
         webdriver = DynamicDataFetcher.fetchPageWebdriver(url, proxies.get(attempts), session);
         waitForElement(webdriver.driver, ".ACTION > .SECONDARY");
         WebElement modal = webdriver.driver.findElement(By.cssSelector(".ACTION > .SECONDARY"));
         webdriver.clickOnElementViaJavascript(modal);
         webdriver.waitLoad(10000);

         waitForElement(webdriver.driver, "#cep-eccomerce-header > a");
         WebElement changeCep = webdriver.driver.findElement(By.cssSelector("#cep-eccomerce-header > a"));
         webdriver.clickOnElementViaJavascript(changeCep);
         webdriver.waitLoad(10000);

         waitForElement(webdriver.driver, "#cep");
         WebElement cep = webdriver.driver.findElement(By.cssSelector("#cep"));
         cep.sendKeys(getCep());
         waitForElement(webdriver.driver, "button[onclick=\"setCepClickHandler();\"]");
         WebElement send = webdriver.driver.findElement(By.cssSelector("button[onclick=\"setCepClickHandler();\"]"));
         webdriver.clickOnElementViaJavascript(send);
         webdriver.waitLoad(10000);
         webdriver.loadUrl(HOME_PAGE + "produtos?search=" + this.keywordEncoded);
         webdriver.waitLoad(10000);
         doc = Jsoup.parse(webdriver.getCurrentPageSource());
      }
      while (doc == null && attempts++ < 3);
      waitForElement(webdriver.driver, ".PRODUCT_ITEM");
      return doc;
   }

   public static void waitForElement(WebDriver driver, String cssSelector) {
      WebDriverWait wait = new WebDriverWait(driver, 10);
      wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)));
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String url = HOME_PAGE + "produtos?search=" + keywordEncoded;
      if (LAST_PRODUCT_INDEX == 0) {
         this.currentDoc = fetchDocumentWithWebDriver(url);
      } else {
         this.currentDoc = fetchNextPage();
      }
      Elements products = this.currentDoc.select(".PRODUCT_ITEM");

      if (products != null && !products.isEmpty()) {
         for (int i = LAST_PRODUCT_INDEX; i < products.size(); i++) {
            Element product = products.get(i);
            String productName = CrawlerUtils.scrapStringSimpleInfo(product, "div > a > h4", true);
            String productUrl = CrawlerUtils.scrapUrl(product, ".PRODUCT_ITEM > a", "href", "https", "www.mercatoemcasa.com.br");
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".PRODUCT_IMAGE_CONTAINER > img", "src");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".PRODUCT_PRICE > #PRODUCT_PRICE_CONTROL", null, false, ',', session, null);
            String internalPid = getInternalPid(product);
            String notAvailable = CrawlerUtils.scrapStringSimpleInfo(product, ".PRODUCT_CONTROLS > p", true);
            boolean available = notAvailable != null && !notAvailable.isEmpty() ? false : true;
            if (!available) {
               price = null;
            }

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalPid(internalPid)
               .setName(productName)
               .setPriceInCents(price)
               .setAvailability(available)
               .setImageUrl(imageUrl)
               .build();

            saveDataProduct(productRanking);
            LAST_PRODUCT_INDEX++;
            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
      this.log("Finalizando Crawler de produtos da página: " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select(".INCREASE_BUTTON > button").isEmpty();
   }

   private Document fetchNextPage() {
      Logging.printLogDebug(logger, session, "fetching next page...");
      webdriver.waitLoad(3000);
      webdriver.waitForElement(".INCREASE_BUTTON > button", 5);
      WebElement button = webdriver.driver.findElement(By.cssSelector(".INCREASE_BUTTON > button"));
      webdriver.clickOnElementViaJavascript(button);
      webdriver.waitLoad(8000);

      return Jsoup.parse(webdriver.getCurrentPageSource());
   }

   private String getInternalPid(Element element) {
      String[] arrayString;
      String internalPid = null;
      Document doc;
      String extractString = CrawlerUtils.scrapStringSimpleInfoByAttribute(element, ".row.qtdBox > div.col-4", "onclick");
      if (extractString != null && !extractString.isEmpty()) {
         arrayString = extractString.split(";");
         if (arrayString.length > 1 && arrayString[1] != null && !arrayString[1].isEmpty()) {
            arrayString[1].split("'\\);");
            internalPid = arrayString[1];
            return internalPid.replace("addCartDataLayer('", "").replace("' , false)", "");
         }
      }
      if (internalPid == null) {
         String productUrl = CrawlerUtils.scrapUrl(element, ".PRODUCT_ITEM > a", "href", "https", "www.mercatoemcasa.com.br");
         Document extractCore = extractInternalPidByCore(productUrl);
//         do {
//            String productUrl = CrawlerUtils.scrapUrl(element, ".PRODUCT_ITEM > a", "href", "https", "www.mercatoemcasa.com.br");
//            webdriver = DynamicDataFetcher.fetchPageWebdriver(productUrl, ProxyCollection.BUY_HAPROXY, session);
//            waitForElement(webdriver.driver, ".ACTION > .SECONDARY");
//            WebElement modal = webdriver.driver.findElement(By.cssSelector(".ACTION > .SECONDARY"));
//            webdriver.clickOnElementViaJavascript(modal);
//            webdriver.waitLoad(10000);
//            Element extractCore = element.selectFirst(productUrl).html(this.session.getOriginalURL());
//            extractString = CrawlerUtils.scrapStringSimpleInfoByAttribute(extractCore, "div.col-4.clear-h.cep-button > button", "onclick");
//            if (extractString != null && !extractString.isEmpty()) {
//               arrayString = extractString.split("'");
//               if (arrayString.length > 1 && arrayString[1] != null && !arrayString[1].isEmpty()) {
//                  arrayString[1].split("'\\);");
//                  internalPid = arrayString[1];
//                  return internalPid;
//               }
//            }
//            doc = Jsoup.parse(webdriver.getCurrentPageSource());
//         } while (doc == null);
      }
      return null;
   }

   private Document extractInternalPidByCore(String url){
      String payload = "[{\"action\":\"SeuMercatoController\",\"method\":\"loadCatalogData\",\"data\":[\"Mercato Em Casa\",\"51160-035\",\"\",\"\",null],\"type\":\"rpc\",\"tid\":2,\"ctx\":{\"csrf\":\"VmpFPSxNakF5TXkwd01TMHhORlF4TkRveU1qbzFNaTQwTnpaYSxLRG92cVVNN3JDUnEwRXJGRENLVXFBLE5XRXhNR00z\",\"vid\":\"0664A000003FXkl\",\"ns\":\"\",\"ver\":48,\"authorization\":\"eyJub25jZSI6ImlFbUhKaUczRmNtQWFFd2E4WFB3UGo1SmtmXzBobUkzcVIxQWZHNW5WQlFcdTAwM2QiLCJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6IntcInRcIjpcIjAwRDQxMDAwMDAxMlRKYVwiLFwidlwiOlwiMDJHNEEwMDAwMDA0bDAzXCIsXCJhXCI6XCJ2ZnJlbW90aW5nc2lnbmluZ2tleVwiLFwidVwiOlwiMDA1NEEwMDAwMDlPQkh1XCJ9IiwiY3JpdCI6WyJpYXQiXSwiaWF0IjoxNjczNDQ2OTcyNDc3LCJleHAiOjB9.Q2lSVFpYVk5aWEpqWVhSdlEyOXVkSEp2Ykd4bGNpNXNiMkZrUTJGMFlXeHZaMFJoZEdFPQ==.wjZiuX89RStZM7nR5IIU7iNW5UFqh31E_AMgJa_OqIQ=\"}},{\"action\":\"SeuMercatoController\",\"method\":\"loadCatalogDataMin\",\"data\":[\"51160-035\",null],\"type\":\"rpc\",\"tid\":3,\"ctx\":{\"csrf\":\"VmpFPSxNakF5TXkwd01TMHhORlF4TkRveU1qbzFNaTQwTnpkYSxac3JLM3FsU1Y0Z0RIbGFsSzhmRm82LE1EYzVNemxq\",\"vid\":\"0664A000003FXkl\",\"ns\":\"\",\"ver\":48,\"authorization\":\"eyJub25jZSI6IklOcWY4cGdDeXYwYmltaXBDVC00dXZ6SXhRc0h3SFE2dV9uWG9TeHJFX1FcdTAwM2QiLCJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6IntcInRcIjpcIjAwRDQxMDAwMDAxMlRKYVwiLFwidlwiOlwiMDJHNEEwMDAwMDA0bDAzXCIsXCJhXCI6XCJ2ZnJlbW90aW5nc2lnbmluZ2tleVwiLFwidVwiOlwiMDA1NEEwMDAwMDlPQkh1XCJ9IiwiY3JpdCI6WyJpYXQiXSwiaWF0IjoxNjczNDQ2OTcyNDc4LCJleHAiOjB9.Q2lkVFpYVk5aWEpqWVhSdlEyOXVkSEp2Ykd4bGNpNXNiMkZrUTJGMFlXeHZaMFJoZEdGTmFXND0=.y1iQnB0qr30eDg34mmSELHTwO5Ng3LA-mSsklGs8KE8=\"}}]";
      Map<String,String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE,"application/json");
      headers.put("referer",this.session.getOriginalURL());
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setPayload(payload)
         .setHeaders(headers)
         .setProxyservice(List.of(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
            ProxyCollection.SMART_PROXY_BR_HAPROXY))
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(this.dataFetcher, new ApacheDataFetcher(), new FetcherDataFetcher(), new JsoupDataFetcher()), session, "get");
      return Jsoup.parse(response.getBody());
   }
}
