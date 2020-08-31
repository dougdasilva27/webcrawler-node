package br.com.lett.crawlernode.crawlers.ranking.keywords.romania;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class RomaniaEmagCrawler extends CrawlerRankingKeywords {

   public RomaniaEmagCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);

      this.pageSize = 24;

      String url = "https://www.emag.ro/search/" + this.keywordWithoutAccents + "/p" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url, cookies);

      Elements products = this.currentDoc.select(".card-section-wrapper");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0)
            setTotalProducts();

         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".card-toolbox .add-to-favorites.btn.btn-lg", "dataproductid");
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".card-section-wrapper .card-heading a", "href");

            saveDataProduct(internalId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
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
      this.totalProducts =
         MathUtils.parseInt(
            currentDoc.selectFirst(".title-phrasing.title-phrasing-sm").text());
      this.log("Total da busca: " + this.totalProducts);
   }
 
}
