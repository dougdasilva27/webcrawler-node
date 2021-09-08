package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilCentralarCrawler extends CrawlerRankingKeywords {

   public BrasilCentralarCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      String url = null;

      String keyword = this.keywordEncoded.replace(" ", "+");

      if (currentPage != 1) {
         url = "https://www.centralar.com.br/search?q=" + keyword + "%3Aprice-asc&page=" + (this.currentPage - 1);
      } else {
         url = "https://www.centralar.com.br/search/?text=" + keyword;
      }


      this.log("Página " + this.currentPage);
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".pdc_product-item");

      if (!products.isEmpty()) {
         if (totalProducts == 0) {
            this.setTotalProducts();
         }

         for (Element product : products) {

            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".compare-to-input input", "data-code");
            String productUrl = CrawlerUtils.scrapUrl(product, ".thumb", "href", "https", "www.centralar.com.br");


            saveDataProduct(internalId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);

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

   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".pagination-bar-results", true, 0);
      this.log("Total: " + this.totalProducts);
   }

}
