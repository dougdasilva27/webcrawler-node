package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

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
import java.text.Normalizer;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilDakiCrawler extends CrawlerRankingKeywords {

   public BrasilDakiCrawler(Session session) {
      super(session);
   }

   private final String hubId = session.getOptions().optString("hub_id");

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.log("Extracting products from page " + this.currentPage);

      String payloadRanking = "{\"operationName\":\"searchProducts\",\"variables\":{\"searchTerm\":\"" + this.keywordEncoded + "\",\"hubId\":\"" + hubId + "\",\"priceHubId2\":\"" + hubId + "\"},\"query\":\"query searchProducts($searchTerm: String!, $hubId: String!, $priceHubId2: String!) {\\n  searchProducts(searchTerm: $searchTerm, hubId: $hubId) {\\n    products {\\n      sku\\n      cmsProduct {\\n        category {\\n          categoryName\\n          __typename\\n        }\\n        name\\n        title\\n        title2\\n        packshot1_front_grid {\\n          url\\n          __typename\\n        }\\n        ui_content_1\\n        subCategory {\\n          cmsSubCategory {\\n            title\\n            __typename\\n          }\\n          __typename\\n        }\\n        price(hubId: $priceHubId2) {\\n          amount\\n          compareAtPrice\\n          discount\\n          id\\n          sku\\n          __typename\\n        }\\n        product_status\\n        inventory(hubId: $hubId) {\\n          quantity\\n          status\\n          showOutOfStock\\n          __typename\\n        }\\n        __typename\\n      }\\n      __typename\\n    }\\n    __typename\\n  }\\n}\"}";
      JSONObject jsonRanking = fetchJsonAPi(payloadRanking);

      if (!jsonRanking.isEmpty()) {
         JSONArray productsArray = JSONUtils.getValueRecursive(jsonRanking, "data.searchProducts.products", JSONArray.class);
         this.log("Total da busca: " + productsArray.length());

         for (Object productObj : productsArray) {
            JSONObject product = (JSONObject) productObj;
            String internalPid = product.optString("sku");
            String name = JSONUtils.getValueRecursive(product, "cmsProduct.name", String.class);

            String title = JSONUtils.getValueRecursive(product, "cmsProduct.title", String.class);
            String content = JSONUtils.getValueRecursive(product, "cmsProduct.ui_content_1", String.class);
            String productUrl = generateProductUrl(name, internalPid);

            String imageUrl = JSONUtils.getValueRecursive(product, "cmsProduct.packshot1_front_grid.url", String.class);
            Double price = JSONUtils.getValueRecursive(product, "cmsProduct.price.amount", Double.class, 0d);
            boolean isAvailable = JSONUtils.getValueRecursive(product, "cmsProduct.inventory.quantity", Integer.class) > 0;
            Integer priceInCents = isAvailable ? (int) (price * 100) : null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalPid)
               .setInternalPid(internalPid)
               .setName(name)
               .setImageUrl(imageUrl)
               .setPriceInCents(priceInCents)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);
            if (this.arrayProducts.size() == productsLimit) break;
         }

      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
   }

   private JSONObject fetchJsonAPi(String payload) {
      HashMap<String, String> headers = new HashMap<>();

      headers.put("Content-Type", "application/json");
      headers.put("Content-Length", "<calculated when request is sent>");
      headers.put("Host", "<calculated when request is sent>");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://api-prd-br.jokrtech.com/")
         .setPayload(payload)
         .setHeaders(headers)
         .build();

      Response response = CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher());
      return JSONUtils.stringToJson(response.getBody());
   }

   private String generateProductUrl(String name, String internalPid) {
      String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
         .replaceAll("[^\\p{ASCII}]", "");

      String formattedName = normalized.toLowerCase()
         .replaceAll("[^a-z0-9\\s-]", "")
         .replaceAll("\\s+", " ")
         .trim()
         .replaceAll(" ", "-");

      Pattern pattern = Pattern.compile("(\\d+)([a-zA-Z]+)");
      Matcher matcher = pattern.matcher(formattedName);
      StringBuffer result = new StringBuffer();

      while (matcher.find()) {
         matcher.appendReplacement(result, matcher.group(1) + "-" + matcher.group(2));
      }
      matcher.appendTail(result);

      String productURL = String.format("https://soudaki.com/shop/p/%s__%s/", result.toString(), internalPid);

      return productURL;
   }
}
