package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.Arrays;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilReidosanimaisCrawler extends CrawlerRankingKeywords {

   public BrasilReidosanimaisCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 30;
      this.log("Página " + this.currentPage);

      String url = "https://www.reidosanimais.com.br/catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordEncoded;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".products .products__item");

      if (!products.isEmpty()) {

         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "[product-id]", "product-id");
            String productUrl = CrawlerUtils.scrapUrl(e, "a.product__name", Arrays.asList("href"), "https", "www.reidosanimais.com.br");

            saveDataProduct(null, internalPid, productUrl);

            // 1197

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
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".amount .total", true, 0);
      this.log("Total: " + this.totalProducts);
   }
}
