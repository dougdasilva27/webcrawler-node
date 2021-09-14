package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilPetzCrawler extends CrawlerRankingKeywords {

   public BrasilPetzCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.APACHE;
   }

   private static final String HOME_PAGE = "https://www.petz.com.br/";

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 18;

      this.log("Página " + this.currentPage);

      // monta a url com a keyword e a página
      String url = "https://www.petz.com.br/busca?q=" + this.keywordEncoded + "&page=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".liProduct .petzProduct");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalPid = e.attr("data-idproduto");
            String productUrl = CrawlerUtils.completeUrl(e.attr("href"), "https", "www.petz.com.br");

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
      Element totalElement = this.currentDoc.select("#paginas a").first();

      if (totalElement != null) {
         String[] parameters = totalElement.attr("href").split("&");

         for (String parameter : parameters) {
            if (parameter.startsWith("total=")) {
               String total = parameter.split("=")[1].replaceAll("[^0-9]", "").trim();

               if (!total.isEmpty()) {
                  this.totalProducts = Integer.parseInt(total);
               }

               break;
            }
         }

         this.log("Total da busca: " + this.totalProducts);
      }
   }

}
