package br.com.lett.crawlernode.crawlers.ranking.keywords.peru;

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
import br.com.lett.crawlernode.util.MathUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PeruJokrCrawler extends CrawlerRankingKeywords {
   public PeruJokrCrawler(Session session) {
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

      String urlApi = "https://api-prd-pe.jokrtech.com/";
      this.log("Link onde são feitos os crawlers: " + urlApi);

      String payloadRanking = "{\"operationName\":\"SearchProducts\",\"variables\":{\"hubId\":\"" + hubId + "\",\"searchTerm\":\"" + location + "\"},\"query\":\"query SearchProducts($searchTerm: String!, $hubId: String) {\\n  searchProducts(searchTerm: $searchTerm, hubId: $hubId) {\\n    products {\\n      sku\\n      __typename\\n    }\\n    queryId\\n    topSortInfo {\\n      activated\\n      auctionId\\n      products {\\n        ...TopsortFields\\n        __typename\\n      }\\n      __typename\\n    }\\n    __typename\\n  }\\n}\\n\\nfragment TopsortFields on TopSortProduct {\\n  sku\\n  topSortRank\\n  resolvedBidId\\n  __typename\\n}\\n\"}";
      JSONObject jsonRanking = fetchJsonAPi(urlApi, payloadRanking);

      String payloadOffers = "{\"operationName\":\"dynamicProductFields\",\"variables\":{\"hubId\":\"" + hubId + "\"},\"query\":\"query dynamicProductFields($hubId: String!) {\\n  availableCategories(hubId: $hubId) {\\n    hubId\\n    products {\\n      sku\\n      price(hubId: $hubId) {\\n        amount\\n        compareAtPrice\\n        __typename\\n      }\\n      inventory(hubId: $hubId) {\\n        quantity\\n        maxQuantity\\n        __typename\\n      }\\n      base_price_relevant\\n      pum_conv_factor\\n      standard_pricing_unit\\n      __typename\\n    }\\n    __typename\\n  }\\n}\\n\"}";
      JSONObject jsonOffers = fetchJsonAPi(urlApi, payloadOffers);

      String payloadAllProduct = "{\"operationName\":\"allCategories\",\"variables\":{\"hubId\":\"" + hubId + "\"},\"query\":\"query allCategories($hubId: String!) {\\n  availableCategories(hubId: $hubId) {\\n    hubId\\n    categories {\\n      cmsMainCategory {\\n        jokr_id\\n        title\\n        list_image_three {\\n          url(transformation: {image: {resize: {width: 200, height: 200, fit: clip}}})\\n          __typename\\n        }\\n        __typename\\n      }\\n      subCategories {\\n        cmsSubCategory {\\n          jokr_id\\n          title\\n          __typename\\n        }\\n        products {\\n          cmsProduct {\\n            ...CoreProductFields\\n            __typename\\n          }\\n          __typename\\n        }\\n        __typename\\n      }\\n      __typename\\n    }\\n    __typename\\n  }\\n}\\n\\nfragment CoreProductFields on Product {\\n  sku\\n  title\\n  title2\\n  packshot1_front_grid_small: packshot1_front_grid {\\n    url(transformation: {image: {resize: {width: 266, height: 266, fit: clip}}})\\n    __typename\\n  }\\n  ui_content_1\\n  ui_content_1_uom\\n  ui_content_2\\n  ui_content_2_uom\\n  tags\\n  __typename\\n}\\n\"}";
      JSONObject jsonAllProducts = fetchJsonAPi(urlApi, payloadAllProduct);


      if (!jsonRanking.isEmpty()) {
         JSONArray productsArray = JSONUtils.getValueRecursive(jsonRanking, "data.searchProducts.products", JSONArray.class);
         this.log("Total da busca: " + productsArray.length());

         for (Object productObj : productsArray) {
            JSONObject product = (JSONObject) productObj;
            String internalPid = product.optString("sku");

            JSONObject objSku = searchJsonSku(jsonAllProducts, internalPid);
            String name = objSku.optString("title");
            String imageUrl = JSONUtils.getValueRecursive(objSku, "packshot1_front_grid_small.url", String.class);

            Map<String, JSONObject> productSkuPrice = new HashMap<>();
            productSkuPrice = getOffersSku(jsonOffers);

            boolean isAvailable = productSkuPrice.get(internalPid) != null;
            Integer price = isAvailable ? getPriceJson(productSkuPrice.get(internalPid)) : null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(internalPid)
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
      return (int) Math.round((Double) spotlightPrice * 100);
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
      return null;
   }

   protected JSONObject fetchJsonAPi(String url, String payload) {


      HashMap<String, String> headers = new HashMap<>();
      headers.put("Content-type", "application/json");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setPayload(payload)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY
         ))
         .build();
      Response response = CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher());
      return JSONUtils.stringToJson(response.getBody());
   }
}
