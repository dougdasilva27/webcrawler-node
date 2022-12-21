package br.com.lett.crawlernode.crawlers.ranking.keywords.espana;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
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
import java.util.List;
import java.util.Map;

public class EspanaZoomaliaCrawler extends CrawlerRankingKeywords {

   public EspanaZoomaliaCrawler(Session session) {
      super(session);
   }

   private JSONObject fetchJsonPage() {
      String url = "https://okzyi6r8r8-1.algolianet.com/1/indexes/*/queries?x-algolia-agent=Algolia%20for%20JavaScript%20(4.11.0)%3B%20Browser%20(lite)&x-algolia-api-key=77c3a484dd1f9b486b3421652774f755&x-algolia-application-id=OKZYI6R8R8";

      String payload = "{\"requests\":[{\"indexName\":\"json_product_es\",\"params\":\"hitsPerPage=" + this.pageSize + "&clickAnalytics=true&query=" + this.keywordEncoded + "&maxValuesPerFacet=30&highlightPreTag=__ais-highlight__&highlightPostTag=__%2Fais-highlight__&page=" + (this.currentPage - 1) + "&userToken=anonymous-c462629b31d03246bee0938d23a304a5373b8ca7&facets=%5B%22root_category%22%2C%22category%22%2C%22brand%22%2C%22c_price_int%22%2C%22c_note%22%5D&tagFilters=&analytics=true\"}]}";

      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/x-www-form-urlencoded");
      headers.put("Accept-Encoding", "gzip, deflate");
      headers.put("accept", "*/*");
      headers.put("connection", "keep-alive");
      headers.put("host","okzyi6r8r8-1.algolianet.com");
      headers.put("origin","https://www.zoomalia.es");
      headers.put("referer","https://www.zoomalia.es");

      Request request = Request.RequestBuilder.create()
         .setCookies(cookies)
         .setHeaders(headers)
         .setPayload(payload)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_ES
         ))
         .setUrl(url)
         .setFollowRedirects(false)
         .mustSendContentEncoding(false)
         .setSendUserAgent(true)
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new FetcherDataFetcher(), new JsoupDataFetcher(), new ApacheDataFetcher()), session, "post");

      return CrawlerUtils.stringToJson(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {

      this.pageSize = 35;
      this.log("Página " + this.currentPage);

      String url = "https://www.zoomalia.es/?q=" + this.keywordEncoded + "&p=" + this.currentPage + "&i=json_product_es";

      this.log("Link onde são feitos os crawlers: " + url);
      JSONObject pageJson = fetchJsonPage();

      if (this.currentPage == 1) {
         this.totalProducts = JSONUtils.getValueRecursive(pageJson, "results.0.nbHits", ".", Integer.class, 0);
         this.log("Total products: " + this.totalProducts);
      }

      JSONArray products = JSONUtils.getValueRecursive(pageJson, "results.0.hits", ".", JSONArray.class, new JSONArray());

      if (!products.isEmpty()) {
         for (Object arrayItem : products) {
            JSONObject product = (JSONObject) arrayItem;
            String internalId = product.optString("objectID");
            String productUrl = product.optString("link");
            String name = product.optString("title");
            Integer priceInCents = JSONUtils.getPriceInCents(product, "price", Integer.class, null);
            boolean isAvailable = priceInCents != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setPriceInCents(priceInCents)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);

            if (this.arrayProducts.size() == productsLimit)
               break;
         }

      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }
}
