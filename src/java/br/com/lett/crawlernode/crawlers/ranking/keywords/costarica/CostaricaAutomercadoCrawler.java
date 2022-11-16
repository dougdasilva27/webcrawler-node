package br.com.lett.crawlernode.crawlers.ranking.keywords.costarica;

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
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.*;

public class CostaricaAutomercadoCrawler extends CrawlerRankingKeywords {

   public CostaricaAutomercadoCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 26;

      JSONObject apiJson = fetchProducts();

      if (apiJson != null && !apiJson.isEmpty()) {
         JSONObject result = JSONUtils.getValueRecursive(apiJson, "data.products", JSONObject.class);
         if (this.totalProducts == 0) {
            this.totalProducts = result.optInt("count", 0);
         }
         JSONArray products = result.optJSONArray("content");
         for (Object o : products) {
            JSONObject product = (JSONObject) o;

            String internalId = product.optString("uuid");
            String internalPid = product.optString("productNumber");
            String name = product.optString("name");
            String url = getUrl(internalId, name);
            String imgUrl = product.optString("icon");
            boolean isAvailable = product.optBoolean("productAvailable");
            Integer price = isAvailable ? product.optInt("rawPrice", 0) * 100 : null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(url)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setImageUrl(imgUrl)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);
         }

      } else {
         log("keyword sem resultado");
      }

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
      String payload = "{\"search\":\"" + this.keywordWithoutAccents + "\",\"page\":0,\"facetFilters\":{\"brand\":[],\"lifeStyle\":[],\"department\":[],\"category\":null,\"subCategory\":[]}}";

      String authHeader = session.getOptions().optString("authHeader");
      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");
      headers.put("Authorization", authHeader);

      Request request = Request.RequestBuilder.create()
         .setUrl("https://automercado.azure-api.net/prod-front/product/searchByAttributes")
         .setPayload(payload)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY
         ))
         .mustSendContentEncoding(false)
         .build();
      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new FetcherDataFetcher(), new ApacheDataFetcher(), new JsoupDataFetcher()), session, "post");

      return CrawlerUtils.stringToJson(response.getBody());
   }
}
