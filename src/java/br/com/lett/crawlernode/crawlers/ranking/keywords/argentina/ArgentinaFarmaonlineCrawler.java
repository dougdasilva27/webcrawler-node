package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Date: 22/01/21
 *
 * @author Fellype Layunne
 */
public class ArgentinaFarmaonlineCrawler  extends CrawlerRankingKeywords  {

   public ArgentinaFarmaonlineCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   public String getHomePage() {
      return br.com.lett.crawlernode.crawlers.corecontent.argentina.ArgentinaFarmaonlineCrawler.HOME_PAGE;
   }

   @Override
   protected void extractProductsFromCurrentPage() {

      if (this.currentPage == 1) {
         setTotalProducts();
      }

      this.pageSize = 48;
      String url = getHomePage() + "buscapagina" +
         "?ft=" + keywordEncoded +
         "&PS=" + this.pageSize +
         "&cc=" + this.pageSize +
         "&sl=ef3fcb99-de72-4251-aa57-71fe5b6e149f" +
         "&sm=0" +
         "&PageNumber=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".prateleira ul li span[data-id]");

      if (!products.isEmpty()) {

         for (Element e : products) {
            String internalPid = e.attr("data-id");
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a", "href");

            saveDataProduct(null, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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

   @Override
   protected void setTotalProducts() {
      String url = getHomePage() + keywordWithoutAccents.replace(" ", "%20");
      Document doc = fetchDocument(url);

      totalProducts =  CrawlerUtils.scrapSimpleInteger(doc, ".resultado-busca-numero .value", false, 0);
   }
}
