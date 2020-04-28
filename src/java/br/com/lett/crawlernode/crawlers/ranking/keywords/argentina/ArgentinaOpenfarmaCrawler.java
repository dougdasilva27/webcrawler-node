package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class ArgentinaOpenfarmaCrawler extends CrawlerRankingKeywords {


   public ArgentinaOpenfarmaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {

      this.pageSize = 12;

      this.log("Página " + this.currentPage);

      String url = "https://www.openfarma.com.ar/products?utf8=%E2%9C%93&keywords=" + this.keywordEncoded + "&page=" + this.currentPage + "&utf8=%E2%9C%93";


      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".col-sm-4 .card-product");

      if (products.size() >= 1) {

         if (this.totalProducts == 0) setTotalProducts();
         for (Element e : products) {

            String internalPid = null;

            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "#variant_id", "value");

            String incompleteProductUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a", "href");
            String productUrl = CrawlerUtils.completeUrl(incompleteProductUrl, "http://", "www.openfarma.com.ar");
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

   @Override
   protected boolean hasNextPage() {
      return this.currentDoc.selectFirst(".pagination .page:nth-child(2)") != null;
   }

}
