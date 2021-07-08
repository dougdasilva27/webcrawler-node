package br.com.lett.crawlernode.crawlers.ranking.keywords.panama;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;

public class PanamaSuperunicoCrawler extends CrawlerRankingKeywords {

   public PanamaSuperunicoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 64;
      this.log("Página " + this.currentPage);

      String url = "https://superunico.com/page/" + this.currentPage + "/?s=" + this.keywordEncoded + "&post_type=product";

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".product-grid-item");
      if (!products.isEmpty()) {
         for (Element e : products) {

            String productId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".woodmart-add-btn a", "data-product_id");
            String productPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".woodmart-add-btn a", "data-product_sku");
            String productUrl = CrawlerUtils.scrapUrl(this.currentDoc, ".product-element-top a", Arrays.asList("href"), "https", "superunico.com");

            saveDataProduct(productId, productPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + productId + " - InternalPid: " + productPid + " - Url: " + productUrl);
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
      return !currentDoc.select(".next.page-numbers").isEmpty();
   }
}
