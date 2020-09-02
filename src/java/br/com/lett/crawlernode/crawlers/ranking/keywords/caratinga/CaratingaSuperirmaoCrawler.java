package br.com.lett.crawlernode.crawlers.ranking.keywords.caratinga;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class CaratingaSuperirmaoCrawler extends CrawlerRankingKeywords {

   public CaratingaSuperirmaoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      //número de produtos por página do market
      this.pageSize = 12;

      this.log("Página " + this.currentPage);
      String url = "https://superirmao.loji.com.br/produtos?q=" + this.keywordWithoutAccents;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".row .produto");
      if (products.size() >= 1) {

         if (this.totalProducts == 0) setTotalProducts();
         for (Element e : products) {

            // InternalPid
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".btn", "href").split("add/")[1];
            // InternalId
            String internalPid = internalId;

            // Url do produto
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".col-auto a", "href");

            saveDataProduct(internalId, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit) break;

         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }
}
