package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

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

public class MexicoMultiherramientasCrawler extends CrawlerRankingKeywords {
   private int LAST_PRODUCT_INDEX = 0;

   public MexicoMultiherramientasCrawler(Session session) {
      super(session);
   }

   @Override
   protected Document fetchDocumentWithWebDriver(String url) {
      Document doc = null;
      List<String> proxies = List.of(ProxyCollection.BUY_HAPROXY, ProxyCollection.SMART_PROXY_MX_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY);
      int attempts = 0;
      do {
         try {
            webdriver = DynamicDataFetcher.fetchPageWebdriver(url, proxies.get(attempts), session);
            waitForElement(webdriver.driver, "div[class=\"adv-product produc \"]");
            doc = Jsoup.parse(webdriver.getCurrentPageSource());
         } catch (Exception e) {
            Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
         }
      }
      while (doc == null && attempts++ < (proxies.size() - 1));
      return doc;
   }

   public static void waitForElement(WebDriver driver, String cssSelector) {
      WebDriverWait wait = new WebDriverWait(driver, 15);
      wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)));
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 25;
      String url = "https://multiherramientas.mx/catalogo.php?cat=999&text=" + keywordEncoded.replace(" ", "%20");
      if (LAST_PRODUCT_INDEX == 0) {
         this.currentDoc = fetchDocumentWithWebDriver(url);
      } else {
         this.currentDoc = fetchNextPage();
      }
      Elements products = this.currentDoc.select("div[class=\"adv-product produc \"]");

      if (products != null && !products.isEmpty()) {
         for (int i = LAST_PRODUCT_INDEX; i < products.size(); i++) {
            Element product = products.get(i);
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".options > button", "data-fkproduct");
            String productName = CrawlerUtils.scrapStringSimpleInfo(product, "div > span.label > a", true);
            String productUrl = CrawlerUtils.scrapUrl(product, "div > span.label > a", "href", "https", "multiherramientas.mx");
            String imageUrl = getImageUrl(product);
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".price > a > span.final", null, false, '.', session, null);
            boolean available = price > 0;

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
      return !this.currentDoc.select("#btn-showmore > button").isEmpty();
   }

   private String getImageUrl(Element element) {
      String imageUrl = null;
      String[] arrayImage;
      String selectorImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(element, "div[class=\"adv-product produc \"]  > figure.text-center", "style");
      if (selectorImage != null && !selectorImage.isEmpty()) {
         arrayImage = selectorImage.split(" ");
         if (arrayImage.length > 1 && arrayImage != null) {
            imageUrl = arrayImage[1].replace("url(/", "").replace(")", "");
         }
      }
      return CrawlerUtils.completeUrl(imageUrl, "https", "multiherramientas.mx");
   }

   private Document fetchNextPage() {
      Logging.printLogDebug(logger, session, "fetching next page...");
      webdriver.waitLoad(3000);
      webdriver.waitForElement("#btn-showmore > button", 5);
      WebElement button = webdriver.driver.findElement(By.cssSelector("#btn-showmore > button"));
      webdriver.clickOnElementViaJavascript(button);
      webdriver.waitLoad(8000);

      return Jsoup.parse(webdriver.getCurrentPageSource());
   }
}
