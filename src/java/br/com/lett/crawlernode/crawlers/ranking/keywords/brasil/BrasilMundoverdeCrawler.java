package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilMundoverdeCrawler extends CrawlerRankingKeywords {

   public BrasilMundoverdeCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {

      this.pageSize = 12;

      this.log("Página " + this.currentPage);

      String url = "https://www.mundoverde.com.br/" + this.keywordEncoded + "?page=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetch(url);

      Elements products = this.currentDoc.select(".vtex-product-summary-2-x-container");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         if (products.size() >= 1) {
            for (Element e : products) {

               String urlProduct = CrawlerUtils.scrapUrl(e, "a", "href", "https://", "www.mundoverde.com.br");

               saveDataProduct(null, null, urlProduct);

               this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + null + " - Url: " + urlProduct);
               if (this.arrayProducts.size() == productsLimit) break;

            }
         } else {
            this.result = false;
            this.log("Keyword sem resultado!");
         }

         this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

      }
   }

   private Document fetch(String url) {
      webdriver = DynamicDataFetcher.fetchPageWebdriver(url, ProxyCollection.LUMINATI_SERVER_BR_HAPROXY, session);
      webdriver.waitLoad(5000);
      Document doc = Jsoup.parse(webdriver.getCurrentPageSource());
      return doc;
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, ".vtex-search-result-3-x-totalProducts--layout span", null, null, false, false, 0);
      this.log("Total de produtos: " + this.totalProducts);
   }
}


