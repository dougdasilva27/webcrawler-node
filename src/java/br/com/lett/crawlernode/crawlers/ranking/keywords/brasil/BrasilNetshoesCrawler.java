package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilNetshoesCrawler extends CrawlerRankingKeywords {

   public BrasilNetshoesCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 42;

      this.log("Página " + this.currentPage);

      String url = "https://www.netshoes.com.br/busca?nsCat=Natural&q=" + this.keywordEncoded + "&page=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("#item-list .wrapper a");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProductsCarrefour();
         }

         for (Element e : products) {
            String internalPid = crawlInternalPid(e);
            String productUrl = CrawlerUtils.scrapUrl(e, null, "href", "https:", "www.netshoes.com.br");

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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   protected void setTotalProductsCarrefour() {
      Element totalElement = this.currentDoc.select(".items-info .block").first();

      if (totalElement != null) {
         String text = totalElement.ownText();

         if (text.contains("de")) {
            String total = text.split("de")[1].replaceAll("[^0-9]", "").trim();

            if (!total.isEmpty()) {
               this.totalProducts = Integer.parseInt(total);
            }
         }

         this.log("Total da busca: " + this.totalProducts);
      }
   }

   private String crawlInternalPid(Element e) {
      return e.attr("parent-sku");
   }

}
