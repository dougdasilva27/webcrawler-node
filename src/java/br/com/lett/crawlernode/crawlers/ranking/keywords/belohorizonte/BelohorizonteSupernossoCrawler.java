package br.com.lett.crawlernode.crawlers.ranking.keywords.belohorizonte;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

public class BelohorizonteSupernossoCrawler extends CrawlerRankingKeywords {

   public BelohorizonteSupernossoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 24;

      this.log("Página " + this.currentPage);

      JSONObject json = fetchApi();
      JSONArray products = json.optJSONArray("products");

      if (products != null && !products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts(json);
         }

         for (Object obj : products) {
            JSONObject product = (JSONObject) obj;
            String internalPid = product.optString("id");
            String productUrl = product.optString("url");

            saveDataProduct(null, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);

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

   private void setTotalProducts(JSONObject json) {
      this.totalProducts = json.optInt("size", 0);
      this.log("Total da busca: " + this.totalProducts);
   }

   private JSONObject fetchApi() {
      String url = "https://api.linximpulse.com/engage/search/v3/search?apiKey=supernossoemcasa&page=" + this.currentPage + "&resultsPerPage=24&terms=" + this.keywordEncoded + "&salesChannel=1";
      this.log("Link onde são feitos os crawlers: " + url);

      Map<String, String> headers = new HashMap<>();
      headers.put("Origin", "https://www.supernossoemcasa.com.br");

      Request request = Request.RequestBuilder.create()
         .setHeaders(headers)
         .setUrl(url)
         .build();

      String response = dataFetcher.get(session, request).getBody();
      return CrawlerUtils.stringToJson(response);
   }
}
