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
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MexicoDespensabodegaaurreraCrawler extends CrawlerRankingKeywords {

   private final static String API_URL = "https://deadpool.instaleap.io/api/v2";
   private final static String STORE_ID = "565";

   public MexicoDespensabodegaaurreraCrawler(Session session) {
      super(session);
   }

   protected JSONArray fetchJSON() {
      Map<String, String> headers = new HashMap<>();

      headers.put("authority", "deadpool.instaleap.io");
      headers.put("content-type", "application/json");
      headers.put("referer", "https://despensa.bodegaaurrera.com.mx/search?name=" + this.keywordEncoded.replace("-", "%20"));

      String body = "[{\"operationName\":\"GetFiltersList\",\"variables\":{\"search\":{\"text\":\"" + this.keywordEncoded.replace("+"," ") + "\",\"language\":\"ES\"},\"storeId\":\"565\"},\"query\":\"query GetFiltersList($search: SearchInput!, $storeId: ID!) {\\n  getFiltersList(search: $search, storeId: $storeId) {\\n    filtersList {\\n      brand\\n      categoryid\\n      categoryname\\n      __typename\\n    }\\n    __typename\\n  }\\n}\\n\"},{\"operationName\":\"GetProducts\",\"variables\":{\"pagination\":{\"pageSize\":100,\"currentPage\":1},\"search\":{\"text\":\"" + this.keywordEncoded.replace("+"," ") +"\",\"language\":\"ES\"},\"storeId\":\"565\",\"orderBy\":{},\"variants\":false,\"filter\":{},\"showProductsWithoutStock\":false},\"query\":\"fragment BaseProduct on Product {\\n  id\\n  description\\n  name\\n  photosUrls\\n  sku\\n  unit\\n  price\\n  specialPrice\\n  promotion {\\n    description\\n    type\\n    isActive\\n    conditions\\n    __typename\\n  }\\n  variants {\\n    selectors\\n    productModifications\\n    __typename\\n  }\\n  isAvailable\\n  stock\\n  nutritionalDetails\\n  clickMultiplier\\n  subQty\\n  subUnit\\n  maxQty\\n  minQty\\n  specialMaxQty\\n  ean\\n  boost\\n  showSubUnit\\n  isActive\\n  slug\\n  categoriesPath\\n  categories {\\n    id\\n    name\\n    __typename\\n  }\\n  formats {\\n    format\\n    equivalence\\n    unitEquivalence\\n    minQty\\n    maxQty\\n    __typename\\n  }\\n  __typename\\n}\\n\\nquery GetProducts($pagination: paginationInput, $search: SearchInput, $storeId: ID!, $categoryId: ID, $onlyThisCategory: Boolean, $filter: ProductsFilterInput, $orderBy: productsSortInput, $variants: Boolean, $showProductsWithoutStock: Boolean) {\\n  getProducts(pagination: $pagination, search: $search, storeId: $storeId, categoryId: $categoryId, onlyThisCategory: $onlyThisCategory, filter: $filter, orderBy: $orderBy, variants: $variants, showProductsWithoutStock: $showProductsWithoutStock) {\\n    redirectTo\\n    products {\\n      ...BaseProduct\\n      __typename\\n    }\\n    paginator {\\n      pages\\n      page\\n      __typename\\n    }\\n    __typename\\n  }\\n}\\n\"}]";

      Request request = Request.RequestBuilder.create()
         .setUrl(API_URL)
         .setPayload(body)
         .setProxyservice(
            Arrays.asList(
               ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_MX,
               ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY
            )
         )
         .setHeaders(headers)
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new JsoupDataFetcher(), new FetcherDataFetcher(), new ApacheDataFetcher()), session, "post");
      return JSONUtils.stringToJsonArray(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      JSONArray json = fetchJSON();

      JSONArray results = JSONUtils.getValueRecursive(json, "1.data.getProducts.products", JSONArray.class);

      if (results != null && !results.isEmpty()) {
         for (Object prod : results) {
            JSONObject product = (JSONObject) prod;

            String internalPid = product.optString("sku");
            String internalId = product.optString("id");
            String productUrl = "https://despensa.bodegaaurrera.com.mx/p/" + product.optString("slug");
            String name = product.optString("name");
            int price = product.optInt("price", 0) == 0 ? product.optInt("specialPrice", 0) : product.optInt("price", 0);
            boolean isAvailable = product.getInt("stock") != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
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

   @Override
   protected boolean hasNextPage() {
      return true;
   }

}
