package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilEtnamoveisCrawler extends CrawlerRankingKeywords {

   public BrasilEtnamoveisCrawler(Session session) {
      super(session);
      this.pageSize = 12;
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      String url = "https://www.etna.com.br/search/?text=" + keywordEncoded + "&page=" + currentPage;
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".element_item");

      if (products.size() >= 1) {
         if (this.totalProducts == 0) setTotalProducts();

         for (Element e : products) {
            String internalId = e.attr("id");
            String urlProduct = "https://www.etna.com.br" + e.attr("href");

            saveDataProduct(internalId, null, urlProduct);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - Url: " + urlProduct);
         }
      }
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapSimpleInteger(currentDoc, ".row .pagination-bar-results", false);
   }
}
