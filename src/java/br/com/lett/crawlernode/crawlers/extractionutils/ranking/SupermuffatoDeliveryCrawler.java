package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

public class SupermuffatoDeliveryCrawler extends CrawlerRankingKeywords {

   private static final String BASE_URL = "delivery.supermuffato.com.br/";
   private String HOME_PAGE = "https://delivery.supermuffato.com.br";

   public SupermuffatoDeliveryCrawler(Session session) {
      super(session);
   }

   protected String getCityCode() {
      return session.getOptions().optString("cityCode");
   }

   public static void waitForElement(WebDriver driver, String cssSelector) {
      WebDriverWait wait = new WebDriverWait(driver, 10);
      wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)));
   }

   protected Document fetchDocumentWebDriver(String url) {
      String city = getCityCode();
      Document doc = null;

      int attempts = 0;

      List<String> proxies = List.of(ProxyCollection.BUY_HAPROXY, ProxyCollection.LUMINATI_SERVER_BR_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_ANY_HAPROXY);
      do {

         try {
            webdriver = DynamicDataFetcher.fetchPageWebdriver(HOME_PAGE, proxies.get(attempts), session);

            waitForElement(webdriver.driver, ".form-control optgroup option[value='13']");
            webdriver.findAndClick(".form-control optgroup option[value='13']", 30000);

            waitForElement(webdriver.driver, "#s-ch-change-channel");
            webdriver.findAndClick("#s-ch-change-channel", 30000);
            webdriver.waitLoad(10000);

            waitForElement(webdriver.driver, "#s-ch-change-channel");
            webdriver.findAndClick("#s-ch-change-channel", 30000);
            webdriver.waitLoad(10000);

            waitForElement(webdriver.driver, ".fulltext-search-box.ui-autocomplete-input");
            WebElement pass = webdriver.driver.findElement(By.cssSelector(".fulltext-search-box.ui-autocomplete-input"));
            pass.sendKeys(this.keywordEncoded);

            waitForElement(webdriver.driver, ".icon.icon-search");
            webdriver.findAndClick(".icon.icon-search", 30000);

            webdriver.waitLoad(30000);
            doc = Jsoup.parse(webdriver.getCurrentPageSource());

         } catch (Exception e) {
            Logging.printLogInfo(logger, session, CommonMethods.getStackTrace(e));

         } finally {
            if (webdriver != null) {
               webdriver.terminate();
            }
         }
      } while (doc == null && attempts++ < 3);

      return doc;
   }

   @Override
   protected Document fetchDocument(String url) {
      Request request = Request.RequestBuilder.create().setUrl(url)
         .build();

      Response response = new JsoupDataFetcher().get(session, request);
      return Jsoup.parse(response.getBody());

   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 48;
      this.log("Página " + this.currentPage);
      String url;

      if (currentPage == 1) {
         url = "https://delivery.supermuffato.com.br/" + this.keywordEncoded;

         this.currentDoc = fetchDocumentWebDriver(url);

      } else {
         url = " https://delivery.supermuffato.com.br/buscapagina?ft=" + this.keywordWithoutAccents + "&PS=48&sl=d85149b5-097b-4910-90fd-fa2ce00fe7c9&cc=48&sm=0&PageNumber=" + this.currentPage;

         this.currentDoc = fetchDocument(url);
      }

//https://delivery.supermuffato.com.br/buscapagina?ft=cerveja&PS=48&sl=d85149b5-097b-4910-90fd-fa2ce00fe7c9&cc=48&sm=0&PageNumber=1

      this.log("Link onde são feitos os crawlers: " + url);

      Elements products = this.currentDoc != null ? this.currentDoc.select("div.prd-list-item") : null;

      if (products != null && !products.isEmpty()) {

         if (this.totalProducts == 0)
            setTotalProducts();

         for (int index = 0; index < products.size(); index++) {
            Element product = products.get(index);

            String internalPid = product.attr("data-product-id");
            String urlProduct = CrawlerUtils.scrapUrl(product, ".prd-list-item-desc > a", "href", "https", BASE_URL);
            Integer price = crawlPrice(product);
            boolean available = price != null;

            RankingProduct rankingProduct = RankingProductBuilder.create()
               .setName(CrawlerUtils.scrapStringSimpleInfo(product, ".prd-list-item-name", true))
               .setUrl(urlProduct)
               .setInternalPid(internalPid)
               .setImageUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "li[layout] .prd-list-item-img img", "src"))
               .setAvailability(available)
               .setPriceInCents(price)
               .build();


            saveDataProduct(rankingProduct);

            if (this.arrayProducts.size() == productsLimit) break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
         setTotalProducts();
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
      if (!(hasNextPage())) setTotalProducts();
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
   protected void setTotalProducts() {

      this.totalProducts = this.currentDoc != null ? CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".resultado-busca-numero span.value", true, 0) : 0;
      this.log("Total da busca: " + this.totalProducts);
   }

}
