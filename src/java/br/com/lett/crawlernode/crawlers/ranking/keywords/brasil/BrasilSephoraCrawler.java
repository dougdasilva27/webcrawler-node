package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BrasilSephoraCrawler extends CrawlerRankingKeywords {

   public BrasilSephoraCrawler(Session session) {
      super(session);
   }

   public JSONObject crawlApi() {

      String apiUrl = "https://api.linximpulse.com/engage/search/v3/search?apiKey=sephora-br&page=" + this.currentPage + "&resultsPerPage=" + productsLimit + "&terms=" + this.keywordEncoded;

      this.log("Link onde são feitos os crawlers: " + apiUrl);

      Map<String, String> headers = new HashMap<>();
      headers.put("origin", "https://www.sephora.com.br");

      Request request = Request.RequestBuilder.create()
         .setUrl(apiUrl)
         .setHeaders(headers)
         .build();
      String content = this.dataFetcher
         .get(session, request)
         .getBody();

      return CrawlerUtils.stringToJson(content);

   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);

      JSONObject json = crawlApi();
      JSONArray products = JSONUtils.getValueRecursive(json, "products", JSONArray.class);

      if (!products.isEmpty()) {

         for (Object obj : products) {
            if (obj instanceof JSONObject) {
               JSONObject product = (JSONObject) obj;

               String internalPid = product.optString("id");
               String url = product.optString("url");

               String productUrl = url != null ? "https://www.sephora.com.br" + url : null;

               saveDataProduct(null, internalPid, productUrl);

               //Didn’t put internalid because the product has variation, that is, two internalids
               this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);

            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

}
