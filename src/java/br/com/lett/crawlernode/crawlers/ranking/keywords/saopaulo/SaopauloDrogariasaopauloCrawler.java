package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

import java.util.HashMap;
import java.util.Map;

public class SaopauloDrogariasaopauloCrawler extends CrawlerRankingKeywords {

   public SaopauloDrogariasaopauloCrawler(Session session) {
      super(session);
   }

   private JSONObject extractJsonFromApi() {
      String urlApi = "https://api.linximpulse.com/engage/search/v3/search?apiKey=drogariasaopaulo&productFormat=complete&resultsPerPage=48&page="+this.currentPage+"&terms="+keywordEncoded;

      Map<String,String> headers = new HashMap<>();
      headers.put("origin","https://www.drogariasaopaulo.com.br");

      Request request = Request.RequestBuilder.create()
         .setUrl(urlApi)
         .setHeaders(headers)
         .build();

      String response = dataFetcher.get(session,request).getBody() ;

      return CrawlerUtils.stringToJson(response);

   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 48;
      this.log("Página " + this.currentPage);

      JSONObject json = extractJsonFromApi();

      JSONArray products = json.optJSONArray("products");

      if (products != null && !products.isEmpty()) {
         for (Object obj : products) {
            if (obj instanceof JSONObject) {
               JSONObject product = (JSONObject) obj;

               String internalPid = product.optString("id") ;
               String internalId = internalPid;
               String productName = product.optString("name");
               String url = JSONUtils.getValueRecursive(product,"skus.0.properties.url", String.class);
               String productUrl = CrawlerUtils.completeUrl(url,"https","www.drogariasaopaulo.com.br");
               String productImg = JSONUtils.getValueRecursive(product,"images.1000x1000", String.class);
               Integer productPrice = getPrice(product.optInt("price"));
               boolean isAvailable = product.optString("status").equals("AVAILABLE");

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(productName)
                  .setImageUrl(productImg)
                  .setPriceInCents(productPrice)
                  .setAvailability(isAvailable)
                  .build();

               saveDataProduct(productRanking);

               this.log(
                  "Position: " + this.position +
                     " - InternalId: " + internalId +
                     " - internalPid: " + internalPid +
                     " - name: " + productName +
                     " - Url: " + productUrl);

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

   private int getPrice(int price) {
      return (price != 0) ? (price * 100) : 0;
   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }
}
