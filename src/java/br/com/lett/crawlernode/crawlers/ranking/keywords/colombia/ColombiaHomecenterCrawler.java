package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;

public class ColombiaHomecenterCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "www.homecenter.com.co";

   public ColombiaHomecenterCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 28;
      this.log("Página " + this.currentPage);

      String url = "https://www.homecenter.com.co/homecenter-co/search/?Ntt=" + this.keywordEncoded + "&currentpage=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select("[data-category='']");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, null, "data-key");
            String productUrl = CrawlerUtils.scrapUrl(e, ".product.ie11-product-container .link-with-wrapper a", Arrays.asList("href"), "https", HOME_PAGE);

            saveDataProduct(internalId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null
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
      Element page = this.currentDoc.selectFirst("#bottom-pagination-next-page");
      return page != null;
   }

}
