package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

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
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MexicoSingerCrawler extends CrawlerRankingKeywords {
   public MexicoSingerCrawler(Session session) {
      super(session);
   }

   protected Document fetch() {
      Document doc = null;

      try {
         List<String> proxies = Arrays.asList(ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY, ProxyCollection.BUY_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_MX);
         int attemp = 0;
         do {
            if (attemp != 0) {
               webdriver.terminate();
            }
            webdriver = DynamicDataFetcher
               .fetchPageWebdriver("https://www.singer.com/mx/search?title=" + this.keywordEncoded.replace(" ", "+") + "&page=" + (currentPage - 1), proxies.get(attemp), session);
            webdriver.waitLoad(10000);
            doc = Jsoup.parse(webdriver.getCurrentPageSource());
         } while (doc.select(".card-text.plp-section-content > strong.plp-section-price.current").isEmpty() && attemp++ < 3);

         webdriver.waitLoad(2000);
         if (doc.selectFirst(".card-text.plp-section-content > strong.plp-section-price.current") != null) {
            WebElement finalScearch = webdriver.driver.findElement(By.cssSelector(".card-text.plp-section-content > strong.plp-section-price.current"));
            webdriver.clickOnElementViaJavascript(finalScearch);
            waitForElement(webdriver.driver, ".card-text.plp-section-content > strong.plp-section-price.current");
            webdriver.waitLoad(5000);
            doc = Jsoup.parse(webdriver.getCurrentPageSource());

            webdriver.terminate();

         }


      } catch (Exception e) {
         Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
         Logging.printLogWarn(logger, "login não realizado");
      }
      return doc;
   }

   @Override
   public void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 30;

      this.log("Página " + this.currentPage);

      String url = "https://www.singer.com/mx/search?title=" + this.keywordEncoded.replace(" ", "+") + "&page=" + (currentPage - 1);

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetch();

      Elements products = this.currentDoc.select(".views-view-grid > .card-onclick");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".card-image > div.product-quickview > button", "data-ep-reference");
            String productUrl = CrawlerUtils.scrapStringSimpleInfo(e, ".card-body.plp-section-body > p > a", false);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".card-body.plp-section-body > p > a", true);
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".main-wrap.card-category-imagewrapper > div > .card-image > a > img", Arrays.asList("data-imgsrc"), "https", "://www.singer.com/mx");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".card-text.plp-section-content > strong.plp-section-price.current", null, false, '.', session, null);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   public static void waitForElement(WebDriver driver, String cssSelector) {
      WebDriverWait wait = new WebDriverWait(driver, 20);
      wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)));
   }

   @Override
   protected boolean hasNextPage() {
      String holder = CrawlerUtils.scrapStringSimpleInfo(this.currentDoc, ".pagerer-center-pane > nav > ul > li.pager__item.pager__item--next > a > .visually-hidden", false);
      return holder != null && !holder.isEmpty();
   }

   @Override
   protected void setTotalProducts() {
      String totalHolder = CrawlerUtils.scrapStringSimpleInfo(this.currentDoc, "#products-tab > span", false);
      Integer totalProduct = null;
      if (totalHolder != null && !totalHolder.isEmpty()) {
         totalProduct = Integer.parseInt(totalHolder.replace("(", "").replace(")", ""));
      }
      this.totalProducts = totalProduct;
      this.log("Total: " + this.totalProducts);

   }
}
