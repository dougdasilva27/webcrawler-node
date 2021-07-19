package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public abstract class CarrefourCrawler extends CrawlerRankingKeywords {

   public CarrefourCrawler(Session session) {
      super(session);
      dataFetcher = new JsoupDataFetcher();
      this.pageSize = 12;
   }

   @Override
   protected void processBeforeFetch() {
      super.processBeforeFetch();

      if (getCep() != null) {
         BasicClientCookie userLocationData = new BasicClientCookie("userLocationData", getCep());
         userLocationData.setPath("/");
         cookies.add(userLocationData);
      }

      if (getLocation() != null) {
         BasicClientCookie vtexSegment = new BasicClientCookie("vtex_segment", getLocation());
         vtexSegment.setPath("/");
         this.cookies.add(vtexSegment);
      }
   }

   protected abstract String getHomePage();

   protected abstract String getLocation();

   protected String getCep() {
      return null;
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);

      String homePage = getHomePage();
      String url = homePage + "api/catalog_system/pub/products/search/" + keywordEncoded.replace("+", "%20") + "?_from=" + ((currentPage - 1) * pageSize) +
         "&_to=" + ((currentPage) * pageSize);

      String body = fetchPage(url);
      JSONArray products = CrawlerUtils.stringToJsonArray(body);

      if (products.length() > 0) {
         for (Object object : products) {

            JSONObject product = (JSONObject) object;
            String productUrl = product.optString("link");
            String internalPid = product.optString("productId");

            saveDataProduct(null, internalPid, productUrl);
            this.log("Position: " + this.position + " - InternalPid: " + internalPid + " - Url: " + productUrl);

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      }
   }

   protected String fetchPage(String url) {

      Map<String, String> headers = new HashMap<>();

      String token = getLocation();

      String userLocationData = getCep();
      headers.put("accept", "*/*");
      headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_0_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Safari/537.36");

      StringBuilder cookieBuffer = new StringBuilder();
      if (token != null) {
         cookieBuffer.append("vtex_segment=").append(token).append(";");
      }
      if (token != null) {
         cookieBuffer.append("userLocationData=").append(userLocationData).append(";");
      }
      headers.put("cookie", cookieBuffer.toString());

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setSendUserAgent(false)
         .mustSendContentEncoding(false)
         .setFetcheroptions(
            FetcherOptions.FetcherOptionsBuilder.create()
               .mustUseMovingAverage(false)
               .mustRetrieveStatistics(true)
               .build())
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.INFATICA_RESIDENTIAL_BR,
            ProxyCollection.LUMINATI_SERVER_BR)
         )
         .build();

      Response response = dataFetcher.get(session, request);
      return response.getBody();
   }

   @Override
   protected boolean hasNextPage() {
      return ((arrayProducts.size() - 1) % pageSize - currentPage) < 0;
   }
}
