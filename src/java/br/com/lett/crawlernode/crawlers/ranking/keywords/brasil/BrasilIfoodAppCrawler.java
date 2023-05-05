package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;


import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
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

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class BrasilIfoodAppCrawler extends CrawlerRankingKeywords {

   public BrasilIfoodAppCrawler(Session session) {
      super(session);
   }

   private static final String baseImageUrl = "https://static.ifood-static.com.br/image/upload/t_medium/pratos/";
   private final String latitude = session.getOptions().optString("latitude", "");
   private final String longitude = session.getOptions().optString("longitude", "");
   private final String zip_code = session.getOptions().optString("zip_code", "");

   private final String merchant_id = session.getOptions().optString("merchant_id", "");

   @Override
   protected JSONObject fetchJSONObject(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("host", "wsloja.ifood.com.br");
      headers.put("channel", "IFOOD");
      headers.put("user-agent", "okhttp/4.10.0");
      headers.put("app_package_name", "br.com.brainweb.ifood");
      headers.put("authority", "marketplace.ifood.com.br");
      headers.put("sec-fetch-dest", "empty");
      headers.put("sec-fetch-mode", "cors");
      headers.put("item_experiment_details", "{\"default_internal\":{\"recommendation_filter\":\"internal_search\",\"model_id\":\"ifood-ml-discovery-default-sort-items\",\"engine\":\"sagemaker\",\"backend_experiment_id\":\"rank_exact\",\"force_recommendation_disabled\":true},\"market_internal\":{\"recommendation_filter\":\"internal_search\",\"model_id\":\"search-bumblebee-endpoint\",\"engine\":\"sagemaker\",\"query_rewriter_model_id\":\"search-r5d4-serve-endpoint\",\"backend_experiment_id\":\"v5\",\"query_rewriter_rule\":\"groceries-context\",\"force_recommendation_disabled\":false,\"similar_search\":{\"backend_experiment_id\":\"v5\",\"query_rewriter_model_id\":\"search-marvin-curated-endpoint\",\"force_recommendation_disabled\":false, \"model_id\": \"search-marvin-filter-endpoint\"}}}");
      headers.put("item_experiment_enabled", "true");
      headers.put("item_experiment_variant", "market_internal");
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_ANY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.SMART_PROXY_BR,
            ProxyCollection.SMART_PROXY_BR_HAPROXY
         ))
         .setFollowRedirects(false)
         .build();
      Response response = CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher(), true);
      return CrawlerUtils.stringToJson(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String url = "https://marketplace.ifood.com.br/v2/search/catalog-items?latitude=" + latitude + "&longitude=" + longitude + "&zip_code=" + zip_code + "&channel=IFOOD&term=" + this.keywordEncoded + "&categories=&item_from_merchant_ids=" + merchant_id + "&size=100&page=0";
      JSONObject jsonObject = fetchJSONObject(url);

      if (jsonObject != null && jsonObject.has("id")) {
         if (this.totalProducts == 0) {
            this.totalProducts = JSONUtils.getValueRecursive(jsonObject, "items.total", Integer.class, 0);
         }
         JSONArray items = JSONUtils.getValueRecursive(jsonObject, "items.data", JSONArray.class, new JSONArray());
         for (Object o : items) {
            JSONObject item = (JSONObject) o;
            String id = item.optString("id");
            String name = item.optString("name");
            String image = scrapImage(item);
            double priceDouble = item.optDouble("price", 0.0);
            boolean availability = priceDouble != 0.0;
            Integer price = availability ? (int) (priceDouble * 100) : null;

            RankingProduct objProducts = RankingProductBuilder.create()
               .setUrl(id)
               .setInternalId(id)
               .setAvailability(availability)
               .setImageUrl(image)
               .setName(name)
               .setPriceInCents(price)
               .build();

            saveDataProduct(objProducts);

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      }
   }

   private String scrapImage(JSONObject jsonObject) {

      String suffixImage = JSONUtils.getValueRecursive(jsonObject, "resources.0.fileName", String.class, "");
      return !suffixImage.isEmpty() ? baseImageUrl + suffixImage : null;
   }
}
