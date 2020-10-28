package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilNanacareCrawler extends CrawlerRankingKeywords {

   public BrasilNanacareCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      // número de produtos por página do market
      this.pageSize = 24;

      this.log("Página " + this.currentPage);

      String url = "https://www.nanacare.com.br/buscar?q=" + this.keywordWithoutAccents.replaceAll(" ", "+");

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".listagem-item");

      if (products.size() >= 1) {
         for (Element e : products) {

            String internalId = CrawlerUtils.scrapStringSimpleInfo(e, ".info-produto .produto-sku", false);
            String productUrl = CrawlerUtils.scrapUrl(e, ".info-produto .nome-produto", "href", "http:", "www.nanacare.com.br");

            saveDataProduct(internalId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }

         }
      } else {
         setTotalProducts();
         this.result = false;
         this.log("Keyword sem resultado!");
      }


      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

}
