package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import java.util.HashMap;
import java.util.Map;

import br.com.lett.crawlernode.util.MathUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.common.net.HttpHeaders;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.crawlers.extractionutils.core.B2WCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public abstract class B2WCrawlerRanking extends CrawlerRankingKeywords {

   public B2WCrawlerRanking(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
      this.setHeaders();
   }

   protected Map<String, String> headers = new HashMap<>();
   private final String storeName = getStoreName();

   protected abstract String getStoreName();

   protected void setHeaders() {
      headers.put(HttpHeaders.REFERER, "https://www." + storeName + ".com.br");
      headers.put(
            HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3"
      );
      headers.put(HttpHeaders.CACHE_CONTROL, "max-age=0");
      headers.put(HttpHeaders.CONNECTION, "keep-alive");
      headers.put(HttpHeaders.ACCEPT_LANGUAGE, "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
      headers.put(HttpHeaders.ACCEPT_ENCODING, "");
      headers.put("Upgrade-Insecure-Requests", "1");
      headers.put("sec-fetch-mode", "navigate");
      headers.put("sec-fetch-user", "?1");
      headers.put("sec-fetch-site", "none");
   }


   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 24;

      this.log("Página " + this.currentPage);
      String url = "https://www." + storeName + ".com.br/busca/" + this.keywordWithoutAccents.replace(" ", "%20")
            + "?limite=24&offset=" + this.arrayProducts.size();
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = Jsoup.parse(B2WCrawler.fetchPage(url, this.dataFetcher, cookies, headers, session));

      Elements products = this.currentDoc.select(".iFeuoP a");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String productUrl = CrawlerUtils.completeUrl(e.attr("href").split("\\?")[0], "https", "www." + storeName + ".com.br");
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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   private String scrapInternalPid(String url) {
      return CommonMethods.getLast(url.split("produto/")).split("/")[0].split("\\?")[0];
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = MathUtils.parseInt(currentDoc.select(".jGRTBJ").text());
      this.log("Total da busca: " + this.totalProducts);
   }
}
