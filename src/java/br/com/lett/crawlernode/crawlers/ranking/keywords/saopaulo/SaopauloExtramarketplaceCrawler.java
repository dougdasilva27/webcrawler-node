package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class SaopauloExtramarketplaceCrawler extends CrawlerRankingKeywords {

   public SaopauloExtramarketplaceCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   private static final String API_KEY = "umQBi56tVoP0MBX8h%2B3ZtA%3D%3D";
   protected String marketHost = "www.extra.com.br";
   protected static final String PROTOCOL = "https";

   @Override
   public void extractProductsFromCurrentPage() {
      this.pageSize = 20;
      this.log("Página " + this.currentPage);

      JSONObject search = fetchProductsFromAPI();

      // se obter 1 ou mais links de produtos e essa página tiver resultado
      if (search.has("products") && search.getJSONArray("products").length() > 0) {
         JSONArray products = search.getJSONArray("products");

         if (this.totalProducts == 0) {
            setTotalProducts(search);
         }

         for (int i = 0; i < products.length(); i++) {
            JSONObject product = products.getJSONObject(i);

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

   @Override
   protected boolean hasNextPage() {
      return this.arrayProducts.size() < this.totalProducts;
   }

   protected void setTotalProducts(JSONObject search) {
      if (search.has("size") && search.get("size") instanceof Integer) {
         this.totalProducts = CrawlerUtils.getIntegerValueFromJSON(search, "size", 0);
         this.log("Total da busca: " + this.totalProducts);
      }
   }

   private JSONObject fetchProductsFromAPI() {
      String url = new StringBuilder()
            .append("https://api.linximpulse.com/engage/search/v3/search/")
            .append("?apiKey=extra")
            .append("&secretKey=" + API_KEY)
            .append("&resultsPerPage=21")
            .append("&terms=" + this.keywordEncoded)
            .append("&page=" + this.currentPage)
            .toString();

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");

      Request request = RequestBuilder.create()
            .setUrl(url)
            .setCookies(cookies)
            .setHeaders(headers)
            .build();

      return CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
   }
}
