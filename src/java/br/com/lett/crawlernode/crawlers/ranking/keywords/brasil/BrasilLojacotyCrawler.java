package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.cookie.Cookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class BrasilLojacotyCrawler extends CrawlerRankingKeywords {

   public BrasilLojacotyCrawler(Session session) {
      super(session);
   }

   private List<Cookie> cookies = new ArrayList<>();

   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);

      this.pageSize = 24;

      String url = "https://busca.lojacoty.com.br/busca?q=" + this.keywordEncoded + "&page=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url, cookies);

      Elements products = this.currentDoc.select(".neemu-products-container li");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0)
            setTotalProducts();

         for (Element e : products) {

            String productUrl = "https:" + CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".nm-product-info a", "href");
            String internalPid = CommonMethods.getLast(productUrl.split("idsku="));

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
      return !this.currentDoc.select(".neemu-pagination-next a").isEmpty();
   }

}
