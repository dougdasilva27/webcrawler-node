package br.com.lett.crawlernode.crawlers.ranking.keywords.curitiba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CuritibaSchummancuritibaCrawler extends CrawlerRankingKeywords {

   public CuritibaSchummancuritibaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 15;
      this.log("Página " + this.currentPage);

      String url = "https://www.estudionetshop.com.br/busca/" + this.keywordEncoded + "/page/" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".card-subproduct");
      if (!products.isEmpty()) {
         for (Element e : products) {

            String productUrl = CrawlerUtils.scrapUrl(e, ".mat-ripple.csb-wrapper", Arrays.asList("href"), "https", "estudionetshop.com.br");
            String productPid = getProductPid(productUrl);

            saveDataProduct(null, productPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + productPid + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit)
               break;

         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private String getProductPid(String url) {
      String id = null;
      Pattern pattern = Pattern.compile("\\/(.[0-9]*)\\/");
      Matcher matcher = pattern.matcher(url);
      if (matcher.find()) {
         id = matcher.group(1);
      }
      return id;
   }

   @Override
   protected boolean hasNextPage() {
      return !currentDoc.select("li.pagination-next a").isEmpty();
   }
}
