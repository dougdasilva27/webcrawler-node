package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ArgentinaMaxidescuentoCrawler extends CrawlerRankingKeywords {

   public ArgentinaMaxidescuentoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 12;

      this.log("Página " + this.currentPage);

      String url = "http://www.maxidescuento.com.ar/busqueda?controller=search&s=" + this.keywordEncoded + "&page=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".products.row .product-miniature");

      if (products.size() >= 1) {
         if (this.totalProducts == 0) setTotalProducts();
         for (Element e : products) {

            String internalId = e.attr("data-id-product");
            String productUrl = CrawlerUtils.scrapUrl(e, ".thumbnail-container-image .product-thumbnail", "href", "https://", "http://www.maxidescuento.com.ar");

            saveDataProduct(internalId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit) break;

         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".row.products-selection p", false,0);
      this.log("Total da busca: " + this.totalProducts);
   }
}

