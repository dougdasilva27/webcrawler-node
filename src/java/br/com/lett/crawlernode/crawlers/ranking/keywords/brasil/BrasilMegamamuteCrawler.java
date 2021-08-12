package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilMegamamuteCrawler extends CrawlerRankingKeywords {

   public BrasilMegamamuteCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      //número de produtos por página do market
      this.pageSize = 16;

      this.log("Página " + this.currentPage);

      //monta a url com a keyword e a página
      String url = "https://www.megamamute.com.br/pesquisa?t=" + this.keywordEncoded + "#/pagina-" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      //chama função de pegar a url
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".wd-browsing-grid-list ul li");

      //se obter 1 ou mais links de produtos e essa página tiver resultado faça:
      if (products.size() >= 1) {

         for (Element e : products) {
            // InternalPid
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div[data-product-id]", "data-product-id");


            // Url do produto
            String urlProduct = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a", "href");
            String completeUrl = CrawlerUtils.completeUrl(urlProduct, "https", "www.megamamute.com.br");

            saveDataProduct(null, internalPid, completeUrl);

            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + completeUrl);
            if (this.arrayProducts.size() == productsLimit) break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }
}

