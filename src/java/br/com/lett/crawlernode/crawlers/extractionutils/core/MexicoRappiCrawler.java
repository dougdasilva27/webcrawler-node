package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MexicoRappiCrawler extends RappiCrawler {

   private static final String HOME_PAGE = "https://www.rappi.com.mx/";
   private static final String API_BASE_URL = "mxgrability.rappi.com";

   @Override
   protected String getHomeDomain() {
      return "rappi.com.mx";
   }

   @Override
   protected String getImagePrefix() {
      return "images.rappi.com.mx/products";
   }

   @Override
   protected String getUrlPrefix() {
      return "producto/";
   }

   @Override
   protected String getHomeCountry() {
      return HOME_PAGE;
   }

   @Override
   protected String getMarketBaseUrl() {
      return "https://www.rappi.com.mx/tiendas/";
   }

   public MexicoRappiCrawler(Session session) {
      super(session);
      this.config.setFetcher(FetchMode.APACHE);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   private String fetchPassportToken() {
      String url = "https://services." + API_BASE_URL + "/api/rocket/v2/guest/passport/";

      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "application/json, text/plain, */*");
      headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36");
      headers.put("content-type", "application/json");
      headers.put("deviceid", "4d848bd9-903e-411d-a3f2-86906184dd84");
      headers.put("needAppsFlyerId", "false");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(List.of(
            ProxyCollection.NETNUT_RESIDENTIAL_MX,
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.SMART_PROXY_MX_HAPROXY))
         .setTimeout(10000)
         .build();

      JSONObject json = JSONUtils.stringToJson(CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher(), true).getBody());


      return json.optString("token");
   }

   @Override
   protected String fetchToken() {
      String url = "https://services." + API_BASE_URL + "/api/rocket/v2/guest";

      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "application/json, text/plain, */*");
      headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36");
      headers.put("content-type", "application/json");
      headers.put("x-guest-api-key", fetchPassportToken());
      headers.put("deviceid", "4d848bd9-903e-411d-a3f2-86906184dd84");
      headers.put("needAppsFlyerId", "false");

      String payload = "{}";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .setProxyservice(List.of(
            ProxyCollection.NETNUT_RESIDENTIAL_MX,
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.SMART_PROXY_MX_HAPROXY))
         .setTimeout(10000)
         .build();


      JSONObject json = JSONUtils.stringToJson(CrawlerUtils.retryRequest(request, session, new FetcherDataFetcher(), false).getBody());

      String token = json.optString("access_token");
      String tokenType = json.optString("token_type");

      if (tokenType.equals("Bearer")) {
         token = tokenType + " " + token;
      }

      return token;
   }

   @Override
   protected JSONObject fetchProductApi(String productId, String token) {
      String url = "https://services." + API_BASE_URL + "/api/ms/web-proxy/dynamic-list/cpgs";

      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "application/json, text/plain, */*");
      headers.put("language", "pt");
      headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36");
      headers.put("content-type", "application/json");
      headers.put("authorization", token);

      String productFriendlyUrl = getStoreId() + "_" + productId;

      String payload = "{\"dynamic_list_request\":{\"context\":\"product_detail\",\"state\":{\"lat\":\"1\",\"lng\":\"1\"},\"limit\":100,\"offset\":0},\"dynamic_list_endpoint\":\"context/content\",\"proxy_input\":{\"product_friendly_url\":\"" + productFriendlyUrl + "\"}}";


      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .build();

      String body = this.dataFetcher.post(session, request).getBody();

      JSONObject jsonObject = CrawlerUtils.stringToJSONObject(body);

      return JSONUtils.getValueRecursive(jsonObject, "dynamic_list_response.data.components.0.resource.product", ".", JSONObject.class, new JSONObject());
   }
}
