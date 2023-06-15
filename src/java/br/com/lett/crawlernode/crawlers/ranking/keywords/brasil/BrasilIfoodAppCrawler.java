package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


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
      try {
         HttpClient client = HttpClient.newBuilder().build();
         HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(url))
            .header("user-agent", "okhttp/4.10.0")
            .header("app_package_name", "br.com.brainweb.ifood")
            .header("authority", "marketplace.ifood.com.br")
            .header("sec-fetch-dest", "empty")
            .header("sec-fetch-mode", "cors")
            .header("item_experiment_details", "{\"default_internal\":{\"recommendation_filter\":\"internal_search\",\"model_id\":\"ifood-ml-discovery-default-sort-items\",\"engine\":\"sagemaker\",\"backend_experiment_id\":\"rank_exact\",\"force_recommendation_disabled\":true},\"market_internal\":{\"recommendation_filter\":\"internal_search\",\"model_id\":\"search-bumblebee-endpoint\",\"engine\":\"sagemaker\",\"query_rewriter_model_id\":\"search-r5d4-serve-endpoint\",\"backend_experiment_id\":\"v5\",\"query_rewriter_rule\":\"groceries-context\",\"force_recommendation_disabled\":false,\"similar_search\":{\"backend_experiment_id\":\"v5\",\"query_rewriter_model_id\":\"search-marvin-curated-endpoint\",\"force_recommendation_disabled\":false, \"model_id\": \"search-marvin-filter-endpoint\"}}}")
            .header("item_experiment_enabled", "true")
            .header("item_experiment_variant", "market_internal")
            .build();

         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         return new JSONObject(response.body());
      } catch (IOException | InterruptedException e) {
         throw new RuntimeException("Failed to load document: " + url, e);
      }
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
               .setUrl(session.getOptions().optString("preLink")+id)
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
