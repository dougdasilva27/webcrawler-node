package br.com.lett.crawlernode.crawlers.ranking.keywords.peru;

import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class PeruMifarmaCrawler extends CrawlerRankingKeywords {

   protected int totalPages;

   public PeruMifarmaCrawler(Session session) {
      super(session);
   }

   @Override
   protected JSONObject fetchJSONObject(String url){
      Map<String, String> headers = new HashMap<>();
      headers.put("content-type","application/json");

      String payload = "{\"params\":\"query=" + this.keywordEncoded + "&attributesToRetrieve=%5B%22objectID%22%2C%22name%22%2C%22uri%22%5D&hitsPerPage=10&page=" + (this.currentPage - 1) + "\"}";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .build();

      String json = new JsoupDataFetcher().post(session, request).getBody();
      return JSONUtils.stringToJson(json);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 10;
      this.log("Página " + this.currentPage);

      String url = "https://o74e6qkj1f-dsn.algolia.net/1/indexes/products/query?x-algolia-agent=Algolia%20for%20JavaScript%20(3.35.1)%3B%20Browser&x-algolia-application-id=O74E6QKJ1F&x-algolia-api-key=b65e33077a0664869c7f2544d5f1e332";
      JSONObject json = fetchJSONObject(url);
      JSONArray products = json.optJSONArray("hits");

      if (products != null && !products.isEmpty()) {
         if (totalProducts == 0) {
            this.totalPages = json.optInt("nbPages");
            setTotalProducts(json);
         }

         for (Object obj : products) {
            if (obj instanceof JSONObject) {
               JSONObject product = (JSONObject) obj;
               String internalPid = product.optString("objectID");
               String productUrl = "https://mifarma.com.pe/producto/" + product.optString("uri") + "/" + internalPid;
               saveDataProduct(null, internalPid, productUrl);

               log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
               if (arrayProducts.size() == productsLimit) {
                  break;
               }
            }
         }
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return this.currentPage < totalPages;
   }

   protected void setTotalProducts(JSONObject json) {
      this.totalProducts = json.optInt("nbHits");
      this.log("Total de produtos: " + this.totalProducts);
   }

}
