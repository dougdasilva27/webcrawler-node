package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class SaopauloAmericanasCrawler extends CrawlerRankingKeywords {

   public SaopauloAmericanasCrawler(Session session) {
      super(session);
   }

   private JSONObject jsonObject() throws UnsupportedEncodingException {

      String url = getUrl();

      this.log("Link onde são feitos os crawlers: " + url);

      Request request = Request.RequestBuilder.create().setUrl(url)
         .build();
      String response = this.dataFetcher.get(session, request).getBody();

      return JSONUtils.stringToJson(response);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException {
      this.pageSize = 24;

      this.log("Página " + this.currentPage);

      JSONObject json = jsonObject();

      JSONArray products = JSONUtils.getValueRecursive(json, "data.search.products", JSONArray.class);

      if (products != null && !products.isEmpty()) {
         if (totalProducts == 0)
            setTotalProducts(json);

         for (Object e : products) {
            if (e instanceof JSONObject) {
               JSONObject product = (JSONObject) e;

               JSONObject data = product.optJSONObject("product");

               String internalPid = JSONUtils.getStringValue(data, "id");

               String productUrl = "https://www.americanas.com.br/produto/" + internalPid;

               saveDataProduct(null, internalPid, productUrl);

               this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
               if (this.arrayProducts.size() == productsLimit)
                  break;
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
   }

   private String getUrl() throws UnsupportedEncodingException {
      String variables = "{\"path\":\"/busca/desifetante?limit=24&offset=," + (this.currentPage - 1) * pageSize + "\",\"content\":\"desifetante\",\"offset\":0,\"limit\":24,\"segmented\":false,\"filters\":[],\"oneDayDelivery\":true}\"";
      String extensions = "{\"persistedQuery\":{\"version\":1,\"sha256Hash\":\"dc1d06c9124fb3b8d1332cfae79f587926aef50f9322e50f0136780b2b94ed5a\"}}";

      return "https://catalogo-bff-v2-americanas.b2w.io/graphql?operationName=pageSearch&variables=" + URLEncoder.encode(variables, "UTF-8") + "&extensions=" + URLEncoder.encode(extensions, "UTF-8");

   }

   protected void setTotalProducts(JSONObject json) {
      JSONObject data = JSONUtils.getValueRecursive(json, "data.search", JSONObject.class);
      Integer total = data != null && !data.isEmpty() ? JSONUtils.getIntegerValueFromJSON(data, "total", 0) : null;
      if (total != null) {
         this.totalProducts = total;
         this.log("Total da busca: " + this.totalProducts);
      }
   }

}
