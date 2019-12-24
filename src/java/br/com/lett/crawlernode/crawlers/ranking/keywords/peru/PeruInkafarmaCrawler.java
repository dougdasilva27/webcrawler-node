package br.com.lett.crawlernode.crawlers.ranking.keywords.peru;

import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.methods.JavanetDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.JSONUtils;

public class PeruInkafarmaCrawler extends CrawlerRankingKeywords {

   public PeruInkafarmaCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   private String accessToken;

   @Override
   protected void processBeforeFetch() {
      super.processBeforeFetch();

      Map<String, String> headersToken = new HashMap<>();
      headersToken.put(HttpHeaders.CONTENT_TYPE, "application/json");

      Request requestToken = RequestBuilder.create()
            .setUrl("https://www.googleapis.com/identitytoolkit/v3/relyingparty/signupNewUser?key="
                  + br.com.lett.crawlernode.crawlers.corecontent.peru.PeruInkafarmaCrawler.GOOGLE_KEY)
            .setPayload("{\"returnSecureToken\":true}")
            .setHeaders(headersToken)
            .build();

      Response response = this.dataFetcher.post(session, requestToken);
      JSONObject apiTokenJson = JSONUtils.stringToJson(response.getBody());
      if (apiTokenJson.has("idToken") && !apiTokenJson.isNull("idToken")) {
         this.accessToken = apiTokenJson.get("idToken").toString();
      }
   }

   @Override
   public void extractProductsFromCurrentPage() {
      this.pageSize = 40;

      this.log("Página " + this.currentPage);
      JSONObject search = crawlSearchApi();

      if (search.has("rows") && search.getJSONArray("rows").length() > 0) {
         JSONArray products = search.getJSONArray("rows");

         if (this.totalProducts == 0) {
            setTotalProducts(search);
         }

         for (Object o : products) {
            if (o instanceof JSONObject) {
               JSONObject product = JSONUtils.getJSONValue((JSONObject) o, "product");

               String internalId = product.has("id") && !product.isNull("id") ? product.get("id").toString() : null;
               String productUrl = internalId != null ? "https://inkafarma.pe/p/product-detail?sku=" + internalId : null;

               saveDataProduct(internalId, internalId, productUrl);

               this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalId + " - Url: " + productUrl);

               if (this.arrayProducts.size() == productsLimit) {
                  break;
               }
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   protected void setTotalProducts(JSONObject search) {
      this.totalProducts = JSONUtils.getIntegerValueFromJSON(search, "totalRecords", 0);
      this.log("Total da busca: " + this.totalProducts);
   }

   private JSONObject crawlSearchApi() {
      JSONObject searchApi = new JSONObject();

      if (this.accessToken != null) {
         String url = "https://qurswintke.execute-api.us-west-2.amazonaws.com/PROD/productsearch";
         this.log("Link onde são feitos os crawlers: " + url);

         JSONObject payload = new JSONObject();
         payload.put("brands", new JSONArray());
         payload.put("categories", new JSONArray());
         payload.put("order", "DESC");
         payload.put("page", this.currentPage - 1);
         payload.put("pharmaceuticalForm", new JSONArray());
         payload.put("prescriptionType", new JSONArray());
         payload.put("presentations", new JSONArray());
         payload.put("query", this.location);
         payload.put("rows", 10);
         payload.put("sort", "ranking");

         Map<String, String> headers = new HashMap<>();
         headers.put(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
         headers.put("x-access-token", this.accessToken);
         headers.put("AndroidVersion", "100000");

         Request request = RequestBuilder.create()
               .setUrl(url)
               .setHeaders(headers)
               .mustSendContentEncoding(false)
               .setPayload(payload.toString())
               .build();

         JSONObject result = JSONUtils.stringToJson(new JavanetDataFetcher().post(session, request).getBody());
         searchApi = JSONUtils.getJSONValue(result, "data");
      }

      return searchApi;
   }
}
