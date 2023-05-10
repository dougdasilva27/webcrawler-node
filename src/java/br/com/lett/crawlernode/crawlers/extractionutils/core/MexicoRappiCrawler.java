package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MexicoRappiCrawler extends RappiCrawler {

   private static final String HOME_PAGE = "https://www.rappi.com.mx/";
   private static final String API_BASE_URL = "mxgrability.rappi.com";
   private static final List<String> proxies = List.of(
      ProxyCollection.BUY_HAPROXY,
      ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
      ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY
      );
   private String DEVICE_ID = UUID.randomUUID().toString();

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

   @Override
   protected String fetchPassportToken() {
      String url = "https://services." + API_BASE_URL + "/api/rocket/v2/guest/passport/";

      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "application/json, text/plain, */*");
      headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36");
      headers.put("content-type", "application/json");
      headers.put("deviceid", DEVICE_ID);
      headers.put("needAppsFlyerId", "false");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(proxies)
         .build();

      JSONObject json = JSONUtils.stringToJson(CrawlerUtils.retryRequest(request, session, new ApacheDataFetcher(), true).getBody());

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
      headers.put("deviceid", DEVICE_ID);
      headers.put("needAppsFlyerId", "false");

      String payload = "{}";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .setProxyservice(proxies)
         .build();

      JSONObject json = JSONUtils.stringToJson(CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher(), false).getBody());

      String token = json.optString("access_token");
      String tokenType = json.optString("token_type");

      if (tokenType.equals("Bearer")) {
         token = tokenType + " " + token;
      }

      return token;
   }

   @Override
   protected JSONObject fetchProductApi(String productId, String token) {
      String url = "https://services." + API_BASE_URL + "/api/web-gateway/web/dynamic/context/content/";

      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "application/json");
      headers.put("language", "es");
      headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36");
      headers.put("content-type", "application/json");
      headers.put("deviceid", DEVICE_ID);
      headers.put("needAppsFlyerId", "false");
      headers.put("include_context_info", "true");
      headers.put("app-version", "web_v1.170.0");
      headers.put("authorization", token);

      String productFriendlyUrl = getStoreId() + "_" + productId;

      String payload = "{\"limit\":10,\"offset\":0,\"state\":{\"parent_store_type\":\"\",\"product_id\":\"" + productId + "\",\"lat\":\"1\",\"lng\":\"1\"},\"stores\":[" + getStoreId() + "],\"context\":\"product_detail\"}";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .setProxyservice(proxies)
         .build();

      Response r = CrawlerUtils.retryRequest(request, session, new ApacheDataFetcher(), false);
      String body = r.getBody();

      JSONObject jsonObject = CrawlerUtils.stringToJSONObject(body);

      return JSONUtils.getValueRecursive(jsonObject, "data.components.0.resource.product", ".", JSONObject.class, new JSONObject());
   }

}
