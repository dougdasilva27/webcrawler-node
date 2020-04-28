package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.Arrays;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilBreedsCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "www.breeds.com.br/";

   public BrasilBreedsCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 21;
      this.log("Página " + this.currentPage);

      String url = "https://" + HOME_PAGE + "catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordEncoded;
      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".products-grid .item");

      if (!products.isEmpty()) {

         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {

            Integer internalPidInt = CrawlerUtils.scrapIntegerFromHtmlAttr(e, ".regular-price", "id", null);
            String internalPid = internalPidInt != null ? internalPidInt.toString() : null;
            String productUrl = CrawlerUtils.scrapUrl(e, ".item a", Arrays.asList("href"), "https:", HOME_PAGE);

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
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".amount.amount--has-pages", "de", "", true, false, 0);

      this.log("Total de produtos: " + this.totalProducts);
   }
}
