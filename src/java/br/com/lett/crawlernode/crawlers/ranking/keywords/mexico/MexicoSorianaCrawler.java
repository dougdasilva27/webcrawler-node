package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

public class MexicoSorianaCrawler extends CrawlerRankingKeywords {

   public MexicoSorianaCrawler(Session session) {
      super(session);
      fetchMode = FetchMode.FETCHER;
   }

   @Override
   protected void processBeforeFetch() {
      super.processBeforeFetch();
      Request request = Request.RequestBuilder.create().setUrl("https://www.soriana.com/").build();
      Response response = this.dataFetcher.get(session, request);

      this.cookies = response.getCookies();
   }

   @Override
   protected Document fetchDocument(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("Accept-Encoding","gzip, deflate, br");
      headers.put("Accept","*/*");
      headers.put("cookie",CommonMethods.cookiesToString(this.cookies));

      Request request = Request.RequestBuilder.create().setUrl(url).setHeaders(headers).build();

      String content = this.dataFetcher.get(session, request).getBody();

      return Jsoup.parse(content);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 12;

      this.log("Página " + this.currentPage);

      String url = "https://www.soriana.com/on/demandware.store/Sites-Soriana-Site/default/Search-UpdateGrid?q=" + this.keywordEncoded + "&pmin=0.01&start=" + (this.currentPage - 1) * 12 + "&sz=12&selectedUrl=https://www.soriana.com/on/demandware.store/Sites-Soriana-Site/default/Search-UpdateGrid?q=" + this.keywordEncoded + "&pmin=0%2e01&start=" + (this.currentPage - 1) * 12 + "&sz=12";
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".product-tile--wrapper.d-flex");

      if (products.size() >= 1) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {

            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product", "data-pid");
            String productUrl = CrawlerUtils.scrapUrl(e, ".image-container a", "href", "https", "www.soriana.com");

            saveDataProduct(internalId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
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

   @Override
   protected boolean hasNextPage() {
      return true;

   }

}
