package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * the order doesn’t change with the login, so I didn’t apply the webdrive to the crawler ranking
 */

public abstract class MrestoqueunidasulCrawler extends CrawlerRankingKeywords {
   public MrestoqueunidasulCrawler(Session session) {
      super(session);
   }

   @Override
   public void extractProductsFromCurrentPage() {
      this.pageSize = 24;
      this.log("Página " + this.currentPage);

      String url = "https://www.mrestoque.com.br/busca?s=" + this.keywordEncoded + "&order=name_asc&pagina=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".products-list.row li");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-image a", "data-product");
            String urlProduct = CrawlerUtils.scrapUrl(e, ".product-image a", "href", "https", "www.mrestoque.com.br");

            saveDataProduct(internalId, null, urlProduct);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + urlProduct);
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }

         }
      } else {
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, ".total-items-found strong", null, null, true, true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }


}
