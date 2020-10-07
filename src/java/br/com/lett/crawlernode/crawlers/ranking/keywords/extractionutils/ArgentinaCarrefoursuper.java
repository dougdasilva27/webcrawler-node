package br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public abstract class ArgentinaCarrefoursuper extends CrawlerRankingKeywords {

   public ArgentinaCarrefoursuper(Session session) {
      super(session);
      super.fetchMode = FetchMode.APACHE;
   }

   private static final String PRODUCTS_SELECTOR = ".home-product-cards .product-card .producto-info .open-modal[title]";
   private static final String HOST = "supermercado.carrefour.com.ar";
   private String categoryUrl;

   /**
    * This function might return a cep from specific store
    * 
    * @return
    */
   protected abstract String getCep();

   @Override
   public void processBeforeFetch() {
      super.processBeforeFetch();

      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");

      String payload = "codigo_postal=" + getCep();

      Request request = RequestBuilder.create()
            .setUrl("https://supermercado.carrefour.com.ar/stock/")
            .setCookies(cookies)
            .setPayload(payload)
            .setHeaders(headers)
            .setProxyservice(Arrays.asList(ProxyCollection.INFATICA_RESIDENTIAL_BR_HAPROXY))
            .setStatusCodesToIgnore(Arrays.asList(302))
            .setFollowRedirects(false)
            .setBodyIsRequired(false)
            .mustSendContentEncoding(false)
            .build();

      List<Cookie> cookiesResponse = new FetcherDataFetcher().post(session, request).getCookies();
      for (Cookie c : cookiesResponse) {
         BasicClientCookie cookie = new BasicClientCookie(c.getName(), c.getValue());
         cookie.setDomain(HOST);
         cookie.setPath("/");
         this.cookies.add(cookie);
      }
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 10;
      this.log("Página " + this.currentPage);

      String url = "https://" + HOST + "/catalogsearch/result/?q=" + this.keywordEncoded + "&p=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(PRODUCTS_SELECTOR);

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProductsCarrefour();
         }
         for (Element e : products) {

            String internalId = e.attr("data-id");
            String productUrl = CrawlerUtils.completeUrl(e.attr("href"), "https", HOST);

            saveDataProduct(internalId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
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
      return this.categoryUrl != null ? this.currentDoc.select(PRODUCTS_SELECTOR).size() >= this.pageSize : super.hasNextPage();
   }

   protected void setTotalProductsCarrefour() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".resultados-count", false, 0);
      this.log("Total da busca: " + this.totalProducts);
   }
}
