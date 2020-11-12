package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilMultiarCrawler extends CrawlerRankingKeywords {

   public BrasilMultiarCrawler(Session session) {
      super(session);
   }


   @Override
   protected void extractProductsFromCurrentPage() {
      // número de produtos por página do market
      this.pageSize = 24;

      this.log("Página " + this.currentPage);

      String key = this.keywordWithoutAccents.replaceAll(" ", "%20");

      // monta a url com a keyword e a página
      String url = "https://www.leveros.com.br/" + key + "#" +this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);

      // chama função de pegar a url
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("div.prateleira.vitrine > ul > li[layout]");

      // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
      if (!products.isEmpty()) {
         for (Element e: products) {

            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, " .spot", "data-id");
            String productUrl = CrawlerUtils.scrapUrl(e, ".spot .spot-img", "href", "https:", "www.leveros.com.br");

            saveDataProduct(null, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit)
               break;

         }
      } else {
         setTotalProducts();
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
      if (!(hasNextPage()))
         setTotalProducts();
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".resultado-busca-numero span[class=\"value\"]", null, null, false, false, 0);
      this.log("Total: " + this.totalProducts);

   }

}
