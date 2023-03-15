package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilCarrefourFetchUtils;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.MathUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CarrefourMercadoRanking extends CrawlerRankingKeywords {

   public CarrefourMercadoRanking(Session session) {
      super(session);
      dataFetcher = new JsoupDataFetcher();
      this.pageSize = 20;
   }

   public static final String HOME_PAGE = "https://mercado.carrefour.com.br/";

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

   protected String getHomePage() {
      return HOME_PAGE;
   }

   protected String getLocation() {
      return session.getOptions().optString("vtex_segment");
   }

   protected String getCep() {
      return this.session.getOptions().optString("cep");
   }

   protected String getRegionId() {
      return session.getOptions().optString("regionId");
   }

   private JSONObject fetchResponse() {
      String regionId = getRegionId();

      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "*/*");
      headers.put("authority", "mercado.carrefour.com.br");
      headers.put("referer", "https://mercado.carrefour.com.br/");
      headers.put("accept-encoding", "gzip, deflate, br");
      //String url = "https://mercado.carrefour.com.br/api/graphql?operationName=ProductsQuery&variables={\"first\":20,\"after\":\"" + (this.currentPage - 1) * this.pageSize + "\",\"sort\":\"score_desc\",\"term\":\"" + this.keywordWithoutAccents + "\",\"selectedFacets\":[{\"key\":\"region-id\",\"value\":\"" + regionId + "\"},{\"key\":\"channel\",\"value\":\"{\\\"salesChannel\\\":\\\"2\\\",\\\"regionId\\\":\\\"" + regionId + "\\\"}\"},{\"key\":\"locale\",\"value\":\"pt-BR\"}]}";
      String url = "https://mercado.carrefour.com.br/api/graphql?operationName=ProductsQuery&variables=%7B%22first%22%3A20%2C%22after%22%3A%22" + (this.currentPage - 1) * this.pageSize + "%22%2C%22sort%22%3A%22score_desc%22%2C%22term%22%3A%22" + this.keywordEncoded + "%22%2C%22selectedFacets%22%3A%5B%7B%22key%22%3A%22region-id%22%2C%22value%22%3A%22" + regionId + "%22%7D%2C%7B%22key%22%3A%22channel%22%2C%22value%22%3A%22%7B%5C%22salesChannel%5C%22%3A%5C%222%5C%22%2C%5C%22regionId%5C%22%3A%5C%22v2.BF5CB0A0B2E63790F00315745EDC0A60%5C%22%7D%22%7D%2C%7B%22key%22%3A%22locale%22%2C%22value%22%3A%22pt-BR%22%7D%5D%7D";
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setCookies(this.cookies)
         .setProxyservice(Arrays.asList(
            ProxyCollection.LUMINATI_SERVER_BR_HAPROXY,
            ProxyCollection.LUMINATI_SERVER_BR,
            ProxyCollection.LUMINATI_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.SMART_PROXY_BR,
            ProxyCollection.BUY,
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY
         ))
         .build();

      return CrawlerUtils.stringToJSONObject(CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new FetcherDataFetcher(), new JsoupDataFetcher()), session, "get").getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 20;
      this.log("Página " + this.currentPage);

      JSONObject jsonResponse = fetchResponse();

      if (currentPage == 1) {
         this.totalProducts = JSONUtils.getValueRecursive(jsonResponse, "data.search.products.pageInfo.totalCount", ".", Integer.class, 0);
      }

      JSONArray products = JSONUtils.getValueRecursive(jsonResponse, "data.search.products.edges", JSONArray.class);

      if (products != null && !products.isEmpty()) {
         for (Object object : products) {

            JSONObject product = ((JSONObject) object).optJSONObject("node");
            String productUrl = CrawlerUtils.completeUrl(product.optString("slug"), "https",
               this.getHomePage().replace("https://", "".replace("http://", ""))) + "/p";
            String internalId = product.optString("id");
            String internalPid = product.optString("sku");
            String name = product.optString("name");
            String image = JSONUtils.getValueRecursive(product, "image.0.url", ".", String.class, null);
            Double price = JSONUtils.getValueRecursive(product, "offers.lowPrice", ".", Double.class, 0.0);
            boolean isAvailable = price != null && price != 0.0;
            Integer priceInCents = isAvailable ? MathUtils.parseInt(price * 100) : null;

            try {
               RankingProduct rankingProduct = RankingProductBuilder.create()
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setUrl(productUrl)
                  .setName(name)
                  .setImageUrl(image)
                  .setPriceInCents(priceInCents)
                  .setAvailability(isAvailable)
                  .build();

               saveDataProduct(rankingProduct);
            } catch (MalformedProductException e) {
               this.log(e.getMessage());
            }

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      }
   }

}
