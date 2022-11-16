package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
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
import java.util.List;
import java.util.Map;

public class ChileLidersuperCrawler extends CrawlerRankingKeywords {

   public ChileLidersuperCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = "https://www.lider.cl/supermercado/";

   private final List<String> proxies = Arrays.asList(
      ProxyCollection.NETNUT_RESIDENTIAL_MX,
      ProxyCollection.NETNUT_RESIDENTIAL_BR,
      ProxyCollection.SMART_PROXY_CL,
      ProxyCollection.SMART_PROXY_CL_HAPROXY,
      ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
      ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
      ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
      ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY);

   protected JSONObject fetchApi(String url) {

      Map<String, String> headers = new HashMap<>();
      headers.put("origin", "www.lider.cl");
      headers.put("referer", "www.lider.cl/");
      headers.put("content-lenght", "<calculated when request is sent>");
      headers.put("tenant", "supermercado");
      headers.put("authority", "apps.lider.cl");
      headers.put("accept-language", "en-US,en;q=0.9,pt;q=0.8,pt-PT;q=0.7");
      headers.put("accept", "application/json, text/plain, */*");
      headers.put("content-type", "application/json");
      headers.put("x-channel", "SOD");

      String payload = "{\"page\":" + this.currentPage + ",\"facets\":[],\"sortBy\":\"\",\"hitsPerPage\":16,\"query\":\"" + this.keywordWithoutAccents + "\"}";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setProxyservice(proxies)
         .setPayload(payload)
         .setHeaders(headers)
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new JsoupDataFetcher(), new FetcherDataFetcher(), this.dataFetcher), session, "post");

      return JSONUtils.stringToJson(response.getBody());

   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 16;
      this.log("Página " + this.currentPage);

      String url = "https://apps.lider.cl/supermercado/bff/search";

      this.log("Link onde são feitos os crawlers: " + url);
      JSONObject json = fetchApi(url);
      JSONArray products = json != null ? json.optJSONArray("products") : null;

      if (products != null && !products.isEmpty()) {
         if (this.totalProducts == 0) {
            this.totalProducts = json.optInt("nbHits");
         }

         for (Object o : products) {
            if (o instanceof JSONObject) {
               JSONObject product = (JSONObject) o;
               String internalId = product.optString("sku");
               String name = product.optString("displayName");
               String productUrl = mountProductUrl(internalId, name);
               String imageUrl = JSONUtils.getValueRecursive(product, "images.defaultImage", String.class);
               boolean available = product.optBoolean("available");
               Integer price = available ? JSONUtils.getValueRecursive(product, "price.BasePriceSales", Integer.class, 0) * 100 : null;

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setName(name)
                  .setPriceInCents(price)
                  .setAvailability(available)
                  .setImageUrl(imageUrl)
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

   private String mountProductUrl(String internalId, String name) {
      String url = HOME_PAGE + "product/sku/" + internalId + "/";

      if (name != null) {
         url += CommonMethods.toSlug(name);
      }

      return url;

   }


}
