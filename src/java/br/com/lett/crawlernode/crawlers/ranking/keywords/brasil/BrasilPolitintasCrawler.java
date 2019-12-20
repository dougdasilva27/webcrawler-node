package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.Arrays;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilPolitintasCrawler extends CrawlerRankingKeywords {
   private static final String HOME_PAGE = "https://loja.politintas.com.br/";

   public BrasilPolitintasCrawler(Session session) {
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
      Elements products = this.currentDoc.select(".box-item");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".wrapper-buy-button-asynchronous .btn", "data-template").replaceAll("[^0-9]+", "");
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".box-item", "data-id");
            String productUrl = CrawlerUtils.scrapUrl(e, ".box-item .product-image", Arrays.asList("href"), "https", HOME_PAGE);

            saveDataProduct(null, internalPid, productUrl);

            this.log(
                  "Position: " + this.position +
                        " - InternalId: " + internalId +
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
   protected boolean hasNextPage() {
      Integer productCount = this.currentDoc.select(".pager.bottom .pages .page-number:not(.pgCurrent)").size();
      return productCount >= this.pageSize;
   }
}
