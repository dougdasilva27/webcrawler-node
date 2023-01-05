package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
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
import java.util.List;

public class BrasilMercatoemcasaCrawler extends CrawlerRankingKeywords {

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
         cep.sendKeys("51160-035");
         waitForElement(webdriver.driver, "button[onclick=\"setCepClickHandler();\"]");
         WebElement send = webdriver.driver.findElement(By.cssSelector("button[onclick=\"setCepClickHandler();\"]"));
         webdriver.clickOnElementViaJavascript(send);
         webdriver.waitLoad(10000);
         webdriver.loadUrl(HOME_PAGE + "produtos?search=" + this.keywordEncoded);
         webdriver.waitLoad(10000);
         doc = Jsoup.parse(webdriver.getCurrentPageSource());
      }
      while (doc == null && attempts++ < 3);
      waitForElement(webdriver.driver,".PRODUCT_ITEM");
      return doc;
   }

   public static void waitForElement(WebDriver driver, String cssSelector) {
      WebDriverWait wait = new WebDriverWait(driver, 10);
      wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)));
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.currentDoc = fetchDocumentWithWebDriver(HOME_PAGE);
      Elements products = this.currentDoc.select(".PRODUCT_ITEM");

      if (!products.isEmpty()) {
         for (Element product : products) {
            String productName = CrawlerUtils.scrapStringSimpleInfo(product, "div > a > h4", true);
            String productUrl = CrawlerUtils.scrapUrl(product, ".PRODUCT_ITEM > a", "href", "https", "www.mercatoemcasa.com.br");
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".PRODUCT_IMAGE_CONTAINER > img", "src");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".PRODUCT_PRICE > #PRODUCT_PRICE_CONTROL", null, false, ',', session, null);
            String internalPid = CommonMethods.getLast(imageUrl.split("="));
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalPid(internalPid)
               .setName(productName)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
               .build();

            saveDataProduct(productRanking);
            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
      this.log("Finalizando Crawler de produtos da página: " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private Document fetchNextPage(){
      Logging.printLogDebug(logger, session, "fetching next page...");
      WebElement button = webdriver.driver.findElement(By.cssSelector(".INCREASE_BUTTON > button"));
      webdriver.clickOnElementViaJavascript(button);
      webdriver.waitLoad(8000);

      return Jsoup.parse(webdriver.getCurrentPageSource());
   }

}
