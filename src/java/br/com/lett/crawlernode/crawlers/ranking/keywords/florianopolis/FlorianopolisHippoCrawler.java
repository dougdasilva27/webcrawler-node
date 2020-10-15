package br.com.lett.crawlernode.crawlers.ranking.keywords.florianopolis;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class FlorianopolisHippoCrawler extends CrawlerRankingKeywords {

   private Elements id;

   public FlorianopolisHippoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {

      this.log("Página " + this.currentPage);
      String url = "http://www.hippo.com.br/produtos/?busca=" + this.keywordEncoded + "&bt_buscar=";

      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".product-block .imagem a");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalPid = e.attr("data-codigo");
            String internalId = e.attr("data-id");
            String productUrl  = e.attr("data-path");

            saveDataProduct(internalId, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalId + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit)
               break;

         }
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return false;
   }

   @Override
   protected void setTotalProducts() {
      Element totalElement = this.currentDoc.select("span.total_count strong").first();

      if (totalElement != null) {
         try {
            int x = totalElement.text().indexOf("ite");

            String token = totalElement.text().substring(0, x).trim();

            this.totalProducts = Integer.parseInt(token);
         } catch (Exception e) {
            this.logError(e.getMessage());
         }

         this.log("Total da busca: " + this.totalProducts);
      }
   }
}
