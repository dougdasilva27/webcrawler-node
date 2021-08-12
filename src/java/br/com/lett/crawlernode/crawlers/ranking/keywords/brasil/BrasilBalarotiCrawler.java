package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilBalarotiCrawler extends CrawlerRankingKeywords {

   public BrasilBalarotiCrawler(Session session) {
      super(session);
   }

   private boolean hasNextPage = true;

   @Override
   public void extractProductsFromCurrentPage() {

      this.pageSize = 16;

      String url = "https://busca.balaroti.com.br/busca?q=" + this.keywordEncoded;

      this.log("Página " + this.currentPage);

      Document doc = fetchDocument(url);

      Elements products = doc.select(".neemu-products-container li");

      if (products.size() > 0) {
         for (Element product : products) {

            String productUrl = "https:" + CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "a", "href");

            String internalPid = crawlInternalPid(productUrl);

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

   private String crawlInternalPid(String productUrl) {
      Pattern p = Pattern.compile("-([0-9]*)\\/p");
      Matcher m = p.matcher(productUrl);
      if (m.find()) {
         return m.group(1);
      }
      return null;
   }
}
