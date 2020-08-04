package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import java.util.Arrays;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class SaopauloPolipetCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "www.polipet.com.br";

   public SaopauloPolipetCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      // número de produtos por página do market
      this.pageSize = 20;

      this.log("Página " + this.currentPage);

      // monta a url com a keyword e a página
      String url = "https://www.polipet.com.br/busca?busca=" + this.keywordEncoded + "&pagina=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);

      // chama função de pegar o html
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".fbits-item-lista-spot");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".spot", "id").split("-")[3];
            String internalPid = internalid;
            String productUrl = CrawlerUtils.scrapUrl(e, ".spotContent .spot-parte-um", Arrays.asList("href"), "https", HOME_PAGE);

            saveDataProduct(internalid, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalid + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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

   @Override protected boolean hasNextPage() {
      return this.currentDoc.selectFirst(".fbits-paginacao ul .pg a") != null;
   }
}
