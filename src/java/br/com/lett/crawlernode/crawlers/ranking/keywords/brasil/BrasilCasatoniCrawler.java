package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.Arrays;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilCasatoniCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "https://www.casatoni.com.br/";

   public BrasilCasatoniCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 24;
      this.log("Página " + this.currentPage);

      String url = HOME_PAGE + this.keywordEncoded +
            "?PageNumber=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".prateleira.vitrine.n4colunas ul > li:not(.helperComplement)");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".box-item.text-center div.wrapper-buy-button-asynchronous input.buy-button-asynchronous-product-id", "value");
            String productUrl = CrawlerUtils.scrapUrl(e, "span b a", Arrays.asList("href"), "https", HOME_PAGE);

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
   protected boolean hasNextPage() {
      boolean hasNext = this.currentDoc.select(".prateleira.vitrine.n4colunas ul > li:not(.helperComplement)").hasClass(".pgEmpty");

      return hasNext;
   }
}
