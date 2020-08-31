package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilNacaoverdeCrawler extends CrawlerRankingKeywords {

   private String HOME_PAGE = "https://www.nacaoverde.com.br";

   public BrasilNacaoverdeCrawler(Session session) {
      super(session);
      this.pageSize = 24;
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);

      String url = HOME_PAGE + "/buscapagina?ft=" + this.keywordEncoded +
         "&PS=" + pageSize + "&sl=3264a113-96ad-4a0a-ac0d-bfa7335914e6&cc=24&sm=0&PageNumber=" + this.currentPage;

      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".prateleira ul li[layout]");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalId = e.selectFirst(".id").text();
            String productUrl = e.selectFirst(".productShelf").attr("href");

            saveDataProduct(internalId, internalId, productUrl);

            this.log("Position: " + this.position +
               " - InternalId: " + internalId +
               " - InternalPid: " + internalId +
               " - Url: " + productUrl);
         }
      }
      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean checkIfHasNextPage() {
      return (arrayProducts.size() % pageSize - currentPage) < 0;
   }
}
