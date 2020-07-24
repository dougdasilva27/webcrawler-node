package br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils;

import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.FetchUtilities;
import br.com.lett.crawlernode.core.fetcher.models.LettProxy;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public abstract class ComperCrawlerRanking extends CrawlerRankingKeywords {

   public ComperCrawlerRanking(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   protected final String storeId = getStoreId();
   private static final String HOME_PAGE = "https://www.comperdelivery.com.br/";
   private String userAgent;
   private LettProxy proxyUsed;

   protected abstract String getStoreId();

   @Override
   protected void processBeforeFetch() {
      this.userAgent = FetchUtilities.randUserAgent();

      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.USER_AGENT, this.userAgent);

      Request request = RequestBuilder.create().setUrl(HOME_PAGE).setCookies(cookies).setHeaders(headers).build();
      Response response = this.dataFetcher.get(session, request);

      this.proxyUsed = response.getProxyUsed();

      for (Cookie cookieResponse : response.getCookies()) {
         BasicClientCookie cookie = new BasicClientCookie(cookieResponse.getName(), cookieResponse.getValue());
         cookie.setDomain("www.comperdelivery.com.br");
         cookie.setPath("/");
         this.cookies.add(cookie);
      }

      Request request2 = RequestBuilder.create()
            .setUrl("https://www.comperdelivery.com.br/store/SetStore?storeId=" + storeId)
            .setCookies(cookies)
            .setHeaders(headers)
            .build();
      Response response2 = this.dataFetcher.get(session, request2);

      for (Cookie cookieResponse : response2.getCookies()) {
         BasicClientCookie cookie = new BasicClientCookie(cookieResponse.getName(), cookieResponse.getValue());
         cookie.setDomain("www.comperdelivery.com.br");
         cookie.setPath("/");
         this.cookies.add(cookie);
      }
   }

   @Override
   public void extractProductsFromCurrentPage() {
      // número de produtos por página do market
      this.pageSize = 20;

      this.log("Página " + this.currentPage);

      String url = "https://www.comperdelivery.com.br/" + this.keywordEncoded + "#" + this.currentPage;

      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.USER_AGENT, this.userAgent);

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).setProxy(proxyUsed).build();
      this.currentDoc = Jsoup.parse(this.dataFetcher.get(session, request).getBody());
      this.log("Link onde são feitos os crawlers: " + url);

      Elements products = this.currentDoc.select(".main-shelf .main-shelf ul .shelf-item");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String productUrl = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a", "href"), "http://", "www.comperdelivery.com.br");
            String internalId = e.attr("data-product-id");

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
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, ".resultado-busca-numero .value", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }

}
