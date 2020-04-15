package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

public abstract class BrasilRappiCrawler extends RappiCrawler {

   private static final String HOME_PAGE = "https://www.rappi.com";
   private static final String STORES_API_URL = "https://services.rappi.com.br/api/base-crack/principal?";
   public static final String PRODUCTS_API_URL = "https://services.rappi.com.br/api/search-client/search/v2/products";
   private static final String DETAILS_API_URL = "https://services.rappi.com.br/api/cpgs-graphql-domain/";
   protected static final String IMAGES_DOMAIN = "images.rappi.com.br/products";

   private final String locationParameters = setLocationParameters();
   private final String storeType = setStoreType();

   public BrasilRappiCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
   }

   protected abstract String setStoreType();

   protected abstract String setLocationParameters();

   @Override
   protected String getImagesDomain() {
      return IMAGES_DOMAIN;
   }

   @Override
   protected JSONObject fetch() {
      JSONObject productsInfo = new JSONObject();
      Map<String, String> stores = crawlStores();

      String productUrl = session.getOriginalURL();
      String storeId = stores.containsKey(storeType) ? stores.get(storeType) : null;
      String productId = null;

      if (productUrl.contains("_")) {
         productId = CommonMethods.getLast(productUrl.split("\\?")[0].split("_"));
      }

      if (productId != null && storeType != null && storeId != null) {
         Map<String, String> headers = new HashMap<>();

         String url = "https://services.rappi.com.br/windu/products/store/" + storeId + "/product/" + productId;
         Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).mustSendContentEncoding(false).build();

         String page = this.dataFetcher.get(session, request).getBody();

         if (page.startsWith("{") && page.endsWith("}")) {
            try {
               JSONObject apiResponse = new JSONObject(page);

               if (apiResponse.has("product") && apiResponse.get("product") instanceof JSONObject) {
                  productsInfo = apiResponse.getJSONObject("product");
               }

            } catch (Exception e) {
               Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
            }
         }
      }

      return productsInfo;
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   protected JSONArray crawlProductImagesFromApi(String productId, String storeId) {
      JSONArray productImages = new JSONArray();

      if (productId != null && storeId != null) {
         Map<String, String> headers = new HashMap<>();
         headers.put("Content-Type", "application/json");

         try {
            long productIdNumber = Long.parseLong(productId);
            long storeIdNumber = Long.parseLong(storeId);

            JSONObject payload = new JSONObject();
            payload.put("operationName", "fetchProductDetails");
            payload.put("variables", new JSONObject().put("productId", productIdNumber).put("storeId", storeIdNumber));
            payload.put("query", "query fetchProductDetails($productId: Int!, $storeId: Int!) " +
                  "{\n productDetail(productId: $productId, storeId: $storeId) {\n longDescription\n " +
                  "images {\n name\n }\n toppings {\n id\n description\n toppingTypeId\n " +
                  "minToppingsForCategories\n maxToppingsForCategories\n index\n presentationTypeId\n " +
                  "topping {\n id\n description\n toppingCategoryId\n price\n maxLimit\n index\n }\n " +
                  "}\n nutritionFactsImg {\n name\n }\n additionalInformation {\n attribute\n value\n " +
                  "}\n ingredients {\n name\n }\n }\n}\n");

            Request request = RequestBuilder.create()
                  .setUrl(DETAILS_API_URL)
                  .setHeaders(headers)
                  .mustSendContentEncoding(false)
                  .setPayload(payload.toString())
                  .build();

            String page = this.dataFetcher.post(session, request).getBody();

            if (page.startsWith("{") && page.endsWith("}")) {
               JSONObject apiResponse = new JSONObject(page);

               if (apiResponse.has("data") && apiResponse.get("data") instanceof JSONObject) {
                  apiResponse = apiResponse.getJSONObject("data");

                  if (apiResponse.has("productDetail") && apiResponse.get("productDetail") instanceof JSONObject) {
                     apiResponse = apiResponse.getJSONObject("productDetail");

                     if (apiResponse.has("images") && apiResponse.get("images") instanceof JSONArray) {
                        productImages = apiResponse.getJSONArray("images");
                     }
                  }
               }
            }
         } catch (Exception e) {
            Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
         }
      }


      return productImages;
   }

   private Map<String, String> crawlStores() {
      Map<String, String> stores = new HashMap<>();
      Request request = RequestBuilder.create().setUrl(STORES_API_URL + this.locationParameters + "&device=2").setCookies(cookies).build();
      JSONArray options = CrawlerUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());

      for (Object o : options) {
         JSONObject option = (JSONObject) o;

         if (option.has("suboptions")) {
            JSONArray suboptions = option.getJSONArray("suboptions");

            for (Object ob : suboptions) {
               JSONObject suboption = (JSONObject) ob;
               if (suboption.has("stores")) {
                  setStores(suboption.getJSONArray("stores"), stores);
               }
            }
         } else if (option.has("stores")) {
            setStores(option.getJSONArray("stores"), stores);
         }
      }

      return stores;
   }

   private void setStores(JSONArray storesArray, Map<String, String> stores) {
      for (Object o : storesArray) {
         JSONObject store = (JSONObject) o;

         if (store.has("store_id") && store.has("store_type")) {
            stores.put(store.getString("store_type"), store.getString("store_id"));
         }
      }
   }
}
