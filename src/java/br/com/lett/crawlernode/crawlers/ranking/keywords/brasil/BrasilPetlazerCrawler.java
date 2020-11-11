package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.Arrays;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilPetlazerCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "petlazer.com.br";

   public BrasilPetlazerCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 30;
      this.log("Página " + this.currentPage);

      String url = "https://www.petlazer.com.br/Busca/" + this.keywordWithoutAccents + "/" + this.currentPage + ".html";

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".container .col-lg-3.col-md-3.top-m-20px");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalPid = null;

            String productUrl = CrawlerUtils.scrapUrl(e, "a", Arrays.asList("href"), "https", HOME_PAGE);

            if(productUrl != null){

            Integer pidString = productUrl.indexOf("produto/");
            String id = productUrl.substring(pidString).split("/")[1];
               internalPid = id != null ? id : null; // O unico lugar onde foi possivel encontrar o internalId foi dentro da URL

            }
            saveDataProduct(null, internalPid, productUrl);

            this.log(
                  "Position: " + this.position +
                        " - InternalId: " + null +
                        " - InternalPid: " + internalPid +
                        " - Url: " + productUrl);

            if (this.arrayProducts.size() == productsLimit)
               break;
         }

      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
            + this.arrayProducts.size() + " produtos crawleados");

   }

   @Override
   protected boolean hasNextPage() {
      return this.currentDoc.selectFirst(".pagination.pagination-lg li:nth-child(3) a") != null;
   }
}
