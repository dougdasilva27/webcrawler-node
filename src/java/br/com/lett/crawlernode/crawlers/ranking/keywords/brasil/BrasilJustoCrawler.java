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

      JSONArray products = (JSONArray) apiResp.optQuery("/products/edges");
      for (int i = 0; i < products.length(); i++) {
         JSONObject product = products.getJSONObject(i).optJSONObject("node");
         String urlPath = product != null ? product.optString("url") : null;
         String internalId = product != null ? product.optString("sku") : null;
         String internalPid = urlPath != null ? urlPath.split("/").length > 0 ? urlPath.split("/")[urlPath.split("/").length - 1] : null : null;
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
      }

   }

   private JSONObject fetchJSON(String url) {
      int offset = this.currentPage == 1 ? 0 : (this.currentPage - 1) * this.pageSize;

      String payload = "{\"operationName\":\"search\",\"variables\":{\"first\":" + this.pageSize + ",\"query\":\"" + this.location + "\",\"offset\":" + offset + ","
         + "\"filter\":{\"postalCode\":\"" + POSTAL_CODE + "\",\"categories\":null}},\"query\":\"fragment TaxedMoneyFragment on"
         + " TaxedMoney {\\n  gross {\\n    amount\\n    currency\\n    localized\\n    __typename\\n  }\\n  __typename\\n}"
         + "\\n\\nfragment CategoryFragment on Category {\\n  id\\n  name\\n  __typename\\n}\\n\\nfragment ShoppingListFragment "
         + "on ShoppingList {\\n  id\\n  name\\n  __typename\\n}\\n\\nquery search($query: String!, $first: Int, $offset: Int, "
         + "$orderOptions: String, $filter: ProductFilterInput) {\\n  search(query: $query) {\\n    pages\\n    total\\n    categories "
         + "{\\n      ...CategoryFragment\\n      __typename\\n    }\\n    products(\\n      first: $first\\n      offset: $offset\\n"
         + "      orderOptions: $orderOptions\\n      filter: $filter\\n    ) {\\n      edges {\\n        node {\\n          id\\n          "
         + "name\\n          isAvailable\\n          url\\n          action\\n          sku\\n          maxQuantityAllowed\\n          "
         + "useWeightPicker\\n          searchIndex\\n          variants {\\n            id\\n            name\\n            stockQuantity"
         + "\\n            weightUnit\\n            isPiece\\n            availability {\\n              price {\\n                ...TaxedMoneyFragment"
         + "\\n                __typename\\n              }\\n              __typename\\n            }\\n            maturationOptions {\\n              "
         + "description\\n              name\\n              type\\n              __typename\\n            }\\n            __typename\\n          }\\n          "
         + "shoppingList {\\n            ...ShoppingListFragment\\n            __typename\\n          }\\n          category {\\n            ...CategoryFragment"
         + "\\n            __typename\\n          }\\n          thumbnail {\\n            url\\n            __typename\\n          }\\n          "
         + "availability {\\n            discountPercentage\\n            lineMaturationOptions\\n            quantityOnCheckout\\n            "
         + "variantOnCheckout\\n            priceRange {\\n              start {\\n                ...TaxedMoneyFragment\\n                __typename"
         + "\\n              }\\n              stop {\\n                ...TaxedMoneyFragment\\n                __typename\\n              }\\n"
         + "              __typename\\n            }\\n            priceRangeUndiscounted {\\n              start {\\n                ...TaxedMoneyFragment"
         + "\\n                __typename\\n              }\\n              stop {\\n                ...TaxedMoneyFragment\\n                __typename"
         + "\\n              }\\n              __typename\\n            }\\n            __typename\\n          }\\n          __typename\\n        }\\n"
         + "        __typename\\n      }\\n      __typename\\n    }\\n    __typename\\n  }\\n}\\n\"}";

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
}
