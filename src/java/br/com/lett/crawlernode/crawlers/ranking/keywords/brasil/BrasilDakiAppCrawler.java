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

public class BrasilDakiAppCrawler extends CrawlerRankingKeywords {
   public BrasilDakiAppCrawler(Session session) {
      super(session);
   }

   private final String hubId = getHubId();

   protected String getHubId() {
      return session.getOptions().optString("hub_id");
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 12;
      this.log("Página " + this.currentPage);

      String payloadRanking = "{\"operationName\":\"SearchProducts\",\"variables\":{\"hubId\":\"" + hubId + "\",\"searchTerm\":\"" + location + "\"},\"query\":\"query SearchProducts($searchTerm: String!, $hubId: String) {\\n  searchProducts(searchTerm: $searchTerm, hubId: $hubId) {\\n    products {\\n      sku\\n      __typename\\n    }\\n    queryId\\n    topSortInfo {\\n      activated\\n      auctionId\\n      products {\\n        ...TopsortFields\\n        __typename\\n      }\\n      __typename\\n    }\\n    __typename\\n  }\\n}\\n\\nfragment TopsortFields on TopSortProduct {\\n  sku\\n  topSortRank\\n  resolvedBidId\\n  __typename\\n}\\n\"}";
      JSONObject jsonRanking = fetchJsonAPi(payloadRanking);

      String payloadOffers = "{\"operationName\":\"dynamicProductFields\",\"variables\":{\"hubId\":\"" + hubId + "\"},\"query\":\"query dynamicProductFields($hubId: String!) {\\n  availableCategories(hubId: $hubId) {\\n    hubId\\n    products {\\n      sku\\n      price(hubId: $hubId) {\\n        amount\\n        compareAtPrice\\n        __typename\\n      }\\n      inventory(hubId: $hubId) {\\n        quantity\\n        maxQuantity\\n        __typename\\n      }\\n      base_price_relevant\\n      pum_conv_factor\\n      standard_pricing_unit\\n      __typename\\n    }\\n    __typename\\n  }\\n}\\n\"}";
      JSONObject jsonOffers = fetchJsonAPi(payloadOffers);

      String payloadAllProduct = "{\"operationName\":\"allCategories\",\"variables\":{\"hubId\":\"" + hubId + "\"},\"query\":\"query allCategories($hubId: String!) {\\n  availableCategories(hubId: $hubId) {\\n    hubId\\n    categories {\\n      cmsMainCategory {\\n        jokr_id\\n        title\\n        list_image_three {\\n          url(transformation: {image: {resize: {width: 200, height: 200, fit: clip}}})\\n          __typename\\n        }\\n        __typename\\n      }\\n      subCategories {\\n        cmsSubCategory {\\n          jokr_id\\n          title\\n          __typename\\n        }\\n        products {\\n          cmsProduct {\\n            ...CoreProductFields\\n            __typename\\n          }\\n          __typename\\n        }\\n        __typename\\n      }\\n      __typename\\n    }\\n    __typename\\n  }\\n}\\n\\nfragment CoreProductFields on Product {\\n  sku\\n  title\\n  title2\\n  packshot1_front_grid_small: packshot1_front_grid {\\n    url(transformation: {image: {resize: {width: 266, height: 266, fit: clip}}})\\n    __typename\\n  }\\n  ui_content_1\\n  ui_content_1_uom\\n  ui_content_2\\n  ui_content_2_uom\\n  tags\\n  category {\\n    cmsMainCategory {\\n      jokr_id\\n      title\\n      __typename\\n    }\\n    __typename\\n  }\\n  subCategory {\\n    cmsSubCategory {\\n      jokr_id\\n      title\\n      __typename\\n    }\\n    __typename\\n  }\\n  __typename\\n}\\n\"}";
      JSONObject jsonAllProducts = fetchJsonAPi(payloadAllProduct);

      if (!jsonRanking.isEmpty()) {
         JSONArray productsArray = JSONUtils.getValueRecursive(jsonRanking, "data.searchProducts.products", JSONArray.class);
         this.log("Total da busca: " + productsArray.length());

         for (Object productObj : productsArray) {
            JSONObject product = (JSONObject) productObj;
            String internalPid = product.optString("sku");

            JSONObject objSku = searchJsonSku(jsonAllProducts, internalPid);
            String name = objSku.has("name") ? objSku.optString("name") : objSku.optString("title");
            String imageUrl = objSku.has("packshot1_front_grid") ? JSONUtils.getValueRecursive(objSku, "packshot1_front_grid.url", String.class) : JSONUtils.getValueRecursive(objSku, "packshot1_front_grid_small.url", String.class);

            Map<String, JSONObject> productSkuPrice = new HashMap<>();
            productSkuPrice = getOffersSku(jsonOffers);

            boolean isAvailable = productSkuPrice.get(internalPid) != null;
            Integer price = isAvailable ? getPriceJson(productSkuPrice.get(internalPid)) : null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(session.getOptions().optString("preLink", "") + internalPid)
               .setInternalId(internalPid)
               .setInternalPid(internalPid)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
               .build();

            saveDataProduct(productRanking);

            if (this.arrayProducts.size() == productsLimit) break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private Integer getPriceJson(JSONObject jsonObject) {
      Double spotlightPrice = jsonObject.optDouble("amount");
      return (int) Math.round(spotlightPrice * 100);
   }

   private Map<String, JSONObject> getOffersSku(JSONObject jsonObject) {
      JSONArray array = JSONUtils.getValueRecursive(jsonObject, "data.availableCategories.products", JSONArray.class);
      Map<String, JSONObject> productOffers = new HashMap<>();
      for (Object obj : array) {
         JSONObject jsonSku = (JSONObject) obj;
         String sku = jsonSku.optString("sku");
         Integer quantity = JSONUtils.getValueRecursive(jsonSku, "inventory.quantity", Integer.class);

         JSONObject price = quantity > 0 ? jsonSku.optJSONObject("price") : null;

         productOffers.put(sku, price);
      }

      return productOffers;
   }

   private JSONObject searchJsonSku(JSONObject jsonAllProducts, String internalPid) {
      JSONArray categories = JSONUtils.getValueRecursive(jsonAllProducts, "data.availableCategories.categories", JSONArray.class);
      for (Object categoryObj : categories) {
         JSONObject category = (JSONObject) categoryObj;
         JSONArray subCategories = JSONUtils.getValueRecursive(category, "subCategories", JSONArray.class);
         for (Object subCategoryObj : subCategories) {
            JSONObject subCategory = (JSONObject) subCategoryObj;
            JSONArray products = JSONUtils.getValueRecursive(subCategory, "products", JSONArray.class);
            for (Object productObj : products) {
               JSONObject product = (JSONObject) productObj;
               String cmsProductSku = JSONUtils.getValueRecursive(product, "cmsProduct.sku", String.class);
               if (cmsProductSku.equals(internalPid)) {
                  return JSONUtils.getValueRecursive(product, "cmsProduct", JSONObject.class);
               }
            }
         }
      }
      String payloadObjSku = "{\"operationName\":\"ProductDetails\",\"variables\":{\"hubId\":\"" + hubId + "\",\"where\":{\"sku_in\":[\"" + internalPid + "\"]}},\"query\":\"query ProductDetails($where: ProductFilters!, $hubId: String!) {\\n  products(where: $where) {\\n    brand\\n    category {\\n      cmsMainCategory {\\n        title\\n        __typename\\n      }\\n      __typename\\n    }\\n    inventory(hubId: $hubId) {\\n      quantity\\n      showOutOfStock\\n      status\\n      __typename\\n    }\\n    long_description\\n    ui_content_1\\n    packshot1_front_grid {\\n      url\\n      __typename\\n    }\\n    product_status\\n    price(hubId: $hubId) {\\n      amount\\n      compareAtPrice\\n      discount\\n      id\\n      sku\\n      __typename\\n    }\\n    name\\n    title\\n    sku\\n    tags\\n    __typename\\n  }\\n}\"}";
      JSONObject objSku = fetchJsonAPi(payloadObjSku);

      return JSONUtils.getValueRecursive(objSku, "data.products.0", JSONObject.class, new JSONObject());
   }

   protected JSONObject fetchJsonAPi(String payload) {
      HashMap<String, String> headers = new HashMap<>();
      headers.put("accept-encoding", "gzip");
      headers.put("connection", "Keep-Alive");
      headers.put("content-type", "application/json");
      headers.put("host", "api-prd-br.jokrtech.com");
      headers.put("x-brand-name", "daki");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://api-prd-br.jokrtech.com/")
         .setPayload(payload)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_ROTATE_BR,
            ProxyCollection.BUY
         ))
         .build();
      Response response = CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher());
      return JSONUtils.stringToJson(response.getBody());
   }
}
