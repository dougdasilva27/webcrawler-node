package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class MexicoBodegaaurreraCrawler extends CrawlerRankingKeywords {

   public MexicoBodegaaurreraCrawler(Session session) {
      super(session);
   }

   private JSONObject fetchJSON() {
      String url = "https://www.bodegaaurrera.com.mx/api/v2/page/search?Ntt=consola&size=48&page=" + (this.currentPage - 1) + "&siteId=bodega&featureFlags=EVG" + this.keywordEncoded;
      this.log("Link onde são feitos os crawlers: " + url);

      Request request = Request.RequestBuilder.create()
         .setCookies(cookies)
         .setUrl(url)
         .build();

      Response response = dataFetcher.get(session, request);

      return CrawlerUtils.stringToJson(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      JSONObject json = fetchJSON();

      JSONArray results = JSONUtils.getValueRecursive(json, "appendix.SearchResults.content", JSONArray.class);

      if (results != null && !results.isEmpty()) {
         if (currentPage == 1) {
            this.totalProducts = JSONUtils.getValueRecursive(json, "appendix.SearchResults.totalElements", Integer.class);
         }

         for (Object prod : results) {
            if (prod instanceof JSONObject) {
               JSONObject product = (JSONObject) prod;

               String internalPid = product.optString("id");
               String productUrl = "https://www.bodegaaurrera.com.mx" + product.optString("productSeoUrl");

               saveDataProduct(null, internalPid, productUrl);
               this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
               if (this.arrayProducts.size() == productsLimit)
                  break;
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

}
