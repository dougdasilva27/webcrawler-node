package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BrasilHavanCrawler extends CrawlerRankingKeywords {

   public BrasilHavanCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {

      this.pageSize = 24;

      this.log("Página " + this.currentPage);

      JSONObject jsonObject = fetchApi();

      JSONArray productsArr = jsonObject != null ? jsonObject.optJSONArray("products") : null;


      if (productsArr != null && !productsArr.isEmpty()) {

         if (this.totalProducts == 0) {
            setTotalProducts(jsonObject);
         }

         for (Object o : productsArr) {

            if (o instanceof JSONObject) {
               JSONObject product = (JSONObject) o;

               String internalPid = product.optString("iId");
               String internalId = product.optString("id");
               String partUrl = product.optString("url");
               String productUrl = partUrl != null ? "https:" + partUrl : null;

               saveDataProduct(internalId, internalPid, productUrl);

               this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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


   private JSONObject fetchApi() {
      String url = "https://api.linximpulse.com/engage/search/v3/search?apiKey=havan&page=" + this.currentPage + "&resultsPerPage=36&terms=" + this.keywordEncoded + "&sortBy=relevance";

      Map<String, String> headers = new HashMap<>();
      headers.put("Origin", "https://www.havan.com.br/");

      Request request = Request.RequestBuilder.create()
         .setHeaders(headers)
         .setUrl(url)
         .build();

      String response = dataFetcher.get(session, request).getBody();
      return CrawlerUtils.stringToJson(response);
   }

   protected void setTotalProducts(JSONObject jsonObject) {

      this.totalProducts = jsonObject.optInt("pagination");


      this.log("Total da busca: " + this.totalProducts);
   }

}
