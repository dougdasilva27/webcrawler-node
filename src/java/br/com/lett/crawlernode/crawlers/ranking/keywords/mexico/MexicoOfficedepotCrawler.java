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

public class MexicoOfficedepotCrawler extends CrawlerRankingKeywords {
   private int LAST_PRODUCT_INDEX = 0;
   private final String HOME_PAGE = "https://www.officedepot.com.mx/officedepot/en/";

   public MexicoOfficedepotCrawler(Session session) {
      super(session);
   }

   private Document fetchNextPage() {
      Logging.printLogDebug(logger, session, "fetching next page...");
      webdriver.waitLoad(3000);
      webdriver.waitForElement(".pagination-next > a", 10);
      WebElement button = webdriver.driver.findElement(By.cssSelector(".pagination-next > a"));
      webdriver.clickOnElementViaJavascript(button);
      webdriver.waitLoad(5000);
      webdriver.waitForElement(".product-item", 10);

      return Jsoup.parse(webdriver.getCurrentPageSource());
   }

   @Override
   protected Document fetchDocumentWithWebDriver(String url) {
      List<String> proxies = List.of(ProxyCollection.BUY_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY);
      int attempts = 0;
      Document doc;
      do {
         webdriver = DynamicDataFetcher.fetchPageWebdriver(url, proxies.get(attempts), session);
         webdriver.waitLoad(10000);
         WebElement search = webdriver.driver.findElement(By.cssSelector("#js-site-search-input"));
         search.sendKeys(this.keywordEncoded);
         webdriver.waitLoad(2000);
         WebElement buttonSearch = webdriver.driver.findElement(By.cssSelector(".btn.btn-link.js_search_button"));
         webdriver.clickOnElementViaJavascript(buttonSearch);
         webdriver.waitLoad(5000);
         doc = Jsoup.parse(webdriver.getCurrentPageSource());

      } while (doc == null && attempts++ < 3);

      return doc;
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {

      if (LAST_PRODUCT_INDEX == 0) {
         this.currentDoc = fetchDocumentWithWebDriver(HOME_PAGE);
      } else {
         this.currentDoc = fetchNextPage();
      }
      Elements products = this.currentDoc.select(".product-item");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         for (int i = LAST_PRODUCT_INDEX; i < products.size(); i++) {
            Element product = products.get(i);
            String productName = CrawlerUtils.scrapStringSimpleInfo(product, ".name.description-style > h2", true);
            String productUrl = CrawlerUtils.scrapUrl(product, ".product-description", "href", "https", "www.officedepot.com.mx");
            String imageUrl = CrawlerUtils.scrapUrl(product, ".thumb.center-content-items > img", "data-src", "https", "www.officedepot.com.mx");
            Integer price = getPrice(product);
            String internalPid = CrawlerUtils.scrapStringSimpleInfo(product, ".product-sku > span.name-add.font-medium", false);
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

   private Integer getPrice(Element element) {
      Integer price = null;
      String priceString = CrawlerUtils.scrapStringSimpleInfoByAttribute(element, ".btn-primary-rs", "data-productdiscounted");
      if (priceString != null && !priceString.isEmpty()) {
         price = CommonMethods.stringPriceToIntegerPrice(priceString, '.', null);
      }
      return price;
   }
}
