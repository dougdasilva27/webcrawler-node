package br.com.lett.crawlernode.crawlers.extractionutils.core;

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

public class ChileRappiCrawler extends RappiCrawler {

   public ChileRappiCrawler(Session session) {
      super(session);
   }


   protected String getHomeDomain() {
      return "rappi.cl";
   }

   protected String getImagePrefix() {
      return "images.rappi.cl/products";
   }

   @Override
   protected String getUrlPrefix() {
      return "producto/";
   }

   @Override
   protected String getHomeCountry() {
      return "https://www.rappi.cl/";
   }

   @Override
   protected String getMarketBaseUrl() {
      return "https://www.rappi.cl/tiendas/";
   }

   @Override
   protected String fetchPassportToken() {
      String url = "https://services." + getHomeDomain() + "/api/rocket/v2/guest/passport/";

      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "application/json, text/plain, */*");
      headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36");
      headers.put("content-type", "application/json");
      headers.put("deviceid", RANDOM_DEVICE_ID);
      headers.put("needAppsFlyerId", "false");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();

      JSONObject json = JSONUtils.stringToJson(CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new FetcherDataFetcher(), new JsoupDataFetcher()), session, "get").getBody());

      return json.optString("token");
   }

   @Override
   protected String fetchToken() {
      String url = "https://services." + getHomeDomain() + "/api/rocket/v2/guest";

      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "application/json, text/plain, */*");
      headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36");
      headers.put("content-type", "application/json");
      headers.put("deviceId", RANDOM_DEVICE_ID);
      headers.put("x-guest-api-key", fetchPassportToken());

      String payload = "";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .mustSendContentEncoding(false)
         .build();

      JSONObject json = JSONUtils.stringToJson(CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new FetcherDataFetcher(), new ApacheDataFetcher(), new JsoupDataFetcher()), session, "post").getBody());

      String token = json.optString("access_token");
      String tokenType = json.optString("token_type");

      if (tokenType.equals("Bearer")) {
         token = tokenType + " " + token;
      }

      return token;
   }
}
