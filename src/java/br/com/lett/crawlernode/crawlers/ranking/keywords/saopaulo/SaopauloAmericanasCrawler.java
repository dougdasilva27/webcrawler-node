package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.B2WCrawler;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.B2WCrawlerRanking;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class SaopauloAmericanasCrawler extends B2WCrawlerRanking {

   public SaopauloAmericanasCrawler(Session session) {
      super(session);
   }

   private static final String selector = "div[class*=ColGridItem-] a";

   @Override
   protected String getStoreName() {
      return "americanas";
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 24;

      this.log("Página " + this.currentPage);
      String url = "https://www." + getStoreName() + ".com.br/busca/" + this.keywordWithoutAccents.replace(" ", "%20")
            + "?limit=24&offset=" + (this.currentPage - 1) * pageSize;

      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = Jsoup.parse(B2WCrawler.fetchPage(url, this.dataFetcher, cookies, headers, session));

      Elements products = this.currentDoc.select(selector);

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapUrl(e, "a", "href", "https", "www." + getStoreName() + ".com.br");

            if (productUrl.contains("?")) {
               productUrl = productUrl.split("\\?")[0];
            }

            String internalPid = scrapInternalPid(productUrl);

            saveDataProduct(null, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
   }

   private String scrapInternalPid(String url) {
      return CommonMethods.getLast(url.split("produto/")).split("/")[0];
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, "span[class*=TotalText]", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }

}
