package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class BrasilIbyteCrawler extends CrawlerRankingKeywords {

   public BrasilIbyteCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 15;

      this.log("Página " + this.currentPage);

      String url = "https://search.smarthint.co/v2/Search/GetPrimarySearch?shcode=SH-150292&term=" + this.keywordEncoded + "&from=" + (this.position - 1) + "&size=15";
      this.log("Link onde são feitos os crawlers: " + url);

      //chama função de pegar a url
      JSONObject obj = fetch(url);

      JSONArray products = obj.optJSONArray("Products");

      if (!products.isEmpty()) {
         for (Object o : products) {
            JSONObject object = (JSONObject) o;

            String internalPid = object.optString("ItemGroupId");
            String internalId = object.optString("PorductId");
            String urlProduct = object.optString("Id");

            saveDataProduct(internalId, internalPid, urlProduct);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
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

   private JSONObject fetch(String url) {
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .build();

      return CrawlerUtils.stringToJson(dataFetcher.get(session, request).getBody());
   }
}
