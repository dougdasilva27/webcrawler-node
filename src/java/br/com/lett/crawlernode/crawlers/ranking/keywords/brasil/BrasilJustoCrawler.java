package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
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
import org.apache.http.HttpHeaders;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrasilJustoCrawler extends CrawlerRankingKeywords {
   private final String HOME_PAGE = "https://soujusto.com.br";
   private final String POSTAL_CODE = getPostalCode();

   private final String idStore = session.getOptions().optString("store_id", "10");

   public BrasilJustoCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.HTTPCLIENT;
   }

   private String getPostalCode() {
      return session.getOptions().getString("postal_code");
   }

   @Override
   protected void processBeforeFetch() {
      BasicClientCookie cookie = new BasicClientCookie("postal_code", POSTAL_CODE);
      cookie.setDomain("soujusto.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 15;

      JSONObject json = fetchJSON();
      if (this.currentPage == 1) {
         this.totalProducts = JSONUtils.getValueRecursive(json, "results.0.nbHits", Integer.class, 0);
      }

      JSONArray products = JSONUtils.getValueRecursive(json, "results.0.hits", JSONArray.class, new JSONArray());
      for (int i = 0; i < products.length(); i++) {
         JSONObject product = products.getJSONObject(i);
         String internalId = JSONUtils.getValueRecursive(product, "sku", String.class, "");
         String productUrl = HOME_PAGE + JSONUtils.getValueRecursive(product, "url", String.class, "");
         String name = product.optString("name");
         String imageUrl = JSONUtils.getValueRecursive(product, "image_thumbnail_url", String.class, "");
         Double price = JSONUtils.getValueRecursive(product, "stores.10.filter_price", Double.class, 0d);
         boolean isAvailable = JSONUtils.getValueRecursive(product, "stores." + idStore + ".available", Boolean.class);
         Integer priceInCents = isAvailable ? (int) (price * 100) : null;

         RankingProduct productRanking = RankingProductBuilder.create()
            .setUrl(productUrl)
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setImageUrl(imageUrl)
            .setPriceInCents(priceInCents)
            .setAvailability(isAvailable)
            .build();

         saveDataProduct(productRanking);

         if (this.arrayProducts.size() == productsLimit) {
            break;
         }
      }
   }

   private JSONObject fetchJSON() {
      int offset = this.currentPage - 1;

      String payload = "{\"requests\":[{\"indexName\":\"arcade_storefront\",\"params\":\"analyticsTags=%5B%22desktop%22%2C%22anonymous%22%2C%22search%22%2C%22short%20query%22%5D&clickAnalytics=true&facets=%5B%22brand%22%2C%22categories.lvl0%22%2C%22first_publication_timestamp%22%2C%22same_day_shipping_available%22%2C%22stores."
         + this.idStore + ".filter_price%22%2C%22stores." + this.idStore + ".has_discount%22%5D&filters=is_published%3A%20true%20AND%20stores."
         + this.idStore + ".active%3A%20true&highlightPostTag=%2Fais-highlight&highlightPreTag=ais-highlight&hitsPerPage=" + this.pageSize + "&maxValuesPerFacet=30&page="
         + offset + "&query=" + this.keywordEncoded.replace("+", "%20") + "&ruleContexts=%5B%22user_store%3A" + this.idStore + "%22%5D&tagFilters=&userToken=2c2f950f-2e53-47f5-9846-82a8168e0a24\"}]}";

      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
      headers.put("authority", "soujusto.com.br");
      headers.put("origin", "https://soujusto.com.br");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://cnn0corslw-dsn.algolia.net/1/indexes/*/queries?x-algolia-agent=Algolia%20for%20JavaScript%20(4.13.0)%3B%20Browser%20(lite)%3B%20instantsearch.js%20(4.54.0)%3B%20react%20(17.0.2)%3B%20react-instantsearch%20(6.38.1)%3B%20react-instantsearch-hooks%20(6.38.1)%3B%20JS%20Helper%20(3.14.0)&x-algolia-api-key=d544ee233b452e57766306b328901df5&x-algolia-application-id=CNN0CORSLW")
         .setHeaders(headers)
         .setCookies(this.cookies)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY))
         .mustSendContentEncoding(false)
         .setPayload(payload)
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new FetcherDataFetcher(), new ApacheDataFetcher(), new JsoupDataFetcher()), session, "post");
      return CrawlerUtils.stringToJson(response.getBody());
   }
}
