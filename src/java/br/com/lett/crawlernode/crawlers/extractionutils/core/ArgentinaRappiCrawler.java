package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import com.google.common.net.HttpHeaders;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArgentinaRappiCrawler extends RappiCrawler {

   private static final String HOME_PAGE = "https://www.rappi.com";

   public ArgentinaRappiCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
   }

   @Override
   protected String getHomeDomain() {
      return "rappi.com.ar";
   }

   @Override
   protected String getImagePrefix() {
      return "images.rappi.com.ar/products";
   }

   @Override
   protected String getUrlPrefix() {
      return "producto/";
   }

   @Override
   protected String getHomeCountry() {
      return "https://www.rappi.com.ar/";
   }

   @Override
   protected String getMarketBaseUrl() {
      return "https://www.rappi.com.ar/tiendas/";
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   protected Object fetch() {
      Request request = Request.RequestBuilder.create()
         .setCookies(cookies)
         .setProxyservice(List.of(
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY))
         .setTimeout(10000)
         .setUrl(session.getOriginalURL())
         .build();

      Response response = CrawlerUtils.retryRequest(request, session, new ApacheDataFetcher(), true);

      return Jsoup.parse(response.getBody());
   }

//   @Override
//   protected String fetchToken() {
//      String url = "https://services." + getHomeDomain() + "/api/auth/guest_access_token";
//
//      Map<String, String> headers = new HashMap<>();
//      headers.put("accept", "application/json, text/plain, */*");
//      headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36");
//      headers.put("content-type", "application/json");
//
//      String payload = "{\"headers\":{\"normalizedNames\":{},\"lazyUpdate\":null},\"grant_type\":\"guest\"}";
//
//      Request request = Request.RequestBuilder.create()
//         .setUrl(url)
//         .setHeaders(headers)
//         .setPayload(payload)
//         .mustSendContentEncoding(false)
//         .setProxyservice(List.of(
//            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
//            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
//            ProxyCollection.BUY_HAPROXY))
//         .setTimeout(10000)
//         .build();
//
//      JSONObject json = JSONUtils.stringToJson(this.dataFetcher.post(session, request).getBody());
//
//      String token = json.optString("access_token");
//      String tokenType = json.optString("token_type");
//
//      if (tokenType.equals("Bearer")) {
//         token = tokenType + " " + token;
//      }
//
//      return token;
//   }
//
//   @Override
//   protected JSONObject fetchProductApi(String productId, String token) {
//      String url = "https://services." + getHomeDomain() + "/api/ms/web-proxy/dynamic-list/cpgs";
//
//      Map<String, String> headers = new HashMap<>();
//      headers.put("accept", "application/json, text/plain, */*");
//      headers.put("language", "pt");
//      headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36");
//      headers.put("content-type", "application/json");
//      headers.put("authorization", token);
//
//      String productFriendlyUrl = getStoreId() + "_" + productId;
//
//      String payload = "{\"dynamic_list_request\":{\"context\":\"product_detail\",\"state\":{\"lat\":\"1\",\"lng\":\"1\"},\"limit\":100,\"offset\":0},\"dynamic_list_endpoint\":\"context/content\",\"proxy_input\":{\"product_friendly_url\":\"" + productFriendlyUrl + "\"}}";
//
//
//      Request request = Request.RequestBuilder.create()
//         .setUrl(url)
//         .setHeaders(headers)
//         .setPayload(payload)
//         .setProxyservice(List.of(ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY))
//         .setTimeout(20000)
//         .build();
//
//      String body = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new FetcherDataFetcher(), new JsoupDataFetcher(), new ApacheDataFetcher()), session, "post").getBody();
//
//      JSONObject jsonObject = CrawlerUtils.stringToJSONObject(body);
//
//      return JSONUtils.getValueRecursive(jsonObject, "dynamic_list_response.data.components.0.resource.product", ".", JSONObject.class, new JSONObject());
//   }
}
