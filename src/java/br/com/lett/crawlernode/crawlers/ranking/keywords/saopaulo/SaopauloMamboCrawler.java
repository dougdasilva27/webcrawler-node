package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;

import java.util.HashMap;
import java.util.Map;

public class SaopauloMamboCrawler extends CrawlerRankingKeywords {

   public SaopauloMamboCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 12;
      this.log("Página " + this.currentPage);

      JSONObject json = fetchJsonApi(this.currentPage);

      JSONArray products = json.optJSONArray("products");

      if (products != null && products.length() > 0) {
         if (this.totalProducts == 0) {
            setTotalBusca(json);
         }

         for (Object o : products) {
            JSONObject jsonProduct = (JSONObject) o;

            this.position++;

            String internalId = jsonProduct.optString("id");
            String productUrl = jsonProduct.optString("url");

            saveDataProduct(internalId, null, productUrl, this.position);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }

      }else{
         this.result = false;
         this.log("Keyword sem resultados!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   protected void setTotalBusca(JSONObject apiSearch) {
      this.totalProducts = JSONUtils.getIntegerValueFromJSON(apiSearch, "size", 0);
      this.log("Total da busca: " + this.totalProducts);
   }

   private JSONObject fetchJsonApi(int page) {
      String url = "https://api.linximpulse.com/engage/search/v3/search?apiKey=mambo-v7&showOnlyAvailable=false&resultsPerPage=12"
         + "&page=" + page + "&productFormat=complete&terms="
         + this.keywordWithoutAccents;

      Map<String, String> headers = new HashMap<>();
      headers.put("authority", "api.linximpulse.com");
      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.182 Safari/537.36");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
      headers.put("origin", "https://www.mambo.com.br");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();
      Response response = dataFetcher.get(session, request);

      return CrawlerUtils.stringToJson(response.getBody());
   }
}
