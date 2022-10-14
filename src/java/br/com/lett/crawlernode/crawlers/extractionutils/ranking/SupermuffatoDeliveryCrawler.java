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
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

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
   @Override
   protected Document fetchDocument(String url) {
      String city = getCityCode();

      try {
         webdriver = DynamicDataFetcher.fetchPageWebdriver(HOME_PAGE, ProxyCollection.BUY_HAPROXY, session);

         waitForElement(webdriver.driver, ".form-control optgroup option[value='13']");
         webdriver.findAndClick(".form-control optgroup option[value='13']", 10000);

         waitForElement(webdriver.driver, "#s-ch-change-channel");
         webdriver.findAndClick("#s-ch-change-channel", 10000);
         webdriver.waitLoad(10000);
         waitForElement(webdriver.driver, ".fulltext-search-box.ui-autocomplete-input");
         WebElement pass = webdriver.driver.findElement(By.cssSelector(".fulltext-search-box.ui-autocomplete-input"));
         pass.sendKeys(this.keywordEncoded);

         waitForElement(webdriver.driver, ".icon.icon-search");
         webdriver.findAndClick(".icon.icon-search", 10000);

//         webdriver.loadUrl(url);
//         webdriver.waitLoad(10000);

         webdriver.waitForElement(".ProductList--grid div.ProductItem", 30);

         return Jsoup.parse(webdriver.getCurrentPageSource());

      } catch (Exception e) {
         Logging.printLogInfo(logger, session, CommonMethods.getStackTrace(e));

      } finally {
         if (webdriver != null) {
            webdriver.terminate();
         }
      }
      return null;
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 24;
      this.log("Página " + this.currentPage);
      // https://delivery.supermuffato.com.br/cerveja?sc=13&utmi_cp=14912022153927552
      // &utmi_cp=14912022154028856
      String url = "https://delivery.supermuffato.com.br/" + this.keywordEncoded ;
         //"?&sc=" + getCityCode() +
         //"#" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select("li[layout]");
      Elements productsIdList = this.currentDoc.select("li[id].helperComplement");

      if (products.size() >= 1) {

         if (this.totalProducts == 0)
            setTotalProducts();

         for (int index = 0; index < products.size(); index++) {
            Element product = products.get(index);

            String internalPid = crawlInternalPid(productsIdList.get(index));
            String urlProduct = CrawlerUtils.scrapUrl(product, ".prd-list-item-desc > a", "href", "https", BASE_URL);
            Integer price = CommonMethods.doublePriceToIntegerPrice(CrawlerUtils.scrapDoublePriceFromHtml(product, ".prd-list-item-price-sell", null, true, ',', session), 0);

            RankingProduct rankingProduct = RankingProductBuilder.create()
               .setName(CrawlerUtils.scrapStringSimpleInfo(product, ".prd-list-item-name", true))
               .setUrl(urlProduct)
               .setInternalPid(internalPid)
               .setImageUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "li[layout] .prd-list-item-img img", "src"))
               .setAvailability(true)
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

   @Override
   protected void setTotalProducts() {
      Document html = fetchDocument("https://delivery.supermuffato.com.br/" + keywordEncoded);
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(html, ".resultado-busca-numero span.value", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }

   private String crawlInternalPid(Element productId) {
      String id = CrawlerUtils.scrapStringSimpleInfoByAttribute(productId, null, "id");
      String[] split = id.split("_");
      return split[1];
   }

}
