package br.com.lett.crawlernode.crawlers.ranking.keywords.romania;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RomaniaGlovoCrawler extends CrawlerRankingKeywords {

   private static final String BASE_URL = "https://glovoapp.com/en/buc/store/kaufland-buc/product_id=";

   public RomaniaGlovoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {

      JSONObject json = fetchJSON();

      JSONArray results = json.optJSONArray("results");

      if (results != null && !results.isEmpty()) {
         this.totalProducts = json.optInt("totalProducts", 0);

         for (Object e : results) {
            if (e instanceof JSONObject) {
               JSONObject result = (JSONObject) e;

               JSONArray products = result.optJSONArray("products");

               for (Object prod : products) {
                  if (prod instanceof JSONObject) {
                     JSONObject product = (JSONObject) prod;

                     String internalId = product.optString("id");
                     String internalPid = product.optString("externalId");
                     //https://glovoapp.com/en/buc/store/kaufland-buc/productid=2135325
                     String productUrl = BASE_URL + internalId;

                     saveDataProduct(internalId, internalPid, productUrl);
                     this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
                     if (this.arrayProducts.size() == productsLimit)
                        break;
                  }
               }
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private JSONObject fetchJSON() {
      String url = "https://api.glovoapp.com/v3/stores/52287/addresses/152639/search?query=" + this.keywordEncoded;
      this.log("Link onde são feitos os crawlers: " + url);

      Map<String, String> headers = new HashMap<>();
      headers.put("glovo-location-city-code", "BUC");

      Request request = Request.RequestBuilder.create()
         .setCookies(cookies)
         .setHeaders(headers)
         .setUrl(url)
         .build();
      Response response = dataFetcher.get(session, request);

      return CrawlerUtils.stringToJson(response.getBody());
   }

}
