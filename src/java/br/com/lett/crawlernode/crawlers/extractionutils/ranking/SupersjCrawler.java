package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

abstract public class SupersjCrawler extends CrawlerRankingKeywords {

   private int LAST_PRODUCT_INDEX = 0;

   public SupersjCrawler(Session session) {
      super(session);
   }

   protected abstract String getLocationId();

   private Document fetchNextPage(){
      Logging.printLogDebug(logger, session, "fetching next page...");
      WebElement button = webdriver.driver.findElement(By.cssSelector("button.loja-btn-cor-secundaria"));
      webdriver.clickOnElementViaJavascript(button);
      webdriver.waitLoad(8000);

      return Jsoup.parse(webdriver.getCurrentPageSource());
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);

      String url = "https://www.supersj.com.br/?busca=" + this.keywordEncoded;

      if(LAST_PRODUCT_INDEX == 0){
         this.currentDoc = fetchDocumentWithWebDriver(url, 20000, ProxyCollection.BUY_HAPROXY);
      }else{
         this.currentDoc = fetchNextPage();
      }

      Elements products = this.currentDoc.select(".product-item-wrapper");

      if (products != null && !products.isEmpty()) {
         for (int i = LAST_PRODUCT_INDEX; i < products.size(); i++) {
            Element product = products.get(i);
            String internalId = CrawlerUtils.scrapStringSimpleInfo(product, "div.product-cdg a", true);
            String productName = CrawlerUtils.scrapStringSimpleInfo(product, "div.product-text-dt h5", true);
            String productUrl = "https://www.supersj.com.br/?busca=" + productName.replace(" ", "%20");

            saveDataProduct(internalId, null, productUrl);
            LAST_PRODUCT_INDEX++;

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      }else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
   }

   @Override
   protected boolean hasNextPage(){
      return this.currentDoc.selectFirst("button.loja-btn-cor-secundaria") != null;
   }


}
