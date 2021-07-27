package br.com.lett.crawlernode.crawlers.ranking.keywords.manaus;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;

public class ManausSenhormercadomanausCrawler extends CrawlerRankingKeywords {

   public ManausSenhormercadomanausCrawler(Session session) {
      super(session);

   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 32;
      this.log("Página " + this.currentPage);

      String lojaId = "957149";
      String url = "https://www.senhormercado.online/loja/busca.php?loja=" + lojaId + "&palavra_busca=" + this.keywordEncoded + "&pg=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".list.flex.f-wrap.row li");
      if (!products.isEmpty()) {
         for (Element e : products) {

            String productId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".list-variants", "data-id");
            String productUrl = CrawlerUtils.scrapUrl(e, ".info-product", Arrays.asList("href"), "https", "www.senhormercado.online");

            saveDataProduct(productId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + productId + " - InternalPid: " + null + " - Url: " + productUrl);
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
   protected boolean hasNextPage() {
      return !currentDoc.select(".page-next.page-link").isEmpty();
   }
}
