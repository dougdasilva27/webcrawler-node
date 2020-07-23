package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BrasilIkesakiCrawler extends CrawlerRankingKeywords {

   public BrasilIkesakiCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 48;
      this.log("Página " + this.currentPage);

      JSONObject json = fetchApi();
      JSONArray products = json.optJSONArray("products");

      if (products != null && !products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts(json);
         }

         for (Object element : products) {
            JSONObject product = (JSONObject) element;

            String internalPid = product.optString("id");
            String productUrl = "https:" + product.optString("url");

            saveDataProduct(null, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }

         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private void setTotalProducts(JSONObject json) {
      this.totalProducts = json.optInt("size", 0);
      this.log("Total da busca: " + this.totalProducts);
   }

   private JSONObject fetchApi() {
      String url = "https://api.linximpulse.com/engage/search/v3/search?apiKey=ikesaki&page=" + this.currentPage + "&resultsPerPage=48&terms=" + this.keywordEncoded;
      this.log("Link onde são feitos os crawlers: " + url);

      Map<String, String> headers = new HashMap<>();
      headers.put("Origin", "https://www.ikesaki.com.br");

      Request request = Request.RequestBuilder.create()
         .setHeaders(headers)
         .setUrl(url)
         .build();

      String response = dataFetcher.get(session, request).getBody();
      return CrawlerUtils.stringToJson(response);
   }
}
