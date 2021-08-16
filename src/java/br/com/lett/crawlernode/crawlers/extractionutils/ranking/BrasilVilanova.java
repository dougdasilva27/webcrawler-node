package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public abstract class BrasilVilanova extends CrawlerRankingKeywords {

   public BrasilVilanova(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);

      this.pageSize = 24;
      String url = "https://www.vilanova.com.br/Busca/Resultado/?p="
         + this.currentPage
         + "&loja=&q="
         + this.keywordEncoded
         + "&ordenacao=6&limit=24";

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".shelf-content-items div.shelf-item");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "button.btn-primary", "id");
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "p.product-name > a", "href");

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

      this.log(
         "Finalizando Crawler de produtos da página "
            + this.currentPage
            + " - até agora "
            + this.arrayProducts.size()
            + " produtos crawleados");
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts =
         CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".qtd-produtos", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }
}
