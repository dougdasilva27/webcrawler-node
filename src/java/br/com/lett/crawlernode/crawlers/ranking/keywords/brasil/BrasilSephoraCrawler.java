package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class BrasilSephoraCrawler extends CrawlerRankingKeywords {

   public BrasilSephoraCrawler(Session session) {
      super(session);
   }

   public JSONObject crawlApi() {

      String apiUrl = "https://api.linximpulse.com/engage/search/v3/search?apiKey=sephora-br&page=" + this.currentPage + "&resultsPerPage=" + productsLimit + "&terms=" + this.keywordEncoded;

      this.log("Link onde são feitos os crawlers: " + apiUrl);

      Map<String, String> headers = new HashMap<>();
      headers.put("origin", "https://www.sephora.com.br");

      Request request = Request.RequestBuilder.create()
         .setUrl(apiUrl)
         .setHeaders(headers)
         .build();
      String content = this.dataFetcher
         .get(session, request)
         .getBody();

      return CrawlerUtils.stringToJson(content);

   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.log("Página " + this.currentPage);

      JSONObject json = crawlApi();
      JSONArray products = JSONUtils.getValueRecursive(json, "products", JSONArray.class);

      if (!products.isEmpty()) {

         for (Object obj : products) {
            if (obj instanceof JSONObject) {
               JSONObject product = (JSONObject) obj;

               String internalPid = product.optString("id");
               String url = product.optString("url");

               String productUrl = url != null ? "https://www.sephora.com.br" + url : null;

               String name = product.optString("name");
               String imgUrl = product.optJSONObject("images").optString("default");
               Integer price = getprice(product);

               boolean  isAvailable  = product.optString("status").equals("AVAILABLE") ? true : false;
               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setImageUrl(imgUrl)
                  .setPriceInCents(price)
                  .setAvailability(isAvailable)
                  .build();

               saveDataProduct(productRanking);

            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private Integer getprice(JSONObject product){
      String priceStr = product.optQuery("/price").toString();
      Double priceDouble = Double.parseDouble(priceStr)*100;
      return  priceDouble.intValue();
   }

}
