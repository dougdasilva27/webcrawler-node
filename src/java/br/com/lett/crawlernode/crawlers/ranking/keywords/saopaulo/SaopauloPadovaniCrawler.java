package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SaopauloPadovaniCrawler extends CrawlerRankingKeywords {

   public SaopauloPadovaniCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 12;
      this.log("Página " + this.currentPage);

      JSONObject gridInfo = scrapProductGridFromApi();

      JSONArray products = gridInfo.optJSONArray("products");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts(gridInfo);
         }

         for (Object e : products) {

            JSONObject product = (JSONObject) e;

            String internalId = product.optString("id");
            String urlProduct = "https:" + product.optString("url");
            saveDataProduct(internalId, null, urlProduct);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + urlProduct);
            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
      if (!(hasNextPage()))
         setTotalProducts();

   }


   private JSONObject scrapProductGridFromApi(){
      String apiURL = "https://api.linximpulse.com/engage/search/v3/search?apiKey=padovani&page=" + this.currentPage + "&resultsPerPage=30&terms=" + this.keywordEncoded + "&sortBy=relevance";

      this.log("Link onde são feitos os crawlers: " + apiURL);


      Map<String,String> headers = new HashMap<>();
      headers.put("origin","https://www.padovani.com.br");

      Request request = Request.RequestBuilder.create().setUrl(apiURL).setHeaders(headers).build();
      String resp = this.dataFetcher.get(session,request).getBody();


      return JSONUtils.stringToJson(resp);
   }


   protected void setTotalProducts(JSONObject json) {
      this.totalProducts = json.optInt("size");
      this.log("Total da busca: " + this.totalProducts);
   }
}
