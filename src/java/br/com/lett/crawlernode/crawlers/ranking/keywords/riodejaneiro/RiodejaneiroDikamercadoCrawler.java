package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;


import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class RiodejaneiroDikamercadoCrawler extends CrawlerRankingKeywords {

   public RiodejaneiroDikamercadoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 20;
      this.log("Página " + this.currentPage);

      String url = "https://dikamercado.com.br/loja/busca/?q=" + this.keywordEncoded + "&page=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".col-xl-3");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts(this.currentDoc);
         }
         for (Element e : products) {
            String infoProduct = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".media a", "href");

            if (infoProduct != null) {
               String internalId = getProductId(infoProduct);
               String productUrl = CrawlerUtils.completeUrl(infoProduct, "https", "dikamercado.com.br");

               saveDataProduct(internalId, null, productUrl);

               this.log(
                  "Position: " + this.position +
                     " - InternalId: " + internalId +
                     " - InternalPid: " + null +
                     " - Url: " + productUrl);

               if (this.arrayProducts.size() == productsLimit) {
                  break;
               }
            }

         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");

   }

   private String getProductId(String infoProduct) {
      String[] url = infoProduct.split("-");
      return url[url.length - 1].split("/")[0];
   }

   private void setTotalProducts(Document doc) {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(doc, ".col-sm-8 h1", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }
}
