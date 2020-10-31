package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public abstract class BrasilRappiCrawler extends RappiCrawler {

   private static final String HOME_PAGE = "https://www.rappi.com";
   private static final String STORES_API_URL = "https://services.rappi.com.br/api/base-crack/principal?";
   protected static final String IMAGES_DOMAIN = "images.rappi.com.br/products";

   private final String locationParameters = setLocationParameters();
   private final String storeType = setStoreType();
   private final String storeIdMainStore = setStoreId();

   public BrasilRappiCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
   }

   protected abstract String setStoreType();

   protected String setStoreId() {
      return null;
   }

   protected abstract String setLocationParameters();

   @Override
   protected String getImagesDomain() {
      return IMAGES_DOMAIN;
   }

   @Override
   protected JSONObject fetch() {
      JSONObject productsInfo = new JSONObject();

      String storeId;

      if (this.storeIdMainStore == null) {
         Map<String, String> stores = crawlStores();
         storeId = stores.containsKey(storeType) ? stores.get(storeType) : null;
      } else {
         storeId = this.storeIdMainStore;
      }

      String productUrl = session.getOriginalURL();
      String productId = null;

      if (productUrl.contains("_")) {
         productId = CommonMethods.getLast(productUrl.split("\\?")[0].split("_"));
      }

      if (productId != null && storeId != null) {
         JSONObject data = JSONUtils.stringToJson(fetchProduct(productId, storeId, fetchToken()));

         JSONArray components = JSONUtils.getValueRecursive(data, "data.components", JSONArray.class);

         if (components != null) {
            for (Object json: components) {
               if (json instanceof JSONObject) {
                  String nameComponents = ((JSONObject) json).optString("name");
                  if (nameComponents.equals("product_information")) {
                     productsInfo = JSONUtils.getJSONValue((JSONObject) json, "resource");
                  }
               }
            }
         }
      }
      return productsInfo;
   }

   String fetchProduct(String productId, String storeId, String token) {

      String url = "https://services.rappi.com.br/api/dynamic/context/content";
      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "application/json, text/plain, */*");
      headers.put("language", "pt");
      headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36");
      headers.put("content-type", "application/json");
      headers.put("authorization", token);

      String payload = "{\"state\":{\"product_id\":\""+productId+"\"},\"limit\":100,\"offset\":0,\"context\":\"product_detail\",\"stores\":["+storeId+"]}";

      Request request = RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .build();

      return this.dataFetcher.post(session, request).getBody();
   }

   String fetchToken() {
      String url = "https://services.rappi.com.br/api/auth/guest_access_token";
      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "application/json, text/plain, */*");
      headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36");
      headers.put("content-type", "application/json");

      String payload = "{\"headers\":{\"normalizedNames\":{},\"lazyUpdate\":null},\"grant_type\":\"guest\"}";

      Request request = RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .mustSendContentEncoding(false)
         .build();

      JSONObject json = JSONUtils.stringToJson(this.dataFetcher.post(session, request).getBody());

      String token = json.optString("access_token");
      String tokenType = json.optString("token_type");

      if (tokenType.equals("Bearer")) {
         token = tokenType + " " + token;
      }

      return token;
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
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
