package br.com.lett.crawlernode.crawlers.ranking.keywords.panama;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PanamaSuperxtraCrawler extends CrawlerRankingKeywords {

   protected int totalPages;

   public PanamaSuperxtraCrawler(Session session) {
      super(session);
      this.dataFetcher = new JsoupDataFetcher();
   }

   protected JSONObject fetchAPI() {
      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");
      headers.put("authority", "deadpool.instaleap.io");
      headers.put("accept", "*/*");
      headers.put("origin", "https://domicilio.superxtra.com");
      headers.put("accept-encoding", "gzip, deflate, br");
      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36");
      headers.put("referer", "https://domicilio.superxtra.com/search?name=" + this.keywordEncoded);

      String payload = "{\"operationName\":\"GetProducts\",\"variables\":{\"pagination\":{\"pageSize\":100,\"currentPage\":1},\"search\":{\"text\":\"desinfectante\",\"language\":\"ES\"},\"storeId\":\"70\",\"orderBy\":{},\"variants\":false,\"filter\":{\"brands\":null,\"categories\":null}},\"query\":\"query GetProducts($pagination: paginationInput, $search: SearchInput, $storeId: ID!, $categoryId: ID, $onlyThisCategory: Boolean, $filter: ProductsFilterInput, $orderBy: productsSortInput, $variants: Boolean) {\\n  getProducts(pagination: $pagination, search: $search, storeId: $storeId, categoryId: $categoryId, onlyThisCategory: $onlyThisCategory, filter: $filter, orderBy: $orderBy, variants: $variants) {\\n    redirectTo\\n    products {\\n      id\\n      description\\n      name\\n      photosUrls\\n      sku\\n      unit\\n      price\\n      specialPrice\\n      promotion {\\n        description\\n        type\\n        isActive\\n        conditions\\n        __typename\\n      }\\n      variants {\\n        selectors\\n        productModifications\\n        __typename\\n      }\\n      stock\\n      nutritionalDetails\\n      clickMultiplier\\n      subQty\\n      subUnit\\n      maxQty\\n      minQty\\n      specialMaxQty\\n      ean\\n      boost\\n      showSubUnit\\n      isActive\\n      slug\\n      categories {\\n        id\\n        name\\n        __typename\\n      }\\n      formats {\\n        format\\n        equivalence\\n        unitEquivalence\\n        __typename\\n      }\\n      __typename\\n    }\\n    paginator {\\n      pages\\n      page\\n      __typename\\n    }\\n    __typename\\n  }\\n}\\n\"}";
      String url = "https://deadpool.instaleap.io/api/v2";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setPayload(payload)
         .setHeaders(headers)
         .setProxyservice(
            Arrays.asList(
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY
            )
         )
         .setCookies(cookies)
         .build();

      Response response = new JsoupDataFetcher().post(session, request);
      return JSONUtils.stringToJson(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      pageSize = 100;
      JSONObject json = fetchAPI();

      JSONArray results = JSONUtils.getValueRecursive(json, "data.getProducts.products", JSONArray.class);

      if (results != null && !results.isEmpty()) {
         if (currentPage == 1) {
            totalPages = JSONUtils.getValueRecursive(json, "data.getProducts.paginator.pages", Integer.class);
         }

         for (Object prod : results) {
            JSONObject product = (JSONObject) prod;

            String internalPid = product.optString("sku");
            String internalId = product.optString("id");
            String productUrl = "https://domicilio.superxtra.com/p/" + product.optString("slug");
            String name = product.optString("name");
            String imgUrl = crawlImage(product);
            Integer price = crawlPrice(product);
            boolean isAvailable = product.optInt("stock", 0) > 0;

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
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private String crawlImage(JSONObject product) {
      JSONArray images = product.optJSONArray("photosUrls");
      if (images != null && !images.isEmpty()) {
         return images.get(0).toString();
      }
      return null;
   }

   private Integer crawlPrice(JSONObject product) {
      int price = 0;
      Object priceObject = product.opt("price");
      if (priceObject != null) {
         Double priceDouble = CommonMethods.objectToDouble(priceObject);
         if (priceDouble != null) {
            price = CommonMethods.doublePriceToIntegerPrice(priceDouble, 0);
         }
      }
      return price;
   }

   @Override
   protected boolean hasNextPage() {
      return currentPage <= totalPages;
   }
}
