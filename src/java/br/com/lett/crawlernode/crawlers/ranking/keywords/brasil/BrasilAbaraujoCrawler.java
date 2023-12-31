package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
/*
  Crawler using scraper 5170
 */
public class BrasilAbaraujoCrawler extends CrawlerRankingKeywords {

   public BrasilAbaraujoCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = "www.abaraujo.com.br";

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 12;
      this.log("Página " + this.currentPage);

      String url = "https://www.abaraujo.com/loja/busca.php?loja=808976&palavra_busca=" + this.keywordEncoded + "&pg=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".product.product-1");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "meta[itemprop=\"productID\"]", "content");
            String productUrl = CrawlerUtils.scrapUrl(e, "a", "href", "http:", HOME_PAGE);

            saveDataProduct(null, internalPid, productUrl);

            this.log(
               "Position: " + this.position +
                  " - InternalId: " + null +
                  " - InternalPid: " + internalPid +
                  " - Url: " + productUrl);

            if (this.arrayProducts.size() == productsLimit)
               break;
         }

      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected void setTotalProducts(){

      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".catalogo-pagination strong", true, 0);
      this.log("Total: " + this.totalProducts);
   }
}
