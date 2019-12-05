package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class SaopauloSupermercadodospetsCrawler extends CrawlerRankingKeywords {

   public SaopauloSupermercadodospetsCrawler(Session session) {
      super(session);
   }

   private static final String HOST = "www.supermercadodospets.com.br";

   @Override
   protected void extractProductsFromCurrentPage() {

      this.pageSize = 40;
      this.log("Página " + this.currentPage);

      String url = "https://" + HOST + "/catalogsearch/result/?q=" + this.keywordEncoded + "&pagina=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".col-main .item-area");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalPid = scrapId(e);
            String productUrl = CrawlerUtils.scrapUrl(e, ".item-area .product-image-area > a", "href", "https", HOST);

            saveDataProduct(null, internalPid, productUrl);

            this.log(
                  "Position: " + this.position +
                        " - InternalPid: " + internalPid +
                        " - Url: " + productUrl
            );

            if (this.arrayProducts.size() == productsLimit)
               break;
         }

      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");


   }


   private String scrapId(Element e) {
      String pid = null;

      String att = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".price-box .regular-price", "id");
      if (att != null) {
         pid = CommonMethods.getLast(att.split("-"));
      }

      return pid;
   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select("li:not(.disabled) a[rel=next]").isEmpty();
   }
}
