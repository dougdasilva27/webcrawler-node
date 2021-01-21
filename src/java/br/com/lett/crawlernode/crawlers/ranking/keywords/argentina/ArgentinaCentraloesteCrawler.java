package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;

public class ArgentinaCentraloesteCrawler extends CrawlerRankingKeywords {


   private static final String HOME_PAGE = "centraloeste.com.ar";

   public ArgentinaCentraloesteCrawler(Session session) {
      super(session);
   }

   private String categoryUrl;

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 20;
      this.log("Página " + this.currentPage);

      String url = crawlUrl();

      if (this.currentPage > 1 && this.categoryUrl != null) {
         url = this.categoryUrl + "?p=" + this.currentPage;
      }

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".item.product.product-item");

      if (this.currentPage == 1) {
         String redirectUrl = CrawlerUtils.getRedirectedUrl(url, session);

         if (!url.equals(redirectUrl)) {
            this.categoryUrl = redirectUrl;
         }
      }

      if (!products.isEmpty()) {

         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, null, "data-product-id");
            String productUrl = CrawlerUtils.scrapUrl(e, ".product-item-image a", Arrays.asList("href"), "https:", HOME_PAGE);
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".actions-secondary a", "data-sku-for-testing");

            saveDataProduct(internalId, internalPid, productUrl);

            this.log(
               "Position: " + this.position +
                  " - InternalId: " + internalId +
                  " - InternalPid: " + internalPid +
                  " - Url: " + productUrl);

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

   private String crawlUrl() {
      String link;

      if (this.currentPage > 1) {
         link = "https://www.centraloeste.com.ar/catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordEncoded;
      } else {
         link = "https://www.centraloeste.com.ar/catalogsearch/result/?q=" + this.keywordEncoded;
      }
      return link;
   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select(".item.pages-item-next").isEmpty();
   }

}
