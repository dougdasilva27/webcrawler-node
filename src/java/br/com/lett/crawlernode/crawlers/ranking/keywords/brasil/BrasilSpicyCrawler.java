package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.HashMap;
import java.util.Map;

import br.com.lett.crawlernode.core.session.ranking.RankingSession;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilSpicyCrawler extends CrawlerRankingKeywords {

   public BrasilSpicyCrawler(Session session) {
      super(session);
   }

   private static final String SMART_CODE = "SH-520465";

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 100;
      this.log("Página " + this.currentPage);

      JSONObject apiResponse = fetchApi();
      JSONArray products = apiResponse.optJSONArray("Products");

      if (products != null && !products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts(apiResponse);
         }


         for (Object element : products) {
            JSONObject product = (JSONObject) element;

            String internalPid = JSONUtils.getStringValue(product,"ItemGroupId");
            String internalId = JSONUtils.getStringValue(product,"ProductId");
            String productUrl = CrawlerUtils.completeUrl(product.optString("Link"), "https:", "www.spicy.com.br");

            saveDataProduct(internalId, internalPid, productUrl);

            this.log("Poition: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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

   protected void setTotalProducts(JSONObject apiResponse) {
      this.totalProducts = apiResponse.optInt("TotalResult", 0);
      this.log("Total products: " + this.totalProducts);
   }

   private JSONObject fetchApi() {
      String url = "https://search.smarthint.co/v2/Search/GetPrimarySearch?"
            + "shcode=" + SMART_CODE
            + "&term=" + this.keywordEncoded
            + "&from=" + this.arrayProducts.size()
            + "&size=" + this.pageSize
            + "&searchSort=0";
      this.log("Link onde são feitos os crawlers: " + url);

      Map<String, String> headers = new HashMap<>();

      Request request = Request.RequestBuilder.create()
            .setHeaders(headers)
            .setUrl(url)
            .build();

      String response = dataFetcher.get(session, request).getBody();

      JSONObject apiResponse = new JSONObject();

      if (response.contains("([\"") && response.contains("\"])")) {
         int firstIndex = response.indexOf("([\"") + 3;
         int lastIndex = response.indexOf("\"])", firstIndex);

         apiResponse = CrawlerUtils.stringToJson(response.substring(firstIndex, lastIndex).replace("\\\"", "\"").replace("\\\\\"", "\\\""));
      }
      else if(!response.isEmpty()) {
         apiResponse = CrawlerUtils.stringToJson(response);
      }

      return apiResponse;
   }
}
