package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class SaopauloTendadriveCrawler extends CrawlerRankingKeywords {

   public SaopauloTendadriveCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      String url =
            "https://tenda-api.stoomlab.com.br/api/public/store/search?query="
                  + keywordEncoded
                  + "&page="
                  + currentPage
                  + "&order=relevance&save=true";

      JSONObject search = fetchJSONObject(url, cookies);

      JSONArray products = search.optJSONArray("products");

      if (products.length() > 0) {
         pageSize = search.optInt("products_per_page");
         if (this.totalProducts == 0) {
            totalProducts = search.optInt("total_products");
         }

         for (int i = 0; i < products.length(); i++) {
            JSONObject productJson = products.optJSONObject(i);

            String productId = productJson.optString("sku");
            String productUrl =
                  "https://www.tendaatacado.com.br/produto/" + productJson.optString("token", null);

            saveDataProduct(productId, productId, productUrl);

            this.log(
                  "Position: " + this.position + " - InternalId: " + productId + " - Url: " + productUrl);
         }
      }
   }


}
