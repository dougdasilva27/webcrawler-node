package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilMagazineluizaCrawler extends CrawlerRankingKeywords {

   public BrasilMagazineluizaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 60;
      this.log("Página " + this.currentPage);

      String url = "https://www.magazineluiza.com.br/busca/" + keywordEncoded + "/" + this.currentPage +"/";
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".productShowCase.big li");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {

            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "[itemprop='productID']", "content");
            String urlProduct = CrawlerUtils.scrapUrl(e, ".product a", "href", "https", "www.magazineluiza.com.br");

            saveDataProduct(internalId, null, urlProduct);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + urlProduct);
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      if (!hasNextPage() && this.arrayProducts.size() > this.totalProducts) {
         this.totalProducts = this.arrayProducts.size();
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, "[itemprop='description'] small", null, null, false, true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }
}
