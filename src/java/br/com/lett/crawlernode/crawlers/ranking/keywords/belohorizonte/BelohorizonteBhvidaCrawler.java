package br.com.lett.crawlernode.crawlers.ranking.keywords.belohorizonte;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BelohorizonteBhvidaCrawler extends CrawlerRankingKeywords {

   public BelohorizonteBhvidaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {

      //For some reason this website loads only one page with all the products. I cannot find any search term with pagination.
      this.log("Página " + this.currentPage);

      String url = "https://www.bhvida.com/?LISTA=procurar&PROCURAR=" + this.keywordEncoded;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("div.row.list div.grid-item");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0){
            setTotalProducts();
         }

         for (Element e : products) {
            String productUrl = "https://www.bhvida.com/" + CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a.block2-name", "href");
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div.grid-item", "id");

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

   }

   @Override
   protected void setTotalProducts() {
      Elements products = this.currentDoc.select("div.row.list div.grid-item");
      this.totalProducts = products.size();
      this.log("Total da busca: " + this.totalProducts);
   }
}
