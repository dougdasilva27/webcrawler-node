package br.com.lett.crawlernode.crawlers.ranking.keywords.portugal;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class PortugalElcorteinglesCrawler extends CrawlerRankingKeywords {

   public PortugalElcorteinglesCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 12;

      this.log("Página " + this.currentPage);
      String keyword = this.keywordWithoutAccents.replace(" ", "+");

      this.log("Página " + this.currentPage);

      // monta a url com a keyword e a página
      String url = "https://www.elcorteingles.pt/search/?s=" + keyword;
      this.currentDoc = fetchDocument(url);


      this.log("Link onde são feitos os crawlers: " + url);


      Elements products = this.currentDoc.select(".product-preview");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-image .hidden", "data-bv-product-id");
            String productUrl = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-image .hidden", "data-bv-redirect-url"), "http://", "www.elcorteingles.pt");

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

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, "#product-list-total", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }

}
