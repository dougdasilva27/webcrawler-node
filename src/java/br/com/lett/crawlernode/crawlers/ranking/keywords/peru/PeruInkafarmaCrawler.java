package br.com.lett.crawlernode.crawlers.ranking.keywords.peru;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.HttpClientFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PeruInkafarmaCrawler extends CrawlerRankingKeywords {

   public PeruInkafarmaCrawler(Session session) {
      super(session);
   }

   public static final String GOOGLE_KEY = "AIzaSyC2fWm7Vfph5CCXorWQnFqepO8emsycHPc";
   private final String storeID = getStoreId();

   protected String getStoreId() {
      return session.getOptions().optString("store_id");
   }

   private String accessToken;

   @Override
   protected void processBeforeFetch() {
      super.processBeforeFetch();

      Map<String, String> headersToken = new HashMap<>();
      headersToken.put(HttpHeaders.CONTENT_TYPE, "application/json");

      Request requestToken = RequestBuilder.create()
         .setUrl("https://www.googleapis.com/identitytoolkit/v3/relyingparty/signupNewUser?key=" + GOOGLE_KEY)
         .setPayload("{\"returnSecureToken\":true}")
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY
         ))
         .setHeaders(headersToken)
         .build();

      Response responseToken = CrawlerUtils.retryRequestWithListDataFetcher(requestToken, List.of(new HttpClientFetcher(), new ApacheDataFetcher()), session, "post");
      JSONObject apiTokenJson = JSONUtils.stringToJson(responseToken.getBody());


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
               String image = JSONUtils.getValueRecursive(product, "imageList.0.url", String.class);
               boolean isAvailable = product.optString("productStatus").equalsIgnoreCase("AVAILABLE");
               Integer price = isAvailable ? setPrice(product) : null;

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setName(name)
                  .setPriceInCents(price)
                  .setImageUrl(image)
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
         headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
         headers.put("x-access-token", this.accessToken);
         headers.put("AndroidVersion", "100000");
         if (storeID != null) {
            headers.put("drugstore-stock", storeID);
         }

         Request request = RequestBuilder.create()
            .setUrl(url)
            .setHeaders(headers)
            .mustSendContentEncoding(false)
            .setProxyservice(Arrays.asList(
               ProxyCollection.BUY_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY
            ))
            .setPayload(payload.toString())
            .build();

         Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new HttpClientFetcher(), new ApacheDataFetcher()), session, "post");
         searchApi = JSONUtils.stringToJson(response.getBody());

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
      if (storeID != null) {
         headers.put("drugstore-stock", storeID);
      }

      Request request = RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY
         ))
         .setPayload(payload.toString())
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new HttpClientFetcher(), new ApacheDataFetcher()), session, "post");
      JSONObject result = JSONUtils.stringToJson(response.getBody());

      return result.optJSONArray("productsId");
   }

   private Integer setPrice(JSONObject product) {
      Integer spotlightPrice = JSONUtils.getPriceInCents(product, "priceAllPaymentMethod");

      if (spotlightPrice == 0) {
         spotlightPrice = JSONUtils.getPriceInCents(product, "price");
      }

      return spotlightPrice;
   }
}
