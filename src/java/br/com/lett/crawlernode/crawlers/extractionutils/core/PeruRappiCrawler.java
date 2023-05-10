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
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;

public class PeruRappiCrawler extends RappiCrawler {
   private static final String HOME_PAGE = "https://www.rappi.com";

   public PeruRappiCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.JSOUP);
   }

   protected String DEVICE_ID = UUID.randomUUID().toString();

   @Override
   protected String getHomeDomain() {
      return "rappi.com.pe";
   }

   @Override
   protected String getImagePrefix() {
      return "images.rappi.pe/products";
   }

   @Override
   protected String getUrlPrefix() {
      return "producto/";
   }

   @Override
   protected String getHomeCountry() {
      return "https://www.rappi.com.pe/";
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
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY))
         .setTimeout(10000)
         .setUrl(session.getOriginalURL())
         .build();

      Response response = CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher(), true);

      return Jsoup.parse(response.getBody());
   }

   @Override
   protected String getMarketBaseUrl() {
      return "https://www.rappi.com.pe/tiendas/";
   }

   @Override
   protected String fetchPassportToken() {
      String url = "https://services.rappi.pe/api/rocket/v2/guest/passport/";

      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "application/json, text/plain, */*");
      headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36");
      headers.put("content-type", "application/json");
      headers.put("deviceid", DEVICE_ID);
      headers.put("needAppsFlyerId", "false");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(List.of(
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY))
         .setTimeout(10000)
         .build();

      JSONObject json = JSONUtils.stringToJson(CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher(), true).getBody());


      return json.optString("token");

   }

   @Override
   protected String fetchToken() {
      String url = "https://services.rappi.pe/api/rocket/v2/guest";

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
         .setProxyservice(List.of(
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY))
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
      String url = "https://services.rappi.pe/api/web-gateway/web/dynamic/context/content/";

      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "application/json, text/plain, */*");
      headers.put("language", "es");
      headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36");
      headers.put("content-type", "application/json");
      headers.put("deviceid", DEVICE_ID);
      headers.put("needAppsFlyerId", "false");
      headers.put("include_context_info", "true");
      headers.put("app-version", "web_v1.170.0");

      headers.put("authorization", token);

      String productFriendlyUrl = getStoreId() + "_" + productId;

      String payload = "{\"limit\":10,\"offset\":0,\"state\":{\"product_id\":\"" + productId + "\",\"lat\":\"1\",\"lng\":\"1\"},\"stores\":[" + getStoreId() + "],\"context\":\"product_detail\"}";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .build();

      String body = CrawlerUtils.retryRequest(request, session, new ApacheDataFetcher(), false).getBody();

      JSONObject jsonObject = CrawlerUtils.stringToJSONObject(body);

      return JSONUtils.getValueRecursive(jsonObject, "data.components.0.resource.product", ".", JSONObject.class, new JSONObject());
   }

   @Override
   protected String getProductIdFromRanking(JSONObject productJson) {
      String productName = productJson.optString("name");
      String productImage = productJson.optString("image");
      String productNameEncoded = productName != null ? StringUtils.stripAccents(productName).replace(" ", "%20") : null;
      String url = getMarketBaseUrl() + getStoreId() + "/s?term=" + productNameEncoded;
      Request request = Request.RequestBuilder.create()
         .setCookies(this.cookies)
         .setUrl(url)
         .setProxyservice(List.of(
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY))
         .setFollowRedirects(true)
         .build();

      Response response = CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher(), true);

      Document docRanking = Jsoup.parse(response.getBody());
      JSONObject rankingPageJson = CrawlerUtils.selectJsonFromHtml(docRanking, "#__NEXT_DATA__", null, null, false, false);
      JSONArray searchProducts = JSONUtils.getValueRecursive(rankingPageJson, "props.pageProps.products", JSONArray.class, new JSONArray());

      if (searchProducts.isEmpty()) {
         JSONObject fallback = JSONUtils.getValueRecursive(rankingPageJson, "props.pageProps.fallback", JSONObject.class, new JSONObject());
         if (!fallback.isEmpty()) {
            Iterator<String> keys = fallback.keys();
            while(keys.hasNext()) {
               String key = keys.next();
               if (fallback.get(key) instanceof JSONObject) {
                  searchProducts = JSONUtils.getValueRecursive(fallback.get(key), "products", JSONArray.class, new JSONArray());
               }
            }
         }
      }

      String productFoundInternalId = null;
      if (searchProducts.length() > 0) {
         for (int i = 0; i < searchProducts.length(); i++) {
            JSONObject searchProduct = searchProducts.getJSONObject(i);
            String imageProductSearch = searchProduct.optString("image");
            if (imageProductSearch.equals(productImage)) {
               productFoundInternalId = searchProduct.optString("product_id");
               break;
            }
         }
      }

      return productFoundInternalId;
   }
}
