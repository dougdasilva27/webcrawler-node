package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilAgrosoloCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "www.agrosolo.com.br";

   public BrasilAgrosoloCrawler(Session session) {
      super(session);
      dataFetcher = new JsoupDataFetcher();
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 24;
      this.log("Página " + this.currentPage);

      String url = "https://www.agrosolo.com.br/busca?busca=" + this.keywordEncoded + "&pagina=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select("div.produtos div.fbits-item-lista-spot ");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".spot", "id").replace("produto-spot-item-", "") + "-0";
            String productUrl = CrawlerUtils.scrapUrl(e, ".spot div.spotContent > a", "href", "https", HOME_PAGE);

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
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".total-items .number", null, "iten", false, true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }
}
