package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilPanoramaCrawler extends CrawlerRankingKeywords {

   public BrasilPanoramaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 24;
      this.log("Página " + this.currentPage);

      String url = "https://www.panoramamoveis.com.br/buscar/" + this.keywordEncoded + "+/p." + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".row.product-list .col-sm-3");
      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         for (Element e : products) {

            String productUrl = CrawlerUtils.scrapUrl(e, ".product a", Arrays.asList("href"), "https", "www.panoramamoveis.com.br");
            String productId = getInternalId(productUrl);
            String productPid = productId;

            saveDataProduct(productId, productPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + productId + " - InternalPid: " + productPid + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit)
               break;

         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private String getInternalId(String url) {
      String id = null;
      Pattern pattern = Pattern.compile("\\-(.[0-9]*)\\.html");
      Matcher matcher = pattern.matcher(url);
      if (matcher.find()) {
         id = matcher.group(1);
      }
      return id;
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".list-head .summary", true);
      this.log("Total da busca: " + this.totalProducts);

   }
}
