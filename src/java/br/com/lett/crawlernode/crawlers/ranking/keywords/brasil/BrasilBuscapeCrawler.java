package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.test.Test;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilBuscapeCrawler extends CrawlerRankingKeywords {
   public BrasilBuscapeCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 36;
      this.log("Página " + this.currentPage);

      String url = "https://www.buscape.com.br/search?q=" + this.keywordEncoded;

      if (this.currentPage > 1) {
         url = CrawlerUtils.scrapUrl(this.currentDoc, "a.next", "href", "https", "www.buscape.com.br");
      }

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      CommonMethods.saveDataToAFile(currentDoc, Test.pathWrite + "BUSCAPE.html");
      Elements products = this.currentDoc.select("li.item[data-id]");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, ".products-amount", true, 0);
            this.log("Total da busca: " + this.totalProducts);
         }
         for (Element e : products) {
            String internalId = e.attr("data-id");
            String productUrl = CrawlerUtils.scrapUrl(e, ".prod-name a", "href", "https", "www.buscape.com.br");

            if (productUrl != null) {
               productUrl = productUrl.split("\\?")[0];
            }

            saveDataProduct(internalId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
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
      String nextUrl = CrawlerUtils.scrapUrl(this.currentDoc, "a.next", "href", "https", "www.buscape.com.br");
      return super.hasNextPage() && nextUrl != null && !nextUrl.contains("javascript:void(0)");
   }

}
