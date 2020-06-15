package br.com.lett.crawlernode.crawlers.ranking.keywords.portugal;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class PortugalAuchanCrawler extends CrawlerRankingKeywords {

   public PortugalAuchanCrawler(Session session) {
      super(session);

   }

   @Override
   protected void extractProductsFromCurrentPage() {

      this.pageSize = 30;
      this.log("Página " + this.currentPage);

      String url = "https://www.auchan.pt/Frontoffice/search/" + this.keywordWithoutAccents;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocumentWithWebDriver(url);

      Elements products = this.currentDoc.select(".product-item");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalId = e.attr("data-product-id");
            String productUrl = "https://www.auchan.pt" + CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-item-header a", "href");
            saveDataProduct(internalId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit)
               break;

         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, "#page .col-sm-9 h3", false, 0);

      this.log("Total de produtos: " + this.totalProducts);
   }

}
