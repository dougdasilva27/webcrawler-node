package br.com.lett.crawlernode.crawlers.ranking.keywords.espana;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class EspanaDiaCrawler extends CrawlerRankingKeywords {

   public EspanaDiaCrawler(Session session) {
      super(session);
   }

   private static final String HOST = "www.dia.es";

   @Override
   protected void extractProductsFromCurrentPage() {

      this.pageSize = 40;
      this.currentPage = 0;
      this.log("Página " + this.currentPage);

      String url = "https://" + HOST + "/compra-online/search?q=" + this.keywordWithoutAccents + "%3Arelevance&page=" + this.currentPage + "&disp=";

      System.err.println(url);

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".span-16.last .span-3");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".prod_grid", "data-productcode");
            String internalPid = internalId;
            String productUrl = CrawlerUtils.scrapUrl(e, ".prod_grid a", "href", "https://", HOST);

            saveDataProduct(internalId, internalPid, productUrl);

            this.log(
                  "Position: " + this.position +
                        " - InternalId: " + internalId +
                        " - InternalPid: " + internalPid +
                        " - Url: " + productUrl
            );

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
      return !this.currentDoc.select(".paginatorBottom .prod_refine .pager li a[href]:not(:first-child)").isEmpty();
   }

}
