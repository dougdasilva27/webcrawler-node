package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BrasilDavoSupermercadosCrawler extends CrawlerRankingKeywords {
   public BrasilDavoSupermercadosCrawler(Session session) {
      super(session);

   }

   private static final String baseUrlApi = "https://www.davo.com.br/ccstore/v1/";
   private static final String hostUrl = "https://www.davo.com.br/";
   private final String locationId = session.getOptions().optString("location_id", "");

   private static final String[] headers = {"authority", "www.davo.com.br", "accept", "application/json", "user-agent", "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Mobile Safari/537.36"};

   private HttpResponse<String> getResponseFromApi(String url) {
      try {
         HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(url))
            .headers(headers)
            .build();
         HttpClient client = HttpClient.newBuilder()
            .proxy(ProxySelector.of(new InetSocketAddress("haproxy.lett.global", 3130)))
            .build();
         return client.send(request, HttpResponse.BodyHandlers.ofString());

      } catch (Exception e) {
         throw new RuntimeException("Failed in request: ", e);
      }
   }


   @Override
   protected JSONObject fetchJSONObject(String url) {
      try {
         HttpResponse<String> response = getResponseFromApi(url);
         JSONObject returnObject = JSONUtils.stringToJson(response.body());
         return JSONUtils.getValueRecursive(returnObject, "results", JSONObject.class, new JSONObject());
      } catch (Exception e) {
         throw new RuntimeException("Failed In load document: " + session.getOriginalURL(), e);
      }
   }

   private HashMap<String, Boolean> getProductAvailabilities(List<RankingProduct> rankingProducts) {
      if(rankingProducts!=null && !rankingProducts.isEmpty()) {
         StringBuilder queryParam = new StringBuilder(rankingProducts.get(0).getInternalId());
         for (int i = 1; i < rankingProducts.size(); i++) {
            queryParam.append("%2C").append(rankingProducts.get(i).getInternalId());
         }
         try {
            HttpResponse<String> response = getResponseFromApi(baseUrlApi + "stockStatus?products=" + queryParam + "locationIds=" + locationId);
            JSONObject returnObject = JSONUtils.stringToJson(response.body());
            if (returnObject.has("items")) {
               HashMap<String, Boolean> results = new HashMap<>();
               JSONArray items = returnObject.optJSONArray("items");
               for (Object o : items) {
                  JSONObject item = (JSONObject) o;
                  if (!item.isEmpty()) {
                     boolean availability = item.optString("stockStatus", "").equals("IN_STOCK");
                     String internalId = JSONUtils.getValueRecursive(item, "productSkuInventoryDetails.0.productId", String.class, "");
                     if (!internalId.isEmpty()) {
                        results.put(internalId, availability);
                     }
                  }
               }
               return results;
            }
         } catch (Exception e) {
            throw new RuntimeException("Failed In load document: " + session.getOriginalURL(), e);
         }

      }
      return new HashMap<>();
   }


   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      int quantityPage = (this.currentPage - 1) * 16;
      String url = baseUrlApi + "assembler/pages/Default/osf/catalog?No=" + quantityPage + "&Nrpp=16&Ntt=" + this.keywordEncoded;
      JSONObject jsonObject = fetchJSONObject(url);
      List<RankingProduct> productsRanking = new ArrayList<>();
      if (!jsonObject.isEmpty()) {
         if (this.totalProducts == 0) this.totalProducts = jsonObject.optInt("totalNumRecs", 0);
         JSONArray jsonArray = jsonObject.optJSONArray("records");
         for (Object o : jsonArray) {
            JSONObject recordJson = (JSONObject) o;
            JSONObject product = JSONUtils.getValueRecursive(recordJson, "records.0.attributes", JSONObject.class, new JSONObject());
            productsRanking.add(scrapRankingProduct(product));
         }
         HashMap<String, Boolean> availabilities = getProductAvailabilities(productsRanking);
         for (RankingProduct productRanking : productsRanking) {
            Boolean isAvailable = availabilities.get(productRanking.getInternalId());
            if (isAvailable != null && isAvailable) {
               saveDataProduct(productRanking);
            }
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }

      } else {
         Logging.printLogDebug(logger, session, "No results for: " + this.keywordEncoded);
      }
   }

   private RankingProduct scrapRankingProduct(JSONObject json) throws MalformedProductException {
      String internalId = getFirstElement(json, "sku.listingId");
      String name = getFirstElement(json, "sku.displayName");
      String productUrl = getUrl(name, internalId);
      String imageUrl = hostUrl + getFirstElement(json, "product.primaryFullImageURL");
      Integer priceInCents = getPrice(json);
      return RankingProductBuilder.create()
         .setUrl(productUrl)
         .setInternalId(internalId)
         .setInternalPid(internalId)
         .setImageUrl(imageUrl)
         .setName(name)
         .setPriceInCents(priceInCents)
         .setAvailability(priceInCents != null)
         .build();
   }

   private Integer getPrice(JSONObject object) {
      if (object ==null || object.isEmpty()) return null;
      String priceString = getFirstElement(object, "sku.activePrice");

      return priceString!=null && !priceString.isEmpty() ? CommonMethods.stringPriceToIntegerPrice(priceString, '.', 0) : null;
   }

   private String getUrl(String name, String internalId) {
      String slugName = name.replace(" ", "-").toLowerCase();
      String finalUrl = "/" + internalId + internalId.replace("prod_", "?sku=");
      return hostUrl + slugName + finalUrl;
   }

   private String getFirstElement(JSONObject json, String path) {
      if (!json.isEmpty()) {
         JSONArray element = json.optJSONArray(path);

         return !element.isEmpty() ? element.optString(0, "") : "";
      }
      return "";
   }
}
