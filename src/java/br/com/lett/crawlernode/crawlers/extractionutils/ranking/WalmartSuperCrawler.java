package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WalmartSuperCrawler extends CrawlerRankingKeywords {

   public WalmartSuperCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.JSOUP;
   }

   String store_id = session.getOptions().optString("store_id");

   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 10;

      this.log("Página " + this.currentPage);
      String url = "https://super.walmart.com.mx/api/assembler/v2/page/search?Ntt=" + this.keywordEncoded + "&No=" + (this.currentPage - 1) +
         "&Nrpp=10&storeId=" + store_id + "&profileId=NA";

      this.log("Link onde são feitos os crawlers: " + url);

      JSONObject search = fetchJSONApi(url);
      JSONObject resultList = JSONUtils.getValueRecursive(search, "appendix.ResultsList", JSONObject.class);
      JSONArray products = resultList.optJSONArray("content");

      if (products != null && !products.isEmpty()) {

         for (Object o : products) {
            if (o instanceof JSONObject) {
               JSONObject product = (JSONObject) o;
               if (this.totalProducts == 0) {
                  setTotalProducts(resultList);
               }

               String productUrl = CrawlerUtils.completeUrl(product.optString("productSeoUrl"), "https", "super.walmart.com.mx");
               String internalId = product.optString("id");
               String name = product.optString("skuDisplayName");
               String imageUrl = CrawlerUtils.completeUrl(JSONUtils.getValueRecursive(product, "imageUrls.small", String.class), "https", "super.walmart.com.mx");
               int price = CommonMethods.doublePriceToIntegerPrice(product.optDouble("skuPrice"), 0);
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
      if (search.has("totalElements")) {
         this.totalProducts = search.getInt("totalElements");
         this.log("Total da busca: " + this.totalProducts);
      }
   }


   private JSONObject fetchJSONApi(String url) {

      String referer = "https://super.walmart.com.mx/productos?Ntt=" + this.keywordEncoded;

      Map<String, String> headers = new HashMap<>();
      headers.put("Host", "super.walmart.com.mx");
      headers.put(HttpHeaders.CONNECTION, "keep-alive");
      headers.put("x-dtreferer", referer);
      headers.put(HttpHeaders.ACCEPT, "application/json");
      headers.put("Content-Type", "application/json");
      headers.put(HttpHeaders.REFERER, referer);
      headers.put("Accept-Encoding", "");
      headers.put("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
      headers.put("Cache-Control", "no-cache");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_ANY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY))
         .mustSendContentEncoding(false)
         .build();

      String response = CrawlerUtils.retryRequestString(request, List.of(new JsoupDataFetcher(), new ApacheDataFetcher(), new FetcherDataFetcher()), session);
      return CrawlerUtils.stringToJson(response);

   }
}
