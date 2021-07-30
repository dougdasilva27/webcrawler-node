package br.com.lett.crawlernode.crawlers.ranking.keywords.curitiba;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class CuritibaSchummancuritibaCrawler extends CrawlerRankingKeywords {

   public CuritibaSchummancuritibaCrawler(Session session) {
      super(session);
   }

   protected JSONObject fetch(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("Authorization", "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6bnVsbCwiYWRtaW4iOnRydWUsImp0aSI6IjU4N2NjMjJjNGYyNjhlMWU5Yjc0YmZlY2NmYTBjN2IyNGZjYTg5MzA3ZDQzYzdjZmVkYTQwYTU4NTZkMWZlNTgiLCJpYXQiOjE2MDE1ODE0NjIsImV4cCI6MTY1MTE3NTA2MiwiZW1haWwiOiJldmVydG9uQHByZWNvZGUuY29tLmJyIiwiZW1wcmVzYSI6bnVsbCwic2NoZW1hIjoiRXN0dWRpb25ldHNob3AiLCJpZFNjaGVtYSI6NTQsImlkU2VsbGVyIjoiNjIiLCJpZFVzZXIiOjF9.v+7JeiaPQVabn+/CeE0RUGQCYaAtCNqo+omeem8Vo2s=");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();
      String content = this.dataFetcher
         .get(session, request)
         .getBody();

      return CrawlerUtils.stringToJson(content);

   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 15;
      this.log("Página " + this.currentPage);

      String url = "https://www.allfront.com.br/api/busca/" + this.keywordEncoded + "?offset=" + (this.currentPage - 1) * 15;

      this.log("Link onde são feitos os crawlers: " + url);
      JSONObject searchedJson = fetch(url);
      JSONArray productsArray = searchedJson.optJSONArray("products");
      if (productsArray != null && !productsArray.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts(searchedJson);
         }
         for (Object obj : productsArray) {
            if (obj instanceof JSONObject) {

               JSONObject product = (JSONObject) obj;

               String productUrl = getUrl(product);
               String productId = product.optString("id");

               saveDataProduct(productId, null, productUrl);

               this.log("Position: " + this.position + " - InternalId: " + productId + " - InternalPid: " + null + " - Url: " + productUrl);
               if (this.arrayProducts.size() == productsLimit)
                  break;

            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private String getUrl(JSONObject product) {
      String url = null;
      String link = product.optString("link");
      if (link != null && !link.isEmpty()) {
         url = "https://www.estudionetshop.com.br/" + link;
      }

      return url;
   }

   protected void setTotalProducts(JSONObject searchedJson) {
      this.totalProducts = searchedJson.optInt("quantidade");
      this.log("Total da busca: " + this.totalProducts);

   }

}
