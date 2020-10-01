package br.com.lett.crawlernode.crawlers.ranking.keywords.romania;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class RomaniaCoraCrawler extends CrawlerRankingKeywords {

   private static final String HOST_PAGE = "https://www.cora.ro/";

   public RomaniaCoraCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   private JSONArray itemsOnPage = new JSONArray();

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 10;
      this.log("Página " + this.currentPage);


      JSONObject json = fetchJSON();
      JSONArray items = json.optJSONArray("items");

      itemsOnPage = new JSONArray();

      if (items != null && !items.isEmpty()) {
         if (totalProducts == 0) {
            setTotalProducts(json);
         }
         for (Object e : items) {

            if (e instanceof JSONObject) {
               JSONObject item = (JSONObject) e;
               JSONObject entity = item.optJSONObject("entity");
               String internalId = entity.optString("id");
               String internalPid = entity.optString("partnumber");

               itemsOnPage.put(internalPid);

               String productUrl = HOST_PAGE + entity.optString("seoUrl");

               saveDataProduct(internalId, internalPid, productUrl);

               this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
               if (this.arrayProducts.size() == productsLimit) {
                  break;
               }
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private void setTotalProducts(JSONObject json) {
      this.totalProducts = json.optInt("totalResults", 0);
      this.log("Total products: " + this.totalProducts);
   }

   private JSONObject fetchJSON() {

      String url = "https://www.cora.ro/rest/search";
      this.log("Link onde são feitos os crawlers: " + url);

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json;charset=UTF-8");
      headers.put("Host", "www.cora.ro");
      headers.put("Origin", "https://www.cora.ro");

      JSONObject payload = new JSONObject();

      JSONObject pager = new JSONObject();
      pager.put("page", currentPage);

      payload.put("pager", pager);
      payload.put("filters", new JSONArray());
      payload.put("itemsOnPage", itemsOnPage);
      payload.put("queryStr", keywordEncoded);

      Request request = Request.RequestBuilder.create()
            .setUrl(url)
            .setHeaders(headers)
            .setCookies(cookies)
            .mustSendContentEncoding(true)
            .setPayload(payload.toString())
            .build();

      String page = this.dataFetcher.post(session, request).getBody();
      return CrawlerUtils.stringToJson(page);
   }
}
