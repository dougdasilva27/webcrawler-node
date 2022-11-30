package br.com.lett.crawlernode.crawlers.ranking.keywords.costarica;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
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
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CostaricaAutomercadoCrawler extends CrawlerRankingKeywords {

   public CostaricaAutomercadoCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   private final String storeId = session.getOptions().getString("store_id");

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 30;

      JSONObject apiJson = fetchProducts();

      if (apiJson != null && !apiJson.isEmpty()) {
         JSONArray products = JSONUtils.getValueRecursive(apiJson, "results.0.hits", JSONArray.class, new JSONArray());
         this.totalProducts = JSONUtils.getValueRecursive(apiJson, "results.0.nbHits", Integer.class, 0);
         for (Object o : products) {
            JSONObject product = (JSONObject) o;
            String internalId = product.optString("productID");
            String internalPid = product.optString("productNumber");
            String name = product.optString("ecomDescription");
            String url = getUrl(internalId, name);
            String imgUrl = product.optString("imageUrl");
            Boolean isSponsored = product.optBoolean("sponsoredProduct", false);
            boolean isAvailable = JSONUtils.getValueRecursive(product, "storeDetail." + storeId + ".productAvailable", Boolean.class, false);
            Integer price = isAvailable ? getPrice(product) : null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(url)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setImageUrl(imgUrl)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setIsSponsored(isSponsored)
               .build();

            saveDataProduct(productRanking);
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      }
   }

   private Integer getPrice(JSONObject product) {
      Double price = JSONUtils.getValueRecursive(product, "storeDetail." + storeId + ".amount", ".", Double.class, null);
      if (price != null) {
         return CommonMethods.doublePriceToIntegerPrice(price, 0);
      }
      return null;
   }

   private String getUrl(String internalId, String name) {
      String url = null;
      String nameEncoded;
      if (name != null && !name.isEmpty()) {
         nameEncoded = CommonMethods.toSlug(name);
      } else {
         nameEncoded = "%20";
      }
      if (internalId != null) {
         url = "https://www.automercado.cr/p/" + nameEncoded + "/id/" + internalId;
      }
      return url;
   }


   private JSONObject fetchProducts() {
      String apiUrl = "https://fu5xfx7knl-2.algolianet.com/1/indexes/*/queries?x-algolia-api-key=113941a18a90ae0f17d602acd16f91b2&x-algolia-application-id=FU5XFX7KNL";
      int page = this.currentPage - 1;
      String payload = "{\"requests\":[{\"indexName\":\"Product_CatalogueV2\",\"params\":\"query=" + this.keywordEncoded + "&optionalWords=%5B%22" + this.keywordEncoded + "%22%5D&filters=NOT%20marca%3AMASTERCHEF%20AND%20NOT%20marca%3APANINI&page=" + page + "&getRankingInfo=true&facets=%5B%22marca%22%2C%22addedSugarFree%22%2C%22fiberSource%22%2C%22lactoseFree%22%2C%22lfGlutemFree%22%2C%22lfOrganic%22%2C%22lfVegan%22%2C%22lowFat%22%2C%22lowSodium%22%2C%22preservativeFree%22%2C%22sweetenersFree%22%2C%22parentProductid%22%2C%22parentProductid2%22%2C%22parentProductid_URL%22%2C%22catecom%22%5D&facetFilters=%5B%5B%22storeDetail." + storeId + ".storeid%3A" + storeId + "%22%5D%5D\"}]}";
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
      headers.put("origin", "https://automercado.cr");
      headers.put("authorization", "https://automercado.cr");
      headers.put("connect", "keep-alive");
      Request request = Request.RequestBuilder.create()
         .setUrl(apiUrl)
         .setHeaders(headers)
         .setPayload(payload)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY
         ))
         .setFollowRedirects(false)
         .setFetcheroptions(FetcherOptions.FetcherOptionsBuilder.create().mustUseMovingAverage(true).build())
         .mustSendContentEncoding(false)
         .setSendUserAgent(true)
         .build();
      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new FetcherDataFetcher(), new JsoupDataFetcher(), new ApacheDataFetcher()), session, "post");

      return CrawlerUtils.stringToJson(response.getBody());
   }
}
