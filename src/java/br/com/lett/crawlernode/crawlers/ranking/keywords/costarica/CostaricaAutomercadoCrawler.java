package br.com.lett.crawlernode.crawlers.ranking.keywords.costarica;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class CostaricaAutomercadoCrawler extends CrawlerRankingKeywords {


   private int numberPageResult = 0;

   public CostaricaAutomercadoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException {
      this.pageSize = 30;


      JSONObject apiJson = fetchProducts();

      if(apiJson!=null && !apiJson.isEmpty()) {
         JSONObject result = JSONUtils.getValueRecursive(apiJson,"results.0",JSONObject.class);
         numberPageResult = result.optInt("nbPages",0);
         JSONArray products = result.optJSONArray("hits");
         for (Object o : products) {
            JSONObject product = (JSONObject) o;

            String internalId = product.optString("productID");
            String internalPId = product.optString("productNumber");
            String url = "https://www.automercado.cr/shop/"+internalPId +"?objectID="+internalId;

            saveDataProduct(internalId,internalPId,url);

         }


      }


   }

   private JSONObject fetchProducts() {

      String payload ="{ \"requests\": [ { \"indexName\": \"Product_Catalogue\", \"params\": \"query="+this.keywordEncoded+"page="+this.currentPage+"\" } ] }";


      Request request  = Request.RequestBuilder.create()
         .setUrl("https://fu5xfx7knl-dsn.algolia.net/1/indexes/*/queries?x-algolia-api-key=113941a18a90ae0f17d602acd16f91b2&x-algolia-application-id=FU5XFX7KNL")
         .setPayload(payload)
         .build();

      return CrawlerUtils.stringToJson(dataFetcher.post(session,request).getBody());

   }
}
