package br.com.lett.crawlernode.crawlers.ranking.keywords.panama;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class PanamaRibasmithCrawler extends CrawlerRankingKeywords {


   public PanamaRibasmithCrawler(Session session) {
      super(session);
   }


   private JSONObject searchJson() {
      Request request = Request.RequestBuilder.create()
         .setUrl("https://ultimate-dot-acp-magento.appspot.com/full_text_search?q=" + keywordEncoded + "&page_num=" + currentPage + "&UUID=19f6f39a-57d5-470f-8795-84369d66b79e")
         .build();

      String response = dataFetcher.get(session, request).getBody();

      return CrawlerUtils.stringToJson(response);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException {

      this.pageSize = 20;

      JSONObject searchResult = searchJson();

      if (searchResult != null && searchResult.has("items")) {

         if(this.totalProducts ==0){
            this.totalProducts = searchResult.optInt("total_results");
         }

         for (Object object : searchResult.optJSONArray("items")) {
            JSONObject products = (JSONObject) object;



            String internalId = products.getString("sku");
            String internalPId = products.getString("id");
            String url = products.getString("u");

            saveDataProduct(internalId, internalPId, url);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPId + " - Url: " + url);


         }


      } else {
         log("keyword sem resultado");
      }
   }

   @Override
   protected boolean hasNextPage() {
      return !(this.position>=this.totalProducts);
   }
}
