package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Array;
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
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 48;

      this.log("Página" + this.currentPage);

      JSONObject json = extractJsonFromApi();
      JSONArray products = json.optJSONArray("products");

      if (products != null && !products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts(json);
         }
         for (Object obj : products) {
            if (obj instanceof JSONObject) {
               JSONObject product = (JSONObject) obj;

               String productUrl = getUrl(product);
               String internalPid = product.optString("id");
               String internalId = internalPid;
               String productName = product.optString("name");
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

   private String getUrl(JSONObject product) {
      String urlApi = product.optString("url");

      String productUrl = (urlApi != null ) ? "https://" + urlApi : null;

      return productUrl;
   }

   private int getPrice(int price) {
      return (price != 0) ? (price * 100) : 0;
   }

   protected void setTotalProducts(JSONObject json) {
      this.totalProducts = json.optInt("size");
   }
}
