package br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilMinhacooper extends CrawlerRankingKeywords {

   public BrasilMinhacooper(Session session) {
      super(session);
   }

   private String store_name;

   public void setStore_name(String store_name) {
      this.store_name = store_name;
   }

   @Override
   protected void extractProductsFromCurrentPage() {

      this.log("Página " + this.currentPage);

      String url = "https://www.minhacooper.com.br/loja/" + this.store_name + "/produto/busca?q=" + this.keywordWithoutAccents + "&page=" + this.currentPage;

      this.currentDoc = fetchDocument(url, cookies);

      Elements products = this.currentDoc.select(".product-list-item");

      if (!products.isEmpty()) {

         for (Element e : products) {

            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-variation .product-variation__actions .product-actions", "data-variant-id");
            String productUrl = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-variation__image-container", "href"), "https://", "www.minhacooper.com.br");

            saveDataProduct(null, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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
      return this.currentDoc.selectFirst(".pagination.pull-right li:not(:first-child)  a") != null;

   }

}
