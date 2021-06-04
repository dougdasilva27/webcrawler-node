package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilMultitintasbrasilCrawler extends CrawlerRankingKeywords {

   public BrasilMultitintasbrasilCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);

      String url = "https://multitintasbrasil.com.br/loja/" + this.keywordEncoded + "?page=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".product-layout");

      if (!products.isEmpty()) {
         for (Element e : products) {

            String productUrl = CrawlerUtils.scrapUrl(e, ".image a", "href", "https", "www.multitintasbrasil.com.br");
            String internalId = productUrl != null ? CommonMethods.getLast(productUrl.split("id=")) : null;

            saveDataProduct(internalId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null
               + " - Url: " + productUrl);

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }

      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   public boolean hasNextPage() {
      Elements pagination = this.currentDoc.select(".pagination li");
      for (Element e : pagination) {
         String nextPage = e.toString();
         return nextPage.contains(">");
      }

      return false;

   }

}
