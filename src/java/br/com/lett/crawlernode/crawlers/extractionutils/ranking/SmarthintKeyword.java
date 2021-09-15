package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class SmarthintKeyword extends CrawlerRankingKeywords {

   private final String SH_key = session.getOptions().optString("SH_KEY");
   private final String version = session.getOptions().optString("version", "v5");


   public SmarthintKeyword(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException {

      this.pageSize = 12;

      JSONObject api = fetchApi();

      if (api.has("Products") && !api.optJSONArray("Products").isEmpty()) {
         JSONArray products = api.optJSONArray("Products");
         for (Object o : products) {
            JSONObject product = (JSONObject) o;

            String productUrl = CrawlerUtils.completeUrl(product.optString("Id"), "https", "");
            String internalId = product.optString("ProductId");
            String internalPid = product.optString("ItemGroupId");

            saveDataProduct(internalId, internalPid, productUrl);
            log("position: " + this.position + " internalId: " + internalId + " internalPid: " + internalPid + " url: " + productUrl);

         }


      } else {
         log("keyword sem resultado");
         this.result = false;
      }

   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }

   private JSONObject fetchApi() {
      Request request = Request.RequestBuilder.create()
         .setUrl("https://search.smarthint.co/" + version + "/Search/GetPrimarySearch?shcode=" + SH_key + "&term=" + this.keywordEncoded + "&from=" + (this.currentPage - 1) * this.pageSize + "&size=" + this.currentPage * this.pageSize + "&searchSort=0")
         .build();

      return CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
   }

}
