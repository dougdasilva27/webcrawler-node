package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

public class RiodejaneiroDrogariavenancioCrawler extends CrawlerRankingKeywords {

   private static final String HOST = "www.drogariavenancio.com.br";

   public RiodejaneiroDrogariavenancioCrawler(Session session) {
      super(session);
   }

   @Override
   protected void processBeforeFetch() {
      super.processBeforeFetch();

      Logging.printLogDebug(logger, session, "Adding cookie...");

      BasicClientCookie cookie = new BasicClientCookie("VTEXSC", "sc=1");
      cookie.setDomain(".drogariavenancio.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   protected void extractProductsFromCurrentPage() {
      this.pageSize = 12;

      this.log("Página " + this.currentPage);

      String url = "https://" + HOST + "/busca?ft=" + this.keywordEncoded + "&pg=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".shelf-product");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalId = null;
            String internalPid = CrawlerUtils.scrapStringSimpleInfo(e, "data-product-id", true);
            String productUrl = CrawlerUtils.scrapUrl(e, "figure.shelf-product__container .shelf-product__image a", "href", "https", HOST);

            saveDataProduct(null, internalPid, productUrl);

            this.log(
                  "Position: " + this.position +
                        " - InternalId: " + internalId +
                        " - InternalPid: " + internalPid +
                        " - Url: " + productUrl
            );

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
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, ".resultado-busca-numero .value", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }
}
