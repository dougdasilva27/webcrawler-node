package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilBuscapeCrawler extends CrawlerRankingKeywords {
   public BrasilBuscapeCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 36;
      this.log("Página " + this.currentPage);

      String url = "https://www.buscape.com.br/search?q=" + this.keywordEncoded + "&page=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select("[data-id]");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         for (Element e : products) {
            String internalId = e.attr("data-id").replace("product", ""); // Ex: data-id="product739372" were 739372 is the internalId
            String productUrl = !internalId.contains("offer") ? CrawlerUtils.scrapUrl(e, "a.name", "href", "https", "www.buscape.com.br") : null;

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
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, "#pageSearchResultsBody span", true, 0);
      this.log("Total: " + this.totalProducts);
   }
}
