package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilColomboCrawler extends CrawlerRankingKeywords {

   public BrasilColomboCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      // número de produtos por página do market
      this.pageSize = 20;

      this.log("Página " + this.currentPage);

      // monta a url com a keyword e a página
      String url = "https://pesquisa.colombo.com.br/busca?q=" + this.keywordEncoded + "&televendas=&page=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".neemu-products-container .nm-product-item");

      // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
      if (!products.isEmpty()) {
         for (Element e : products) {
            // InternalPid
            String internalPid = e.attr("id").split("-")[2];

            // Url do produto
            String urlProduct = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".nm-product-info", "href"), "http:", "www.colombo.com");

            saveDataProduct(null, internalPid, urlProduct);

            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
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
   protected boolean hasNextPage() {
      return this.currentDoc.select(".neemu-pagination .neemu-pagination-inner a") != null;
   }

}
