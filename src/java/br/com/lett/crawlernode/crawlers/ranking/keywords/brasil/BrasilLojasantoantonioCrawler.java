package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilLojasantoantonioCrawler extends CrawlerRankingKeywords {

   public BrasilLojasantoantonioCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 50;

      this.log("Página " + this.currentPage);

      String url = "https://www.lojasantoantonio.com.br/" + CommonMethods.encondeStringURLToISO8859(location, logger, session) + "?PS=50&PageNumber=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("div.vitrine div.prateleira > ul > li[layout]");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) setTotalProducts();
         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".id-sku", "value");
            String productUrl = CrawlerUtils.scrapUrl(e, ".informacoes a", "href", "https", "www.lojasantoantonio.com.br");

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
      Element totalElement = this.currentDoc.select("span.resultado-busca-numero > span.value").first();

      if (totalElement != null) {
         try {
            this.totalProducts = Integer.parseInt(totalElement.text().trim());
         } catch (Exception e) {
            this.logError(e.getMessage());
         }

         this.log("Total da busca: " + this.totalProducts);
      }
   }
}
