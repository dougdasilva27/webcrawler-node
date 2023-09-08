package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

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
import br.com.lett.crawlernode.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MexicoLacomerCrawler extends CrawlerRankingKeywords {

   public MexicoLacomerCrawler(Session session) {
      super(session);
   }

   private final String succId = session.getOptions().optString("succId");

   private JSONObject fetchSearchJson(String url) {
      String clientKey = "client-key 061cbdb5-fbcd-4a50-a70d-945d85a7de2f";

      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "application/json, text/plain, */*");
      headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36");
      headers.put("content-type", "application/json");
      headers.put("authorization", clientKey);
      headers.put("X-Groupby-Customer-ID", "lacomer");
      headers.put("origin", "https://www.lacomer.com.mx");
      headers.put("referer", "https://www.lacomer.com.mx/");

      String payload = "{\"query\": \"" + this.keywordEncoded + "\",\"collection\": \"Production\",\"area\": \"Production\",\"pageSize\": 20,\"skip\": " + (this.currentPage - 1) * this.pageSize + ",\"dynamicFacet\": false,\"sorts\": null,\"preFilter\": \"attributes.sponsored:IN(*,0.5e) AND attributes.storeIds:ANY(\\\"" + succId + "\\\")\",\"refinements\": [],\"variantRollupKeys\": [\"inventory(" + succId + ",price)\",\"inventory(" + succId + ",originalPrice)\",\"inventory(" + succId + ",attributes.promotionPrice)\",\"inventory(" + succId + ",attributes.promotion_type)\",\"inventory(" + succId + ",attributes.promotionJSON)\",\"inventory(" + succId + ",attributes.promotion_description)\"]}";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .setProxyservice(List.of(
            ProxyCollection.BUY_HAPROXY
         ))
         .build();

      String body = new JsoupDataFetcher().post(session, request).getBody();

      return CrawlerUtils.stringToJSONObject(body);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 20;

      this.log("Página " + this.currentPage);

      String url = "https://search.lacomer.groupbycloud.com/api/search";
      this.log("Link onde são feitos os crawlers: " + url);

      JSONObject search = fetchSearchJson(url);

      if (search.has("records") && search.getJSONArray("records").length() > 0) {
         JSONArray products = search.getJSONArray("records");

         if (this.totalProducts == 0) {
            setTotalBusca(search);
         }

         for (int i = 0; i < products.length(); i++) {

            JSONObject product = products.getJSONObject(i);
            String internalPid = JSONUtils.getValueRecursive(product, "allMeta.id", ".", String.class, null);
            String productUrl = crawlProductUrl(product);
            String name = JSONUtils.getValueRecursive(product, "allMeta.title", ".", String.class, "") + " " + JSONUtils.getValueRecursive(product, "allMeta.description", ".", String.class, "");
            String imgUrl = JSONUtils.getValueRecursive(product, "allMeta.images.0.uri", ".", String.class, null);
            Integer price = getPrice(product);
            boolean  isAvailable  = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalPid(internalPid)
               .setName(name)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
            + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return this.arrayProducts.size() < this.totalProducts;
   }

   protected Integer getPrice(JSONObject product){
      JSONArray variantRollUpValues = JSONUtils.getValueRecursive(product, "allMeta.variantRollUpValues", ".", JSONArray.class, new JSONArray());
      JSONArray prices = JSONUtils.filterItemsByKeyValue(variantRollUpValues, new Pair<>("key", "inventory(" + succId + ",price)"));

      if (!prices.isEmpty()) {
         Double price = JSONUtils.getValueRecursive(prices, "0.value.0", ".", Double.class, null);
         return price != null ? MathUtils.parseInt(price * 100) : null;
      }

      return null;
   }

   protected void setTotalBusca(JSONObject search) {
      if (search.has("totalRecordCount")) {

         this.totalProducts = search.getInt("totalRecordCount");


         this.log("Total da busca: " + this.totalProducts);
      }
   }

   private String crawlProductUrl(JSONObject product) {
      String ean = JSONUtils.getValueRecursive(product, "allMeta.id", ".", String.class, null);
      Double aisle = JSONUtils.getValueRecursive(product, "allMeta.attributes.aisle.numbers.0", ".", Double.class, null);
      int aisleInt = aisle != null ? aisle.intValue() : 0;

      if (aisleInt == 0) {
         return null;
      }

      return "https://www.lacomer.com.mx/lacomer/#!/detarticulo/" + ean + "/0/" + aisleInt + "/1///" + aisleInt + "?succId=" + succId + "&succFmt=100";
   }
}
