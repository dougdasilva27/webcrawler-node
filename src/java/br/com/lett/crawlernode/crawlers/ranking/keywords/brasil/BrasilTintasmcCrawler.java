package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.Arrays;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilTintasmcCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "https://www.tintasmc.com.br";

   public BrasilTintasmcCrawler(Session session) {
      super(session);
      this.pageSize = 12;
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);

      String url = HOME_PAGE + "/" + this.keywordWithoutAccents.replaceAll(" ", "%20") + "?" +
            "PS=" + this.pageSize + "#" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".box-item.text-center");
      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".wrapper-buy-button-asynchronous :nth-child(3)", "value");
            String productUrl = CrawlerUtils.scrapUrl(e, "a.product-image", Arrays.asList("href"), "https", HOME_PAGE);
            saveDataProduct(null, internalId, productUrl);
            this.log("Position: " + this.position + " - InternalId: " + internalId + " - Url: " + productUrl);
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
      Integer size = this.currentDoc.select(".box-item").size();
      return size >= this.pageSize;
   }
}
