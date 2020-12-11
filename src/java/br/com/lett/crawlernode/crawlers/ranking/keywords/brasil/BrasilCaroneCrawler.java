package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;

public class BrasilCaroneCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "carone.com.br";

   public BrasilCaroneCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 48;
      this.log("Página " + this.currentPage);

      String url = crawlUrl();

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select("div.category-products > ul > li");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts(this.currentDoc);
         }
         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".add-to-list a", "data-id");
            String productUrl = CrawlerUtils.scrapUrl(e, ".product-image a", Arrays.asList("href"), "https:", HOME_PAGE);

            saveDataProduct(internalId, null, productUrl);

            this.log(
               "Position: " + this.position +
                  " - InternalId: " + internalId +
                  " - InternalPid: " + null +
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
      if (this.currentPage == 1) {
         link = "https://www.carone.com.br/catalogsearch/result/?q=" + this.keywordEncoded;
      } else {
         link = "https://www.carone.com.br/search/page/" + this.currentPage + "?q=" + this.keywordEncoded;
      }
      return link;
   }

   private void setTotalProducts(Document doc) {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(doc, ".amount .show > span", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }
}
