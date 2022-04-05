package br.com.lett.crawlernode.crawlers.ranking.keywords.peru;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.JSONUtils;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
   public void extractProductsFromCurrentPage() throws MalformedProductException {
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
               JSONObject product = (JSONObject) o;

               String internalId = product.has("id") && !product.isNull("id") ? product.get("id").toString() : null;
               String productUrl = internalId != null ? "https://inkafarma.pe/producto/" + product.optString("slug", "") + "/" + internalId : null;

               String name = product.optString("name");
               int price = setPrice(product);
               String img = setImage(product);
               boolean isAvailable = setAvailability(product);

               //New way to send products to save data product
               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setName(name)
                  .setPriceInCents(price)
                  .setImageUrl(img)
                  .setAvailability(isAvailable)
                  .build();

               saveDataProduct(productRanking);


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
      JSONArray searchFilters = getProductsFilter();

      if (this.accessToken != null && searchFilters != null && !searchFilters.isEmpty()) {
         String url = "https://5doa19p9r7.execute-api.us-east-1.amazonaws.com/PROD/filtered-products";
         this.log("Link onde são feitos os crawlers: " + url);

         JSONObject payload = new JSONObject();
         payload.put("brandsFilter", new JSONArray());
         payload.put("categoriesFilter", new JSONArray());
         payload.put("departmentsFilter", new JSONArray());
         payload.put("order", "ASC");
         payload.put("page", this.currentPage - 1);
         payload.put("productsFilter", searchFilters);
         payload.put("rows", 24);
         payload.put("sort", "ranking");
         payload.put("subcategoriesFilter", new JSONArray());


         Map<String, String> headers = new HashMap<>();
         headers.put(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
         headers.put("x-access-token", this.accessToken);
         headers.put("AndroidVersion", "100000");
         headers.put("referer", "https://inkafarma.pe/");
         headers.put("origin", "https://inkafarma.pe");
         headers.put("Accept","application/json");

         Request request = RequestBuilder.create()
            .setUrl(url)
            .setHeaders(headers)
            .mustSendContentEncoding(false)
            .setProxyservice(Arrays.asList(
               ProxyCollection.BUY_HAPROXY,
               ProxyCollection.LUMINATI_SERVER_BR,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_ANY_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_MX,
               ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY
            ))
            .setPayload(payload.toString())
            .build();

         searchApi = JSONUtils.stringToJson(new JsoupDataFetcher().post(session, request).getBody());

      }

      return searchApi;
   }

   private JSONArray getProductsFilter() {

      String url = "https://5doa19p9r7.execute-api.us-east-1.amazonaws.com/PROD/search-filters";

      JSONObject payload = new JSONObject();
      payload.put("query", this.keywordWithoutAccents);
      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");
      headers.put("x-access-token", this.accessToken);
      headers.put("androidversion", "100000");
      headers.put("referer", "https://inkafarma.pe/");
      headers.put("origin", "https://inkafarma.pe");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
      headers.put("authority", "5doa19p9r7.execute-api.us-east-1.amazonaws.com");
      headers.put("Accept","application/json");


      Request request = RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.LUMINATI_SERVER_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_ANY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_MX,
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY
         ))
         .setPayload(payload.toString())
         .build();

      JSONObject result = JSONUtils.stringToJson(new JsoupDataFetcher().post(session, request).getBody());

      return result.optJSONArray("productsId");
   }

   private boolean setAvailability(JSONObject product) {
      String holdAvaliability = product.optString("productStatus");
      return !holdAvaliability.contains("SOLD_OUT");
   }

   private String setImage(JSONObject product) {
      Object holdImg = product.optQuery("/imageList/0/url");
      if (holdImg instanceof String && !holdImg.toString().isEmpty()) {
         return holdImg.toString();
      }
      return null;
   }

   private int setPrice(JSONObject product) {
      int holdPrioce = JSONUtils.getPriceInCents(product, "priceAllPaymentMethod");

      if (holdPrioce == 0) {
         holdPrioce = JSONUtils.getPriceInCents(product, "pricePack");
      }

      return holdPrioce;
   }
}
