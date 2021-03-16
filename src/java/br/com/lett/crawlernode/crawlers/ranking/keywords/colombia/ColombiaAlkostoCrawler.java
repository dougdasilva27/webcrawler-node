package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;

public class ColombiaAlkostoCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "www.alkosto.com";

   public ColombiaAlkostoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 30;
      this.log("Página " + this.currentPage);

      String url = crawlUrl();

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".products-grid .item");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div.yotpo.bottomLine", "data-product-id");
            String productUrl = CrawlerUtils.scrapUrl(e, ".amlabel-div a", Arrays.asList("href"), "https", HOME_PAGE);

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


   private String crawlUrl() {
      String urlFirstPage = "https://www.alkosto.com/salesperson/result/?q=" + this.keywordEncoded;
      if (this.currentPage > 1) {
         Document doc = fetchDocument(urlFirstPage);

         return CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".pages li a", "href").replaceAll("p=2", "p=" + this.currentPage);

      } else {

         return urlFirstPage;
      }

   }

   @Override
   protected boolean hasNextPage() {
      Element page = this.currentDoc.selectFirst(".pages .next");
      return page != null;
   }
}
