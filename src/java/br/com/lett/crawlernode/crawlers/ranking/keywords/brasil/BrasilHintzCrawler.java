package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilHintzCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "loja.hintz.ind.br";

   public BrasilHintzCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {

      this.pageSize = 21;

      this.log("Página " + this.currentPage);

      String url = "https://loja.hintz.ind.br/busca/?q=" + this.keywordEncoded + "&p=2" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".produtos-index.interno .col-xxs-12.col-xs-12");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalPid = null;



            // String internalId = crawlInternalId(e);
            String productUrl = CrawlerUtils.scrapUrl(e, ".produtos-index.interno .col-xxs-12.col-xs-12 a", "href", "http://", HOME_PAGE);

            saveDataProduct("222", internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + "222" + " - InternalPid: " + internalPid + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   // private String scrapInternalId(String productUrl) {
   // String internalId = null;

   // Integer index = productUrl.indexOf("")

   // return internalId;
   // }

}
