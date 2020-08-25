package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ChileCruzverdeCrawler extends CrawlerRankingKeywords {

   public ChileCruzverdeCrawler(Session session) {
      super(session);
   }

   protected void extractProductsFromCurrentPage() {
      this.pageSize = 0;
      this.log("Página " + this.currentPage);

      String url = "https://www.cruzverde.cl/busqueda?q=" + this.keywordWithoutAccents + "&search-button=&lang=es_CL&start=" + this.pageSize + "&sz=12";
      String urlWithoutSpaces = url.replaceAll(" ", "+");

      this.log("Link onde são feitos os crawlers: " + urlWithoutSpaces);
      this.currentDoc = fetchDocument(urlWithoutSpaces);
      Elements products = this.currentDoc.select(".product.product-wrapper");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {

            String internalId = e.attr("data-pid");
            String productUrl = "https://www.cruzverde.cl" + CrawlerUtils.scrapStringSimpleInfoByAttribute(e, " .image-container a", "href");

            saveDataProduct(internalId, internalId, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalId + " - Url: " + productUrl);
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

   @Override protected boolean hasNextPage() {
      return currentDoc.selectFirst(".pagination.pagination-footer button:not(first-child)") != null;
   }
}
