package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.*;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PedidosyaCrawler extends CrawlerRankingKeywords {

   public PedidosyaCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   private final List<String> proxies = Arrays.asList(
      ProxyCollection.LUMINATI_SERVER_BR,
      ProxyCollection.BUY,
      ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
      ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
      ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
      ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY);

   String storeId = session.getOptions().optString("store_id");
   String type = session.getOptions().optString("type");
   String city = session.getOptions().optString("city");
   String market = session.getOptions().optString("market");
   String catalogues = session.getOptions().optString("catalogues");



   private Response retryRequest(Request request, List<DataFetcher> dataFetcherList) {
      Response response = dataFetcherList.get(0).get(session, request);

      if (!response.isSuccess()) {
         int tries = 0;
         while (!response.isSuccess() && tries < 3) {
            tries++;
            if (tries % 2 == 0) {
               response = dataFetcherList.get(1).get(session, request);
            } else {
               response = dataFetcherList.get(2).get(session, request);
            }
         }
      }

      return response;
   }

   @Override
   protected void processBeforeFetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
      headers.put("authority", "www.pedidosya.com.ar");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
      String url = "https://www.pedidosya.com.ar";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setProxyservice(proxies)
         .setHeaders(headers)
         .setFetcheroptions(FetcherOptions.FetcherOptionsBuilder.create().setForbiddenCssSelector("#px-captcha").build())
         .setSendUserAgent(false)
         .build();
      Response response = new ApacheDataFetcher().get(session, request);

      if (!response.isSuccess()) {
         response = retryRequest(request, List.of(this.dataFetcher, new JavanetDataFetcher(), new JsoupDataFetcher()));
      }
      this.cookies = response.getCookies();
   }

   private JSONObject getInfoFromAPI() {
      String url = "https://www.pedidosya.com.ar/mobile/v3/catalogues/" + catalogues + "/search?max=50&offset=0&partnerId=" + storeId + "&query=" + this.keywordEncoded + "&sort=default";
      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "application/json, text/plain, */*");
      headers.put("referer", "https://www.pedidosya.com.ar/restaurantes/buenos-aires/pedidosya-market-belgrano-menu/buscar");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
      headers.put("authority", "www.pedidosya.com.ar");
      headers.put("cookie", CommonMethods.cookiesToString(this.cookies));

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(proxies)
         .setSendUserAgent(true)
         .setFetcheroptions(FetcherOptions.FetcherOptionsBuilder.create().setForbiddenCssSelector("#px-captcha").build())
         .build();

      Response resp = new ApacheDataFetcher().get(session, request);

      if (!resp.isSuccess()) {
         resp = retryRequest(request, List.of(this.dataFetcher, new JavanetDataFetcher(), new JsoupDataFetcher()));
      }
      return CrawlerUtils.stringToJson(resp.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 30;
      this.log("Página " + this.currentPage);

      JSONObject json = getInfoFromAPI();
      JSONArray data = json.optJSONArray("data");

      if (data != null && !data.isEmpty()) {

         for (Object o : data) {
            JSONObject productInfo = (JSONObject) o;

            String internalId = productInfo.optString("id");
            String internalPid = internalId;
            String productUrl = " https://www.pedidosya.com.ar/" + type + "/" + city + "/" + market + "-menu?p=" + internalId;
            String name = productInfo.optString("name");
            int price = productInfo.optInt("price");
            boolean isAvailable = price != 0;

            //New way to send products to save data product
            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);
            if (arrayProducts.size() == productsLimit) {
               break;
            }

         }
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }
}
