package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MexicoWalmartCrawler extends CrawlerRankingKeywords {

   public MexicoWalmartCrawler(Session session) {
      super(session);
   }

   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 48;

      this.log("Página " + this.currentPage);
      String url = "https://www.walmart.com.mx/api/v2/page/search?Ntt=" + this.keywordEncoded + "&page=" + (this.currentPage - 1) + "&size=" + this.pageSize;

      this.log("Link onde são feitos os crawlers: " + url);

      JSONObject search = fetchJSONApi(url);

      JSONArray results = JSONUtils.getJSONArrayValue(search, "content");
      if (results != null && !results.isEmpty()) {

         for (Object o : results) {
            if (o instanceof JSONObject) {
               JSONObject product = (JSONObject) o;
               if (this.totalProducts == 0) {
                  setTotalProducts(search);
               }
               String productUrl = CrawlerUtils.completeUrl(product.optString("productSeoUrl"), "https", "www.walmart.com.mx");
               String internalId = product.optString("productId");
               String name = product.optString("skuDisplayName");
               String imageUrl = CrawlerUtils.completeUrl(JSONUtils.getValueRecursive(product, "imageUrls.large", String.class), "https", "www.walmart.com.mx");
               Integer price = CommonMethods.doublePriceToIntegerPrice(JSONUtils.getDoubleValueFromJSON(product, "skuPrice", false), 0);
               boolean isAvailable = price != 0;
               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setName(name)
                  .setPriceInCents(price)
                  .setAvailability(isAvailable)
                  .setImageUrl(imageUrl)
                  .build();

               saveDataProduct(productRanking);
            }

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

   protected void setTotalProducts(JSONObject search) {
      this.totalProducts = search.optInt("totalElements", 0);
      this.log("Total da busca: " + this.totalProducts);
   }


   private JSONObject fetchJSONApi(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");

      Request request = RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY))
         .build();

      Response response = this.dataFetcher.get(session, request);

      JSONObject json = CrawlerUtils.stringToJson(response.getBody());

      return JSONUtils.getValueRecursive(json, "appendix.SearchResults", JSONObject.class);

   }
}
