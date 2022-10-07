package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Product;
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

public class BrasilZoolojapetCrawler extends CrawlerRankingKeywords {
   public BrasilZoolojapetCrawler(Session session) {
      super(session);
   }

   Integer pageSize = 36;
   String homePage = "https://zoolojapet.com.br/";

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      JSONObject json = fetchJSONObject("https://m2.zoolojapet.com.br/graphql/V1/products");
      totalProducts = JSONUtils.getValueRecursive(json, "data.products.total_count", Integer.class);
      JSONArray products = JSONUtils.getValueRecursive(json, "data.products.items", JSONArray.class);
      if (products != null && !products.isEmpty()) {
         for (Object obj : products) {
            JSONObject product = (JSONObject) obj;
            String internalId = product.optString("id");
            String internalPid = product.optString("sku", null);
            String name = product.optString("name");
            Boolean available = checkAvailability(product.optString("stock_status", null));
            String productUrl = getUrl(product);
            String imageUrl = JSONUtils.getValueRecursive(product, "thumbnail.url", String.class);
            Integer price = getPrice(product);
            RankingProduct productRanking = RankingProductBuilder.create()
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setAvailability(available)
               .setUrl(productUrl)
               .setImageUrl(imageUrl)
               .setPriceInCents(price)
               .build();
            saveDataProduct(productRanking);
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      }
   }

   private String getUrl(JSONObject product) {
      String urlKey = product.optString("url_key");
      String urlSuffix = product.optString("url_suffix");
      if ((urlKey) != null && (urlSuffix) != null && !urlKey.isEmpty() && !urlSuffix.isEmpty()) {
         return homePage + urlKey + urlSuffix;
      }
      return null;
   }

   private String getImage(JSONObject product) {
      String imageUrl = JSONUtils.getValueRecursive(product, "thumbnail.url", String.class);
      if (imageUrl != null && !imageUrl.isEmpty()) {
         return imageUrl;
      }
      return null;
   }

   private Integer getPrice(JSONObject product) {
      Double priceDouble = JSONUtils.getValueRecursive(product, "price_range.minimum_price.final_price.value", Double.class);

      if (priceDouble != null) {
         return CommonMethods.doublePriceToIntegerPrice(priceDouble, null);
      }
      Integer priceInteger = JSONUtils.getValueRecursive(product, "price_range.minimum_price.final_price.value", Integer.class);
      if (priceInteger != null) {
         return priceInteger * 100;
      }
      return null;
   }

   private Boolean checkAvailability(String stock) {
      if (stock != null && !stock.isEmpty()) {
         return stock.equals("IN_STOCK");
      }
      return false;
   }

   @Override
   protected JSONObject fetchJSONObject(String url) {
      String payload = "{\"query\":\"\\n        query($page: Int!, $pageSize: Int!) {\\n          products(\\n            search: \\\"" + this.location + "\\\"\\n            currentPage: $page\\n            pageSize: $pageSize\\n            sort: { relevance: DESC }\\n            \\n          ) {\\n            \\n  items {\\n    id\\n    name\\n    sku\\n    stock_status\\n    url_key\\n    url_suffix\\n    recorrente\\n    price_range {\\n      minimum_price {\\n        regular_price { value }\\n        final_price { value }\\n        discount { amount_off, percent_off }\\n      }\\n    }\\n    thumbnail { url, label }\\n  }\\n  total_count\\n  page_info { current_page, total_pages }\\n\\n            aggregations {\\n              attribute_code\\n              count\\n              label\\n              options { count, label, value }\\n            }\\n          }\\n        }\\n      \",\"variables\":{\"page\":" + this.currentPage + ",\"pageSize\":" + this.pageSize + "}}";
      Map<String, String> headers = new HashMap<>();
      headers.put("origin", "https://zoolojapet.com.br/");
      headers.put("referer", "https://zoolojapet.com.br/");
      headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
      headers.put("authority", "m2.zoolojapet.com.br");
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .setProxyservice(Arrays.asList(ProxyCollection.BUY, ProxyCollection.NETNUT_RESIDENTIAL_BR))
         .build();
      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new FetcherDataFetcher(), new JsoupDataFetcher(), new ApacheDataFetcher()), session, "post");
      return JSONUtils.stringToJson(response.getBody());
   }


}
