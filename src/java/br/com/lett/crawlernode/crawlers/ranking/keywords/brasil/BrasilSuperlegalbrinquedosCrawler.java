package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRanking;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.VTEXRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilSuperlegalbrinquedosCrawler extends CrawlerRankingKeywords {

   private static final String BASE_URL = "www.superlegalbrinquedos.com.br";

   public BrasilSuperlegalbrinquedosCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 12;

      this.log("Página " + this.currentPage);

      String url = "https://www.superlegalbrinquedos.com.br/buscapagina?&ft="+this.keywordEncoded+"&PS=12&sl=bb03e559-9abb-44c8-aca7-5be426a4988d&cc=12&sm=0&PageNumber="+this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("ul .brinquedos");

      if (!products.isEmpty()) {

         for (Element product : products) {

            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "div[data-product-id]","data-product-id");
            String productUrl = CrawlerUtils.scrapUrl(product, ".product-item__title a", "href", "https:", BASE_URL);

            saveDataProduct(null, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);

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
   protected boolean hasNextPage() {
      return true;
   }
}
