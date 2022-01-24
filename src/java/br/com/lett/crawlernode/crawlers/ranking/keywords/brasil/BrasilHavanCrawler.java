package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BrasilHavanCrawler extends CrawlerRankingKeywords {

   public BrasilHavanCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {

      this.pageSize = 24;

      this.log("Página " + this.currentPage);

      JSONObject jsonObject = fetchApi();

      JSONArray productsArr = jsonObject != null ? jsonObject.optJSONArray("products") : null;


      if (productsArr != null && !productsArr.isEmpty()) {

         if (this.totalProducts == 0) {
            setTotalProducts(jsonObject);
         }

         for (Object o : productsArr) {

            if (o instanceof JSONObject) {
               JSONObject product = (JSONObject) o;

               Object internalPidObject = product.optQuery("/skus/0/properties/details/idAddToCart");
               String internalPid = internalPidObject != null ? internalPidObject.toString() : null;
               String internalId = product.optString("id");
               String partUrl = product.optString("url");
               String productUrl = partUrl != null ? "https:" + partUrl : null;
               String name = product.optString("name");
               Object imageUrlObject = product.optQuery("/images/default").toString();
               String imageUrl = imageUrlObject != null ? imageUrlObject.toString() : "";
               Integer priceInCents = (int) Math.round(product.optDouble("price", 0d)  * 100);
               boolean isAvailable = product.optString("status").equalsIgnoreCase("AVAILABLE");

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setImageUrl(imageUrl)
                  .setName(name)
                  .setPriceInCents(priceInCents)
                  .setAvailability(isAvailable)
                  .build();

               saveDataProduct(productRanking);
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


   private JSONObject fetchApi() {
      String url = "https://api.linximpulse.com/engage/search/v3/search?apiKey=havan&page=" + this.currentPage + "&resultsPerPage=36&terms=" + this.keywordEncoded + "&sortBy=relevance";

      Map<String, String> headers = new HashMap<>();
      headers.put("origin", "https://www.havan.com.br/");
      headers.put("authority", "api.linximpulse.com");
      headers.put("referer", "https://www.havan.com.br/");

      Request request = Request.RequestBuilder.create()
         .setHeaders(headers)
         .setSendUserAgent(true)
         .setUrl(url)
         .build();

      String response = dataFetcher.get(session, request).getBody();
      return CrawlerUtils.stringToJson(response);
   }

   protected void setTotalProducts(JSONObject jsonObject) {

      this.totalProducts = jsonObject.optInt("pagination");


      this.log("Total da busca: " + this.totalProducts);
   }

}
