package br.com.lett.crawlernode.crawlers.ranking.keywords.chapeco;

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
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChapecoSuperroyalCrawler extends CrawlerRankingKeywords {
   public ChapecoSuperroyalCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 20;
      log("Página " + this.currentPage);

      String url = "https://superroyal.com.br/busca/" + this.keywordEncoded;
      log("Link onde são feitos os crawlers: " + url);

      JSONObject productsJSON = fetchJson();
      JSONArray edges = productsJSON.optJSONArray("edges");
      if (edges != null && !edges.isEmpty()) {

         if (this.totalProducts == 0) {
            setTotalProducts(productsJSON);
         }

         for (Object o : edges) {
            if (o instanceof JSONObject) {
               JSONObject node = (JSONObject) o;
               JSONObject productJson = node.optJSONObject("node");
               if (productJson != null) {
                  String internalId = productJson.optString("objectID");
                  String productUrl = "https://www.superroyal.com.br/produtos/" + internalId + "/" + productJson.optString("slug");
                  String name = productJson.optString("name");
                  String image = productJson.optString("originalImage");
                  Integer priceInCents = CommonMethods.doublePriceToIntegerPrice(JSONUtils.getValueRecursive(productJson, "pricing.0.price", Double.class), null);
                  boolean available = priceInCents != 0;

                  RankingProduct productRanking = RankingProductBuilder.create()
                     .setUrl(productUrl)
                     .setInternalId(internalId)
                     .setImageUrl(image)
                     .setName(name)
                     .setPriceInCents(priceInCents)
                     .setAvailability(available)
                     .build();

                  saveDataProduct(productRanking);

                  if (arrayProducts.size() == productsLimit) {
                     break;
                  }
               }
            }
         }
      } else {
         result = false;
         log("Keyword sem resultado!");
      }
      log("Finalizando Crawler de produtos da página " + this.currentPage + " até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   protected void setTotalProducts(JSONObject json) {
      this.totalProducts = json.optInt("count", 0);
      this.log("Total de produtos: " + this.totalProducts);
   }

   private JSONObject fetchJson() {

      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
      headers.put("origin", "https://www.superroyal.com.br");
      headers.put("referer", "https://www.superroyal.com.br/");
      headers.put("authority", "search.osuper.com.br");

      JSONObject payload = new JSONObject();
      payload.put("accountId", 8);
      payload.put("storeId", 18);
      payload.put("search", keywordEncoded);
      payload.put("first", productsLimit);
      payload.put("sort", "{\"field\":\"_score\",\"order\":\"desc\"},\"pricingRange\":{}");


      String urlApi = "https://search.osuper.com.br/ecommerce_products_production/_search";
      Request request = Request.RequestBuilder.create()
         .setUrl(urlApi)
         .setProxyservice(List.of(ProxyCollection.NETNUT_RESIDENTIAL_BR, ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY))
         .setHeaders(headers)
         .setPayload(payload.toString())
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new JsoupDataFetcher(), new FetcherDataFetcher(), new ApacheDataFetcher()), session, "post");

      return JSONUtils.stringToJson(response.getBody());
   }
}
