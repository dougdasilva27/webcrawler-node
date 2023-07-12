package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
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


public class BrasilIfood extends CrawlerRankingKeywords {

   public BrasilIfood(Session session) {
      super(session);
   }

   protected String storeId = session.getOptions().getString("store_id");
   protected String geolocation = session.getOptions().getString("geolocation");

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      String url = "https://marketplace.ifood.com.br/v2/search/catalog-items?" + geolocation + "&channel=IFOOD&term=" +
         this.keywordEncoded + "&size=36&page=" + (this.currentPage - 1) + "&item_from_merchant_ids=" + storeId;
      JSONObject apiJson = fetch(url);

      JSONArray data = JSONUtils.getValueRecursive(apiJson, "items.data", JSONArray.class);
      if (data != null && !data.isEmpty()) {

         for (Object obj : data) {
            JSONObject product = (JSONObject) obj;

            if (!product.isEmpty()) {
               String internalId = product.optString("code");
               String internalPid = internalId;
               String productUrl = getUrl(product, internalId);
               String imgUrl = crawlImage(product);
               String name = product.optString("name");
               int price = JSONUtils.getPriceInCents(product, "price");
               boolean isAvailable = JSONUtils.getValueRecursive(product, "merchant.available", Boolean.class);

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setImageUrl(imgUrl)
                  .setName(name)
                  .setPriceInCents(price)
                  .setAvailability(isAvailable)
                  .build();

               saveDataProduct(productRanking);
               if (this.arrayProducts.size() == productsLimit)
                  break;
            }

            this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
   }

   private String crawlImage(JSONObject product) {
      String path = JSONUtils.getValueRecursive(product, "resources.0.fileName", String.class, "");
      return "https://static-images.ifood.com.br/image/upload/t_high/pratos/" + path;
   }

   private String getUrl(JSONObject itensObject, String internalId) {
      String slug = JSONUtils.getValueRecursive(itensObject, "merchant.slug", String.class);
      return CrawlerUtils.completeUrl(slug + "/" + storeId + "?item=" + internalId, "https", "www.ifood.com.br/delivery");
   }

   protected JSONObject fetch(String url) {
      JSONObject json = new JSONObject();
      JSONObject defaultInternalId = new JSONObject();
      defaultInternalId.put("recommendation_filter", "internal_search");
      defaultInternalId.put("model_id", "ifood-ml-discovery-default-sort-items");
      defaultInternalId.put("engine", "sagemaker");
      defaultInternalId.put("force_recommendation_disabled", true);
      JSONObject marketInternal = new JSONObject();
      marketInternal.put("recommendation_filter", "internal_search");
      marketInternal.put("model_id", "search-bumblebee-endpoint");
      marketInternal.put("engine", "sagemaker");
      marketInternal.put("query_rewriter_model_id", "ifood-ml-r5d4-v2");
      marketInternal.put("backend_experiment_id", "v5");
      marketInternal.put("query_rewriter_rule", "groceries-context");
      marketInternal.put("force_recommendation_disabled", false);
      JSONObject similarSearch = new JSONObject();
      similarSearch.put("backend_experiment_id", "v5");
      similarSearch.put("query_rewriter_model_id", "search-marvin-curated-endpoint");
      similarSearch.put("force_recommendation_disabled", true);
      marketInternal.put("similar_search", similarSearch);
      json.put("default_internal", defaultInternalId);
      json.put("market_internal", marketInternal);

      Map<String, String> headers = new HashMap<>();
      headers.put("item_experiment_details", json.toString());
      headers.put("item_experiment_variant", "market_internal");
      headers.put("accept", "application/json, text/plain, */*");
      headers.put("authority", "marketplace.ifood.com.br");

      Request request = RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.LUMINATI_SERVER_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_ROTATE_BR
         ))
         .build();

      String content = CrawlerUtils.retryRequest(request, session, new FetcherDataFetcher(), true).getBody();
      return JSONUtils.stringToJson(content);
   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }
}
