package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BrasilDrogariapachecoCrawler extends CrawlerRankingKeywords {

   public BrasilDrogariapachecoCrawler(Session session) {
      super(session);
   }

   private JSONObject extractJsonFromApi() {

      Map<String, String> headers = new HashMap<>();
      headers.put("origin", "https://www.drogariaspacheco.com.br");

      String url = "https://api.linximpulse.com/engage/search/v3/search?apiKey=drogariaspacheco&productFormat=complete&resultsPerPage=48&page="
         + this.currentPage
         + "&_from="
         + "&terms="
         + this.keywordEncoded;

      Request request = RequestBuilder.create().setUrl(url).setHeaders(headers).build();
      String body = this.dataFetcher.get(session, request).getBody();

      return JSONUtils.stringToJson(body);

   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 48;

      this.log("Página" + this.currentPage);

      JSONObject json = extractJsonFromApi();
      JSONArray productsArray = json.optJSONArray("products");

      if (productsArray != null && !productsArray.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts(json);
         }
         for (Object obj : productsArray) {
            if (obj instanceof JSONObject) {
               JSONObject product = (JSONObject) obj;

               String productUrl = getUrl(product);
               String internalPid = product.optString("id");
               String internalId = internalPid;

               saveDataProduct(null, internalPid, productUrl);

               this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);


               if (this.arrayProducts.size() == productsLimit) {
                  break;
               }
            }
         }
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   private String getUrl(JSONObject product) {
      String productUrl = null;
      String incompleteUrl = product.optString("url");
      if (incompleteUrl != null) {
         productUrl = "https://" + incompleteUrl;
      }

      return productUrl;
   }


   protected void setTotalProducts(JSONObject json) {
      this.totalProducts = json.optInt("size");
   }
}
