package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilLebesCrawler extends CrawlerRankingKeywords {

   public BrasilLebesCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);

      // número de produtos por página do market
      this.pageSize = 16;
      String keyword = this.keywordWithoutAccents.replace(" ", "%20");

      // monta a url com a keyword e a página
      String url = "https://www.lebes.com.br/" + keyword + "?PageNumber=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);

      // chama função de pegar a url
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".prateleira.principal ul li .data");

      // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
      if (products.size() >= 1) {
         // se o total de busca não foi setado ainda, chama a função para setar
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {

            String internalPid = CrawlerUtils.scrapStringSimpleInfo(e, ".id-produto", false);
            String urlProduct = CrawlerUtils.scrapUrl(e,".productImage", "href", "https:", "https://www.lebes.com.br/");

            saveDataProduct(null, internalPid, urlProduct);

            this.log("Position: " + this.position + " - InternalId: " + null
               + " - InternalPid: " + internalPid + " - Url: " + urlProduct);

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
   protected void setTotalProducts() {
      Element totalElement = this.currentDoc.select("span.resultado-busca-numero span.value").first();

      try {
         if (totalElement != null) {
            this.totalProducts = Integer.parseInt(totalElement.text());
         }
      } catch (Exception e) {
         this.logError(e.getMessage());
      }

      this.log("Total da busca: " + this.totalProducts);
   }
}
