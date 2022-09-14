package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class BrasilJustoCrawler extends CrawlerRankingKeywords {
   private final String HOME_PAGE = "https://soujusto.com.br";
   private final String POSTAL_CODE = getPostalCode();

   public BrasilJustoCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   private String getPostalCode() { return session.getOptions().getString("postal_code"); }

   @Override
   protected void processBeforeFetch() {
      BasicClientCookie cookie = new BasicClientCookie("postal_code", POSTAL_CODE);
      cookie.setDomain("soujusto.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 10;
      String url = HOME_PAGE + "/graphql/";

      JSONObject json = fetchJSON(url);
      JSONObject apiResp = (JSONObject) json.optQuery("/data/search");

      if (this.currentPage == 1) {
         this.totalProducts = apiResp.optInt("total");
      }

      JSONArray products = JSONUtils.getValueRecursive(apiResp, "products/edges", "/", JSONArray.class, new JSONArray());
      for (int i = 0; i < products.length(); i++) {
         JSONObject product = products.getJSONObject(i).optJSONObject("node");
         String urlPath = product != null ? product.optString("url") : null;
         String internalId = product != null ? product.optString("sku") : null;
         String internalPid = scrapInternalPid(urlPath);
         String productUrl = HOME_PAGE + urlPath;
         String name = product != null ? product.optString("name") : null;;
         String imageUrl = product != null ? product.optJSONObject("thumbnail").optString("url") : null;
         Double price = product != null ? product.optJSONObject("availability").optJSONObject("priceRange").optJSONObject("start").optJSONObject("gross").optDouble("amount") * 100 : null;
         Integer priceInCents = price != null ? price.intValue() : null;
         boolean isAvailable = product != null && product.optBoolean("isAvailable");

         RankingProduct productRanking = RankingProductBuilder.create()
            .setUrl(productUrl)
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setImageUrl(imageUrl)
            .setPriceInCents(priceInCents)
            .setAvailability(isAvailable)
            .build();

         saveDataProduct(productRanking);
         if (this.arrayProducts.size() == productsLimit) {
            break;
         }

      }

   }

   private JSONObject fetchJSON(String url) {
      int offset = this.currentPage == 1 ? 0 : (this.currentPage - 1) * this.pageSize;

      String payload = "{\n" +
         "    \"operationName\": \"search\",\n" +
         "    \"variables\": {\n" +
         "        \"first\": " + this.pageSize + ",\n" +
         "        \"query\": \"" + this.location + "\",\n" +
         "        \"offset\": " + offset + ",\n" +
         "        \"filter\": {\n" +
         "            \"postalCode\": \"" + POSTAL_CODE + "\",\n" +
         "            \"categories\": null\n" +
         "        },\n" +
         "        \"onlyEnabledVariants\": true\n" +
         "    },\n" +
         "    \"query\": \"fragment CategoryFragment on Category {\\n  id\\n  name\\n  __typename\\n}\\n\\nfragment TaxedMoneyFragment on TaxedMoney {\\n  gross {\\n    amount\\n    currency\\n    localized\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment ShoppingListFragment on ShoppingList {\\n  id\\n  name\\n  __typename\\n}\\n\\nfragment MoneyFragment on Money {\\n  localized\\n  amount\\n  currency\\n  __typename\\n}\\n\\nfragment ProductFragment on Product {\\n  id\\n  name\\n  isAvailable\\n  url\\n  sku\\n  maxQuantityAllowed\\n  label\\n  labelFontColor\\n  labelBackgroundColor\\n  showPriceWeightUnit\\n  variants {\\n    id\\n    name\\n    stockQuantity\\n    weightUnit\\n    isPiece\\n    bundle {\\n      discountPrice {\\n        ...MoneyFragment\\n        __typename\\n      }\\n      discountMinQuantity\\n      discountLabel\\n      __typename\\n    }\\n    maturationOptions {\\n      description\\n      name\\n      type\\n      __typename\\n    }\\n    __typename\\n  }\\n  shoppingList {\\n    ...ShoppingListFragment\\n    __typename\\n  }\\n  category {\\n    ...CategoryFragment\\n    __typename\\n  }\\n  thumbnail {\\n    url\\n    __typename\\n  }\\n  availability {\\n    discountPercentage\\n    lineMaturationOptions\\n    quantityOnCheckout\\n    variantOnCheckout\\n    priceRange {\\n      start {\\n        ...TaxedMoneyFragment\\n        __typename\\n      }\\n      stop {\\n        ...TaxedMoneyFragment\\n        __typename\\n      }\\n      __typename\\n    }\\n    priceRangeUndiscounted {\\n      start {\\n        ...TaxedMoneyFragment\\n        __typename\\n      }\\n      stop {\\n        ...TaxedMoneyFragment\\n        __typename\\n      }\\n      __typename\\n    }\\n    __typename\\n  }\\n  __typename\\n}\\n\\nquery search($query: String!, $first: Int, $offset: Int, $orderOptions: String, $filter: ProductFilterInput, $onlyEnabledVariants: Boolean) {\\n  search(query: $query) {\\n    pages\\n    total\\n    queryId\\n    categories {\\n      ...CategoryFragment\\n      __typename\\n    }\\n    promotionalRule {\\n      promotionalContent\\n      urlContent\\n      imageContent\\n      __typename\\n    }\\n    products(\\n      first: $first\\n      offset: $offset\\n      orderOptions: $orderOptions\\n      filter: $filter\\n      onlyEnabledVariants: $onlyEnabledVariants\\n    ) {\\n      edges {\\n        node {\\n          ...ProductFragment\\n          searchIndex\\n          __typename\\n        }\\n        __typename\\n      }\\n      __typename\\n    }\\n    __typename\\n  }\\n}\\n\"\n" +
         "}";

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setCookies(this.cookies)
         .mustSendContentEncoding(false)
         .setPayload(payload)
         .build();

      Response response = dataFetcher.post(session, request);

      return CrawlerUtils.stringToJson(response.getBody());
   }

   private String scrapInternalPid(String urlPath) {
      String internalPid = null;

      if (urlPath != null) {
         if ( urlPath.split("/").length > 0) {
            internalPid = urlPath.split("/")[urlPath.split("/").length - 1];
         }
      }

      return internalPid;
   }
}
