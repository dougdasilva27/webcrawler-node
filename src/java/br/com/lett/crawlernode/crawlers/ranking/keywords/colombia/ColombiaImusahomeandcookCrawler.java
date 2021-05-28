package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class ColombiaImusahomeandcookCrawler extends CrawlerRankingKeywords {

   public ColombiaImusahomeandcookCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);

      String url = "https://www.imusahomeandcook.com/search?q=" + this.keywordEncoded;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".product");

      if (!products.isEmpty()) {
         for (Element e : products) {

            String productUrl = CrawlerUtils.scrapUrl(e, ".caja-tags", "href", "https", "www.imusahomeandcook.com");
            String internalId = CommonMethods.getLast(productUrl.split("-"));

            saveDataProduct(internalId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId  + " - InternalPid: " + null
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

}
