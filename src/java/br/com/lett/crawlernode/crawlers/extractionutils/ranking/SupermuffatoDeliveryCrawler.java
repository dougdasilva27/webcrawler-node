package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

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

public class SupermuffatoDeliveryCrawler extends CrawlerRankingKeywords {

   private static final String BASE_URL = "delivery.supermuffato.com.br/";
   private String HOME_PAGE = "https://www.supermuffato.com.br/";
   private String vtexSegment = getVtexSegment();

   public SupermuffatoDeliveryCrawler(Session session) {
      super(session);
   }

   protected String getCityCode() {
      return session.getOptions().optString("cityCode");
   }

   public String getVtexSegment() {
      return session.getOptions().optString("vtex_segment", "");
   }

   public static void waitForElement(WebDriver driver, String cssSelector) {
      WebDriverWait wait = new WebDriverWait(driver, 10);
      wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)));
   }

   @Override
   protected Document fetchDocumentWithWebDriver(String url) {
      String city = getCityCode();
      Document doc = null;

      int attempts = 0;

      List<String> proxies = List.of(ProxyCollection.BUY_HAPROXY, ProxyCollection.LUMINATI_SERVER_BR_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_ANY_HAPROXY);
      do {

         try {
            webdriver = DynamicDataFetcher.fetchPageWebdriver(url, proxies.get(attempts), session);

            waitForElement(webdriver.driver, ".form-control optgroup option[value='" + city + "']");
            webdriver.findAndClick(".form-control optgroup option[value='" + city + "']", 30000);

            waitForElement(webdriver.driver, "#s-ch-change-channel");
            webdriver.findAndClick("#s-ch-change-channel", 30000);

            waitForElement(webdriver.driver, ".fulltext-search-box.ui-autocomplete-input");
            WebElement pass = webdriver.driver.findElement(By.cssSelector(".fulltext-search-box.ui-autocomplete-input"));
            pass.sendKeys(this.keywordEncoded);

            waitForElement(webdriver.driver, ".icon.icon-search");
            webdriver.findAndClick(".icon.icon-search", 30000);

            webdriver.waitLoad(30000);
            doc = Jsoup.parse(webdriver.getCurrentPageSource());

         } catch (Exception e) {
            Logging.printLogInfo(logger, session, CommonMethods.getStackTrace(e));

         }
      } while (doc == null && attempts++ < 3);
      waitForElement(webdriver.driver, "div.prd-list-item");
      return doc;
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 48;
      this.log("Página " + this.currentPage);
      if (this.currentPage == 1) {
         this.currentDoc = fetchDocumentWithWebDriver(HOME_PAGE);
      } else {
         this.currentDoc = fetchNextPage();
      }
      this.log("Link onde são feitos os crawlers: " + HOME_PAGE);


      Elements products = this.currentDoc.select("div.prd-list-item");

      if (products != null && !products.isEmpty()) {
         for (int i = 0; i < products.size(); i++) {
            Element product = products.get(i);
            String internalPid = product.attr("data-product-id");
            String name = CrawlerUtils.scrapStringSimpleInfo(product, ".prd-list-item-name", true);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "li[layout] .prd-list-item-img img", "src");
            String urlProduct = CrawlerUtils.scrapUrl(product, ".prd-list-item-desc > a", "href", "https", BASE_URL);
            Integer price = crawlPrice(product);
            boolean available = price != null;

            RankingProduct rankingProduct = RankingProductBuilder.create()
               .setName(name)
               .setUrl(urlProduct)
               .setInternalPid(internalPid)
               .setImageUrl(imageUrl)
               .setAvailability(available)
               .setPriceInCents(price)
               .build();

            saveDataProduct(rankingProduct);
            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
      this.log("Finalizando Crawler de produtos da página: " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private Integer crawlPrice(Element product) {
      Integer price = null;
      Double priceDoub = CrawlerUtils.scrapDoublePriceFromHtml(product, ".prd-list-item-price-sell", null, true, ',', session);
      if (priceDoub != null) {
         price = CommonMethods.doublePriceToIntegerPrice(priceDoub, null);
      }

      return price;

   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select(".pager.bottom > ul > li.next").isEmpty();
   }

   private Document fetchNextPage() {
      Logging.printLogDebug(logger, session, "fetching next page...");
      webdriver.waitForElement(".pager.bottom > ul > li.next", 5);
      WebElement button = webdriver.driver.findElement(By.cssSelector(".pager.bottom > ul > li.next"));
      webdriver.clickOnElementViaJavascript(button);
      webdriver.waitLoad(10000);

      return Jsoup.parse(webdriver.getCurrentPageSource());
   }

}
