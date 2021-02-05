package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class ArgentinaCordiezCrawler  extends CrawlerRankingKeywords {

   public ArgentinaCordiezCrawler(Session session) {
      super(session);
   }

   private JSONArray getProductsFromApi(){
      String urlApi = "https://www.cordiez.com.ar/api/catalog_system/pub/products/search?ft="+this.keywordEncoded;

      Request request =  Request.RequestBuilder.create().setUrl(urlApi).build();
      String resp = this.dataFetcher.get(session,request).getBody();

      return JSONUtils.stringToJsonArray(resp);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      JSONArray products = getProductsFromApi();

      if( products.length() > 0) {
         for (Object o : products) {
            JSONObject product = (JSONObject) o;

            String internalId = product.optString("productId");
            String productUrl = product.optString("link");


            saveDataProduct(internalId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalId + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit) break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
   }
}
