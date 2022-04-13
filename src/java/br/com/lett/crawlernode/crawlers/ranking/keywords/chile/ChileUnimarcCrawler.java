package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ChileUnimarcCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "https://www.unimarc.cl/";

   public ChileUnimarcCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.APACHE;
      pageSize = 24;
   }

   @Override
   protected void processBeforeFetch() {
      BasicClientCookie vtexSegment = new BasicClientCookie("vtex_segment", "eyJjYW1wYWlnbnMiOm51bGwsImNoYW5uZWwiOiIxIiwicHJpY2VUYWJsZXMiOm51bGwsInJlZ2lvbklkIjpudWxsLCJ1dG1fY2FtcGFpZ24iOm51bGwsInV0bV9zb3VyY2UiOm51bGwsInV0bWlfY2FtcGFpZ24iOm51bGwsImN1cnJlbmN5Q29kZSI6IkNMUCIsImN1cnJlbmN5U3ltYm9sIjoiJCIsImNvdW50cnlDb2RlIjoiQ0hMIiwiY3VsdHVyZUluZm8iOiJlcy1DTCIsImFkbWluX2N1bHR1cmVJbmZvIjoiZXMtQ0wiLCJjaGFubmVsUHJpdmFjeSI6InB1YmxpYyJ9");
      vtexSegment.setPath("/");
      this.cookies.add(vtexSegment);
   }


   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      String url = HOME_PAGE + keywordEncoded.replace("+", "%20") + "?q_=" +
         keywordEncoded.replace("+", "%20") + "&__pickRuntime=page,queryData&map=ft&page=" + currentPage;

      JSONArray products = fetchProducts(url);

      for (Object object : products) {

         JSONObject product = (JSONObject) object;
         String productUrl = HOME_PAGE + product.optString("linkText") + "/p";
         String internalPid = product.optString("productId");
         String name = product.optString("productName");
         Number numberPrice = JSONUtils.getValueRecursive(product, "priceRange.sellingPrice.lowPrice", Number.class);
         Double doublePrice = numberPrice != null ? numberPrice.doubleValue() : null;
         Integer price = doublePrice != null ? (int) (doublePrice * 100) : null;
         String imageUrl = JSONUtils.getValueRecursive(product, "items.0.images.0.imageUrl", String.class);

         RankingProduct productRanking = RankingProductBuilder.create()
            .setUrl(productUrl)
            .setInternalPid(internalPid)
            .setName(name)
            .setPriceInCents(price)
            .setAvailability(price != null)
            .setImageUrl(imageUrl)
            .build();

         saveDataProduct(productRanking);


      }
   }

   protected JSONArray fetchProducts(String url) {
      Map<String, String> headers = new HashMap<>();

      headers.put("accept", "*/*");
      headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_0_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Safari/537.36");


      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setCookies(cookies)
         .setSendUserAgent(false)
         .mustSendContentEncoding(false)
         .setFetcheroptions(
            FetcherOptions.FetcherOptionsBuilder.create()
               .mustUseMovingAverage(false)
               .mustRetrieveStatistics(true)
               .build())
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.LUMINATI_SERVER_BR)
         )
         .build();

      Response response = dataFetcher.get(session, request);
      JSONObject jsonResponse = CrawlerUtils.stringToJson(response.getBody());
      String data = JSONUtils.getValueRecursive(jsonResponse, "queryData.0.data", String.class);
      JSONObject jsonData = CrawlerUtils.stringToJson(data);
      return JSONUtils.getValueRecursive(jsonData, "productSearch.products", JSONArray.class);
   }

   @Override
   protected boolean hasNextPage() {
      return ((arrayProducts.size() - 1) % pageSize - currentPage) < 0;
   }
}
