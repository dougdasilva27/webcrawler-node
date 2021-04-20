package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

import java.util.HashMap;
import java.util.Map;

public class SaopauloDrogariasaopauloCrawler extends CrawlerRankingKeywords {

   public SaopauloDrogariasaopauloCrawler(Session session) {
      super(session);
   }

   private JSONArray productsJSONArray;

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 48;
      this.log("Página " + this.currentPage);

      this.productsJSONArray = new JSONArray();
      String urlApi = "https://api.linximpulse.com/engage/search/v3/search?apiKey=drogariasaopaulo&productFormat=complete&resultsPerPage=48&page="+this.currentPage+"&terms="+keywordEncoded;

      Map<String,String> headers = new HashMap<>();
      headers.put("origin","https://www.drogariasaopaulo.com.br");

      Request request = Request.RequestBuilder.create()
         .setUrl(urlApi)
         .setHeaders(headers)
         .build();

      String response = dataFetcher.get(session,request).getBody() ;

      JSONObject jsononj = CrawlerUtils.stringToJson(response);


      productsJSONArray = JSONUtils.getJSONArrayValue(jsononj,"products");



      if (productsJSONArray.length() > 0) {
         for (Object o : productsJSONArray) {
            JSONObject item = (JSONObject) o;
            String internalPid = item.optString("id") ;
            String url = JSONUtils.getValueRecursive(item,"skus.0.properties.url",String.class);

            String productUrl = CrawlerUtils.completeUrl(url,"https","www.drogariasaopaulo.com.br");


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

   @Override
   protected boolean hasNextPage() {
      return true;
   }


}
