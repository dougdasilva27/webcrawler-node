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
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ChileUnimarcCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "https://www.unimarc.cl";
   private Integer page;
   private Integer endPage;

   public ChileUnimarcCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.APACHE;
      totalProducts = -1;
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {

      Integer toPage = (totalProducts == -1 || totalProducts > (currentPage * 50)) ? currentPage * 50 : totalProducts;
      String url = "https://bff-unimarc-web.unimarc.cl/bff-api/products/intelligence-search/" + keywordEncoded.replace("+", "%20") + "/?from=" + (this.currentPage - 1) * 50 + "&to=" + toPage + "&hideUnavailableItems=1";


      JSONArray products = fetchProducts(url);


      for (Object object : products) {

         JSONObject product = (JSONObject) object;
         String urlEnd = product.optString("detailUrl");
         urlEnd = urlEnd.replaceAll("\\/p", "");
         String productUrl = HOME_PAGE + "/product" + urlEnd;
         String internalPid = product.optString("productId");
         String name = product.optString("name");
         Number numberPrice = JSONUtils.getValueRecursive(product, "sellers.0.price", Number.class);
         Double doublePrice = numberPrice != null ? numberPrice.doubleValue() : null;
         Integer price = doublePrice != null ? (int) (doublePrice * 100) : null;
         String imageUrl = JSONUtils.getValueRecursive(product, "images.0", String.class);
         RankingProduct productRanking = RankingProductBuilder.create()
            .setUrl(productUrl)
            .setInternalPid(internalPid)
            .setName(name)
            .setPriceInCents(price)
            .setAvailability(price != null)
            .setImageUrl(imageUrl)
            .build();

         saveDataProduct(productRanking);
         if (this.arrayProducts.size() == productsLimit) {
            break;
         }
      }
   }

   protected JSONArray fetchProducts(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "*/*");
      headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_0_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Safari/537.36");
      headers.put("Accept-Encoding","gzip, deflate, br");
      headers.put("Connection","keep-alive");

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
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR)
         )
         .build();

      Response response = dataFetcher.get(session, request);
      JSONObject jsonResponse = CrawlerUtils.stringToJson(response.getBody());
      String totalString = JSONUtils.getValueRecursive(jsonResponse, "data.resources", String.class);
      this.totalProducts = Integer.parseInt(totalString);
      JSONArray value = JSONUtils.getValueRecursive(jsonResponse, "data.availableProducts", JSONArray.class);

      return value;
   }

   @Override
   protected boolean hasNextPage() {
      return ((currentPage) * 50) < this.totalProducts;
   }
}
