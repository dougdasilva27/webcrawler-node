package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;


import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
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


public class BrasilIfoodAppCrawler extends CrawlerRankingKeywords {

   public BrasilIfoodAppCrawler(Session session) {
      super(session);
   }

   @Override
   protected JSONObject fetchJSONObject(String url) {
      String payload = "{" +
         "\"supported-cards\": [" +
         "\"IMAGE_BANNER\"," +
         "\"MINI_BANNER\"," +
         "\"SMALL_BANNER_CAROUSEL\"," +
         "\"MEDIUM_BANNER_CAROUSEL\"," +
         "\"MEDIUM_BANNER_LIST\"," +
         "\"BIG_BANNER_CAROUSEL\"," +
         "\"MERCHANT_LIST\"," +
         "\"MERCHANT_LIST_V2\"," +
         "\"COMPACTED_MERCHANT_LIST\"," +
         "\"MERCHANT_CAROUSEL\"," +
         "\"COMPACTED_MERCHANT_CAROUSEL\"," +
         "\"CATALOG_ITEM_LIST\"," +
         "\"CATALOG_ITEM_LIST_V2\"," +
         "\"CATALOG_ITEM_CAROUSEL\"," +
         "\"CATALOG_ITEM_CAROUSEL_NO_DELIVERY\"," +
         "\"CATALOG_ITEM_CAROUSEL_V2\"," +
         "\"SIMPLE_CATALOG_ITEM_CAROUSEL\"," +
         "\"CATALOG_ITEM_LIST_V3\"," +
         "\"CATALOG_ITEM_CAROUSEL_V3\"," +
         "\"CATALOG_ITEM_QUICK_ADD_LIST\"," +
         "\"CATALOG_ITEM_QUICK_ADD_CAROUSEL\"," +
         "\"SOCIAL_IMAGE_CARD_V2\"," +
         "\"SOCIAL_POST\"," +
         "\"ROUND_IMAGE_CAROUSEL\"," +
         "\"ROUND_IMAGE_CAROUSEL_V2\"," +
         "\"MERCHANT_LIST_WITH_CATALOG_ITEMS\"," +
         "\"FEATURED_MERCHANT_LIST\"," +
         "\"FEATURED_MERCHANT_CAROUSEL\"," +
         "\"SIMPLE_MERCHANT_CAROUSEL\"," +
         "\"SIMPLE_MERCHANT_LIST\"," +
         "\"BANNER_GRID\"," +
         "\"NEXT_CONTENT\"," +
         "\"INFO_CARD\"," +
         "\"MERCHANT_TILE_CAROUSEL\"," +
         "\"MERCHANT_LIST_WITH_ITEMS_CAROUSEL\"," +
         "\"CATALOG_ITEM_CAROUSEL_WITH_TIMER\"," +
         "\"MERCHANT_GROUPED_CAROUSEL\"," +
         "\"MERCHANT_GROUPED_CAROUSEL_V2\"," +
         "\"WAITING_LEGACY_IMAGE_BANNER\"," +
         "\"WAITING_LEGACY_INFO_BANNER\"," +
         "\"INFO_POSTER_CAROUSEL\"," +
         "\"MERCHANT_MENU_ITEM_LIST\"," +
         "\"MERCHANT_MENU_WITHOUT_RATING\"," +
         "\"MERCHANT_MENU_OVERALL_INFO\"," +
         "\"MERCHANT_MENU_DELIVERY_METHODS_CLOSED_STATE\"," +
         "\"MERCHANT_MENU_DELIVERY_METHODS_OUT_OF_AREA_STATE\"," +
         "\"MERCHANT_MENU_DELIVERY_METHODS_SCHEDULE_ONLY_STATE\"," +
         "\"MERCHANT_MENU_DELIVERY_METHODS_TIME_SLOT_STATE\"," +
         "\"MERCHANT_MENU_MINIMUM_ORDER\"," +
         "\"MERCHANT_MENU_RATING\"," +
         "\"IMAGE_GRID\"," +
         "\"IMAGE_FOLDER_GRID\"," +
         "\"MEDIUM_IMAGE_BANNER\"," +
         "\"SMALL_CATALOG_ITEM_LIST\"," +
         "\"SMALL_CATALOG_ITEM_CAROUSEL\"," +
         "\"AWARENESS_BANNER_CAROUSEL\"," +
         "\"SMALL_ICON_GRID\"," +
         "\"RELATED_SEARCH_CAROUSEL\"," +
         "\"ACTION_BUTTON\"," +
         "\"ACTION_BUTTON_V2\"," +
         "\"SIMPLE_HEADER\"," +
         "\"MERCHANT_WITH_CATALOG_ITEMS_CAROUSEL\"," +
         "\"AWARENESS_MERCHANT_CAROUSEL\"," +
         "\"SEARCH_BAR\"," +
         "\"MOSAIC\"," +
         "\"TRENDING_MERCHANT_CAROUSEL\"," +
         "\"VOUCHER_WALLET_LIST\"," +
         "\"CATALOG_PROMOTIONAL_ITEMS_CAROUSEL\"," +
         "\"SIMILAR_MERCHANT_CAROUSEL\"," +
         "\"SMALL_ICON_GRID_V2\"," +
         "\"SHOPPING_LIST_CAROUSEL\"," +
         "\"VERTICAL_BANNER_CAROUSEL\"," +
         "\"BANNER_SLIDESHOW\"," +
         "\"INFO_CARD_CAROUSEL\"," +
         "\"FEATURED_ITEM_CAROUSEL\"," +
         "\"COMPARATOR_MERCHANT_CAROUSEL\"," +
         "\"COMPARATOR_MERCHANT_CAROUSEL_V2\"," +
         "\"HIGHLIGHTED_ITEM_CAROUSEL\"," +
         "\"GROCERIES_REORDER_CAROUSEL\"," +
         "\"VERTICAL_CONTENT_LIST_CAROUSEL\"," +
         "\"FEATURED_SQUARE_IMAGE_CAROUSEL\" " +
         "]," +
         "\"supported-headers\": [ " +
         "\"OPERATION_HEADER\"," +
         "\"SIMPLE_SECTION_HEADER\" " +
         "]," +
         "\"supported-actions\": [ " +
         "\"payment-details\"," +
         "\"create-favorites-folder\"," +
         "\"feed\"," +
         "\"grocery-categories\"," +
         "\"merchant\"," +
         "\"feed-profile\"," +
         "\"page\"," +
         "\"reorder\"," +
         "\"donations-results\"," +
         "\"voucher-wallet\"," +
         "\"shopping-details\"," +
         "\"merchant-menu-rating\"," +
         "\"open-search\"," +
         "\"search\"," +
         "\"purchase-ifood-card\"," +
         "\"merchant-menu-details\"," +
         "\"groceries-details\"," +
         "\"catalog-item\"," +
         "\"donations-checkout\"," +
         "\"register-new-payment\"," +
         "\"donations-detail\"," +
         "\"merchant-menu-delivery-time-slot\"," +
         "\"favorites\"," +
         "\"redeem-ifood-card\"," +
         "\"home\"," +
         "\"showcase-aisle\"," +
         "\"fullscreen-video\"," +
         "\"voucher-details\"," +
         "\"shoppinglist\"," +
         "\"merchant-menu-search\"," +
         "\"post-details\"," +
         "\"quick-commerce\"," +
         "\"grocery-categories-list\"," +
         "\"donations-list\"," +
         "\"loyalty\"," +
         "\"payment-transaction-details\"," +
         "\"shorts\"," +
         "\"card-content\"," +
         "\"display_express_status_component\"," +
         "\"home-tab\"," +
         "\"merchant-page\"," +
         "\"payment-list\"," +
         "\"webmiddleware\"," +
         "\"groceries\"," +
         "\"last-restaurants\"," +
         "\"webview\"," +
         "\"payment-method\"," +
         "\"card-content-fallback\"," +
         "\"payment-page\" " +
         "]," +
         "\"faster-overrides\": \"{ \\\"storeTypeFilterEnabled\\\": true }\"," +
         "\"feature-availability\": { " +
         "\"home_feed_feature\": \"{ \\\"enabled\\\": true }\"," +
         "\"quick_journey_availability\": \"{ \\\"enabled\\\": false }\"," +
         "\"groceries_ordering_itens_tico_enabled\": \"{\\\"default\\\": {\\\"model_id\\\": \\\"ifood-ml-groceries-tico-v2-endpoint\\\"}}\"}}";
      Map<String, String> headers = new HashMap<>();
      //headers.put("alias", "SEARCH_RESULTS_ITEM_TAB_GLOBAL");
      //headers.put("latitude", "-16.724496702061654");
      //headers.put("longitude", "-43.81544463336468");
      //headers.put("zip_code", "39404301");
      //headers.put("channel", "IFOOD");
      //headers.put("term", "cerveja");
      headers.put("content-type", "application/json; charset=UTF-8");
      headers.put("host", "marketplace.ifood.com.br");
      headers.put("user-agent", "okhttp/4.10.0");
      //headers.put("search_method", "Term");
      headers.put("Accept-Encoding", "gzip,deflate,br");
//      headers.put("search_id","84e1d1c6-d598-4905-a335-9074e3310521");
//      headers.put("experiment_details","{\"default_item\":{\"is_discovery_search_service_enabled\": true,\"is_app_experiment_enabled\": true,\"model_id\":\"search-optimus-prime-endpoint\",\"engine\":\"sagemaker\",\"backend_experiment_id\":\"v4\",\"query_rewriter_rule\":\"items-synonyms\",\"force_similar_search_disabled\":true}}");
      //headers.put("authorization","Bearer eyJraWQiOiJiNjRjNjZmZS00ZWY5LTExZTktODY0Ny1kNjYzYmQ4NzNkOTMiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzUxMiJ9.eyJzdWIiOiI2MzcxMWZiZi1kMWRlLTQ3MjAtYTJjYi0zOWRmMTg4ZTljZTAiLCJpc3MiOiJpRm9vZCIsInRlbmFudElkIjoiSUZPIiwidHlwZSI6ImFjY2Vzc190b2tlbiIsImV4cCI6MTY4MTk5MjQwOSwiaWF0IjoxNjgxOTg1MjA5LCJkaWQiOiJmMDUzNWI0MzQ5MDRmOGM5NmJkNDQyZTMyMzMzOGRjYTFjOTk1MTlkOTc4NjljYTZmZTA0NjlhNTk5ZDZjNTI2ZDdkODU5NDg2NzA5OTU4YWRjYjE1MDI2NjliNjQ0ZmY4YmE2YmM0OTg4Y2E3MmM0MTA5YTkwNDg5OGNkYmYxNyJ9.CZHq6-OncwtZQo3o07jkuQjFeRaDRPasTMOdLb7LEUaQ7Nt5U4cn2yy_cAmk1p9HHgplCvVvjIHf3jLX__ZbGz9dYn3uZ6jlyoca2oiw-NxchLxMD5UjO0FWXBZgeY7D_RMBDiRYVXCA6_rLLRZ-Hw5ukQ5nglSIxt9axW4PEXDXsEtW5mIzMdKjqd30DK5U8xMHHuROQgnFhQHzC-sbwzpW0xtnumRPswkAm35NAF5ReN7oWbuHPdsNeZGBVG_2S9YW_IGjNxD-BFxvHobPzoXOVh8s0SUBcGDT7ZZUdMi0CYTXVpZ56X0XGDBUgqrPYD7wiKKHILsAZNGGxmMRGQ");
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .setProxyservice(Arrays.asList(
            ProxyCollection.SMART_PROXY_BR_HAPROXY,
            ProxyCollection.SMART_PROXY_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR
         ))
         .build();
      Response response = new FetcherDataFetcher().post(session, request);
      return CrawlerUtils.stringToJson(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String url = "https://marketplace.ifood.com.br/v2/cardstack/search/results?alias=SEARCH_RESULTS_ITEM_TAB_GLOBAL&latitude=-16.724496702061654&longitude=-43.81544463336468&zip_code=39404301&channel=IFOOD&term=cerveja";
      JSONObject jsonObject = fetchJSONObject(url);
      if (jsonObject != null && jsonObject.has("id")) {
         String idM = jsonObject.optString("id");
         String baseImageUrl = jsonObject.optString("baseImageUrl");
         JSONArray items = JSONUtils.getValueRecursive(jsonObject, "sections.0.cards.0.data.contents", JSONArray.class, new JSONArray());
         for (Object o : items) {
            JSONObject item = (JSONObject) o;
         }
      }
   }
}
