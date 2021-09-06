package br.com.lett.crawlernode.crawlers.ranking.keywords.panama;

import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class PanamaSuperxtraCrawler extends CrawlerRankingKeywords {

   protected int totalPages;

   public PanamaSuperxtraCrawler(Session session) {
      super(session);
      this.dataFetcher = new JsoupDataFetcher();
   }

   protected JSONObject fetchAPI() {
      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");
      headers.put("authority", "deadpool.instaleap.io");
      headers.put("accept", "*/*");
      headers.put("origin", "https://domicilio.superxtra.com");
      headers.put("accept-encoding", "gzip, deflate, br");
      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36");

      String payload = "{\"variables\":{\"pagination\":{\"pageSize\":100,\"currentPage\":" + this.currentPage + "},\"search\":{\"text\":\"" + this.keywordEncoded + "\",\"language\":\"ES\"},\"storeId\":\"22\"},\"query\":\"query ($pagination: paginationInput, $search: SearchInput, $storeId: ID!, $categoryId: ID, $onlyThisCategory: Boolean, $filter: ProductsFilterInput, $orderBy: productsSortInput) {  getProducts(pagination: $pagination, search: $search, storeId: $storeId, categoryId: $categoryId, onlyThisCategory: $onlyThisCategory, filter: $filter, orderBy: $orderBy) {    redirectTo    products {      id      name      photosUrls      sku      unit      price      specialPrice      promotion {        description        type        isActive        conditions        __typename      }      stock      nutritionalDetails      clickMultiplier      subQty      subUnit      maxQty      minQty      specialMaxQty      ean      boost      showSubUnit      isActive      slug      categories {        id        name        __typename      }      __typename    }    paginator {      pages      page      __typename    }    __typename  }}\"}";

      String url = "https://deadpool.instaleap.io/api/v2";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setPayload(payload)
         .setHeaders(headers)
         .setCookies(cookies)
         .build();

      Response response = new JsoupDataFetcher().post(session, request);
      return JSONUtils.stringToJson(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      pageSize = 100;
      JSONObject json = fetchAPI();

      JSONArray results = JSONUtils.getValueRecursive(json, "data.getProducts.products", JSONArray.class);

      if (results != null && !results.isEmpty()) {
         if (currentPage == 1) {
            totalPages = JSONUtils.getValueRecursive(json, "data.getProducts.paginator.pages", Integer.class);
         }

         for (Object prod : results) {
            JSONObject product = (JSONObject) prod;

            String internalPid = product.optString("sku");
            String internalId = product.optString("id");
            String productUrl = "https://domicilio.superxtra.com/p/" + product.optString("slug");

            saveDataProduct(internalId, internalPid, productUrl);
            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return  currentPage <= totalPages;
   }
}
