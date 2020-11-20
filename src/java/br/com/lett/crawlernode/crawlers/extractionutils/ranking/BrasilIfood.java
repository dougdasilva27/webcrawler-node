package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import com.google.common.net.HttpHeaders;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.JSONUtils;


public abstract class BrasilIfood extends CrawlerRankingKeywords {

   public BrasilIfood(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   protected String region = getRegion();
   protected String storeName = getStoreName();
   protected String storeId = getStoreId();

   protected abstract String getRegion();

   protected abstract String getStoreName();

   protected abstract String getStoreId();

   @Override
   protected void extractProductsFromCurrentPage() {

      String url = "https://www.ifood.com.br/delivery/" + region + "/" + storeName + "/" + storeId;
      JSONObject apiJson = fetch();
      JSONObject data = JSONUtils.getJSONValue(apiJson, "data");
      JSONArray menu = JSONUtils.getJSONArrayValue(data, "menu");

      if (menu != null && !menu.isEmpty()) {

         for (Object menuArr : menu) {
            JSONObject menuObject = (JSONObject) menuArr;
            JSONArray itens = JSONUtils.getJSONArrayValue(menuObject, "itens");

            for (Object itensArr : itens) {

               JSONObject itensObject = (JSONObject) itensArr;

               if (!itensObject.isEmpty()) {

                  String internalId = itensObject.optString("code");
                  String internalPid = internalId;
                  String productUrl = url + "?prato=" + internalId;

                  saveDataProduct(internalId, internalPid, productUrl);

                  this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
                  if (this.arrayProducts.size() == productsLimit)
                     break;

               }
            }

            this.log("Finalizando categoria " + menuObject.optString("name") + " do ifood, ate agora " + this.arrayProducts.size() + " produtos crawleados");
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

   }

   protected JSONObject fetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.ACCEPT, "application/json, text/plain, */*");
      headers.put("platform", "Desktop");
      headers.put("app_version", "8.31.0");
      headers.put("access_key", "69f181d5-0046-4221-b7b2-deef62bd60d5");
      headers.put("secret_key", "9ef4fb4f-7a1d-4e0d-a9b1-9b82873297d8");
      headers.put(HttpHeaders.USER_AGENT, "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116 Safari/537.36");

      Request request = RequestBuilder.create()
            .setUrl("https://wsloja.ifood.com.br/ifood-ws-v3/restaurants/" + storeId + "/menu")
            .setCookies(this.cookies)
            .setHeaders(headers)
            .mustSendContentEncoding(false)
            .setFetcheroptions(
                  FetcherOptionsBuilder.create()
                        .mustUseMovingAverage(false)
                        .mustRetrieveStatistics(true)
                        .setForbiddenCssSelector("#px-captcha")
                        .build()
            ).setProxyservice(
                  Arrays.asList(
                        ProxyCollection.INFATICA_RESIDENTIAL_BR,
                        ProxyCollection.BUY,
                        ProxyCollection.NETNUT_RESIDENTIAL_BR
                  )
            ).build();

      String content = this.dataFetcher.get(session, request).getBody();

      if (content == null || content.isEmpty()) {
         content = new ApacheDataFetcher().get(session, request).getBody();
      }

      return JSONUtils.stringToJson(content);
   }

}
