package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilCasavegCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "https://casaveg.com.br/";

   public BrasilCasavegCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 48;
      this.log("Página " + this.currentPage);

      String url = "https://casaveg.com.br/busca?controller=search&orderby=position&orderway=desc"
            + "&search_query=" + this.keywordEncoded + "&submit_search=&p=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".product_list .ajax_block_product a.product-name");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String productUrl = e.attr("href");
            String internalId = productUrl.replace(HOME_PAGE, "").split("/")[0];

            saveDataProduct(internalId, null, productUrl);

            this.log(
                  "Position: " + this.position +
                        " - InternalId: " + internalId +
                        " - InternalPid: " + null +
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
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, ".heading-counter", true, 0);
      this.log("Total de produtos: " + this.totalProducts);
   }
}
