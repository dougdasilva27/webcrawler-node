package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class ArgentinaComodincoronelariasCrawler extends CrawlerRankingKeywords {

   public ArgentinaComodincoronelariasCrawler(Session session) {
      super(session);
      this.pageSize = 28;
   }

   private Response fetch(String url) {

      Map<String, String> headers = new HashMap<>();

      headers.put("authority", "*/*");
      headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_1_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.96 Safari/537.36");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();
      return dataFetcher.get(session, request);
   }

   private int getTotalProducts() {
      String url = "https://tienda.comodinencasa.com.ar/api/catalog_system/pub/facets/search/" +
         getKeywordEncoded() +
         "?map=ft&sc=1";

      Response response = fetch(url);

      JSONObject body = JSONUtils.stringToJson(response.getBody());

      int totalProducts = 0;

      JSONArray departments = JSONUtils.getJSONArrayValue(body, "Departments");

      for (Object d : departments) {
         if (d instanceof JSONObject) {
            JSONObject department = (JSONObject) d;
            totalProducts += department.optInt("Quantity", 0);
         }
      }

      return totalProducts;
   }

   String getKeywordEncoded(){
      return keywordWithoutAccents.replace(" ", "%20");
   }

   private JSONArray getProducts() {
      int start = (currentPage -1)*pageSize;

      int end = currentPage*pageSize -1;

      if (end > this.totalProducts) {
         end = totalProducts;
      }

      String url = "https://tienda.comodinencasa.com.ar/api/catalog_system/pub/products/search/busca?O=OrderByTopSaleDESC&sc=1" +
         "&_from=" + start +
         "&_to=" + end +
         "&ft=" + getKeywordEncoded();

      Response response = fetch(url);

      return JSONUtils.stringToJsonArray(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException {
      if (currentPage == 1) {
         this.totalProducts = getTotalProducts();
      }

      JSONArray products = getProducts();

      for (Object p: products) {
         if (p instanceof JSONObject) {
            JSONObject product = (JSONObject) p;
            String internalPid = product.optString("productId");
            String productUrlText = product.optString("linkText");
            String productUrl = "https://tienda.comodinencasa.com.ar/"+productUrlText+"/p";

            saveDataProduct(null, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      }
   }
}
