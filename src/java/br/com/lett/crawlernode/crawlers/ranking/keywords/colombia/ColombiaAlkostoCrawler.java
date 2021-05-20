package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Collections;

public class ColombiaAlkostoCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "www.alkosto.com";

   public ColombiaAlkostoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 25;
      this.log("Página " + this.currentPage);

      String url = "https://www.alkosto.com/search/?text=" + this.keywordEncoded + "&page=" + this.currentPage + "&pageSize=25&sort=relevance";

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("ul.product__listing.product__list > li.product__list--item");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "h2.product__information--name > a.js-product-click-datalayer", "data-id");
            String productUrl = CrawlerUtils.scrapUrl(e, "h2.product__information--name > a.js-product-click-datalayer", Collections.singletonList("href"), "https", HOME_PAGE);

            saveDataProduct(null, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid
               + " - Url: " + productUrl);

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
      String totalProductsStr = CrawlerUtils.scrapStringSimpleInfoByAttribute(this.currentDoc, "span.js-search-count", "data-count");

      if(totalProductsStr != null){
         int totalProducts = Integer.parseInt(totalProductsStr);
         return this.arrayProducts.size() < totalProducts;
      }

      return false;
   }
}
