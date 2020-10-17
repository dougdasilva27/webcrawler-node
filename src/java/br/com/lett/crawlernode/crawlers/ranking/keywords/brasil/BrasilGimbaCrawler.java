package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilGimbaCrawler extends CrawlerRankingKeywords {
   public BrasilGimbaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {

      this.pageSize = 21;

      this.log("Página " + this.currentPage);

      String url = "https://www.gimba.com.br/?txt-busca="+this.keywordWithoutAccents+"&btn-buscar=";

      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".lst-vitrine.clearfix li[id]");

      if (products.size() >= 1) {

         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {

               String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "ul li", "data-erp");
               String urlProduct = CrawlerUtils.scrapUrl(e, "li[data-name] a", "href", "http:", "www.gimba.com.br");
               saveDataProduct(internalId, null, urlProduct);

               this.log("Position: " + this.position + " - InternalId: " + internalId
                  + " - InternalPid: " + null + " - Url: " + urlProduct);

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
      Element totalElement = this.currentDoc.selectFirst(".total-buscas .red");

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
