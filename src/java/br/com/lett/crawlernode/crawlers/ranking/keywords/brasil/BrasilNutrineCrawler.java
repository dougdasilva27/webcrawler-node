package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.Arrays;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilNutrineCrawler extends CrawlerRankingKeywords {
   public BrasilNutrineCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = "https://www.nutrine.com.br";

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 36;

      this.log("Página " + this.currentPage);

      String url = "https://www.nutrine.com.br/loja/busca.php?loja=722608&palavra_busca=" + this.keywordWithoutAccents + "&pg=" + this.currentPage;

      this.log("Url: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".list .item .product");

      if (products.size() >= 1) {

         for (Element e : products) {
            String internalPid = null;
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".variants .list-variants", "data-id");
            String productUrl = CrawlerUtils.scrapUrl(e, " .image a", Arrays.asList("href"), "https", HOME_PAGE);


            saveDataProduct(internalId, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: "
                  + internalPid + " - Url: " + productUrl);

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
   protected boolean hasNextPage() {
      return !this.currentDoc.select(".paginate-links .page-link:not(:first-child)").isEmpty();
   }

}
