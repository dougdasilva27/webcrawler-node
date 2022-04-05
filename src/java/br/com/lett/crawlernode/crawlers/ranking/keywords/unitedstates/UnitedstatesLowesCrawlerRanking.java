package br.com.lett.crawlernode.crawlers.ranking.keywords.unitedstates;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
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
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class UnitedstatesLowesCrawlerRanking extends CrawlerRankingKeywords {
   private String storeId = getStoreId();
   private String region = getRegion();

   private String getStoreId() {
      return session.getOptions().optString("storeId");
   }

   private String getRegion() {
      return session.getOptions().optString("region");
   }

   public UnitedstatesLowesCrawlerRanking(Session session) {
      super(session);
      super.fetchMode = FetchMode.JSOUP;
   }

   @Override
   protected void processBeforeFetch() {
      BasicClientCookie storeIdCookie = new BasicClientCookie("sn", storeId);
      storeIdCookie.setDomain("www.lowes.com");
      storeIdCookie.setPath("/");
      this.cookies.add(storeIdCookie);

      BasicClientCookie regionCookie = new BasicClientCookie("region", region);
      regionCookie.setDomain("www.lowes.com");
      regionCookie.setPath("/");
      this.cookies.add(regionCookie);
   }

   @Override
   protected Document fetchDocument(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("authority", "www.lowes.com");
      headers.put("accept", "application/json, text/plain, */*");
      headers.put("referer", session.getOriginalURL());

      Request request = Request.RequestBuilder.create()
         .setProxyservice(Arrays.asList(ProxyCollection.NETNUT_RESIDENTIAL_US_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_UK, ProxyCollection.NETNUT_RESIDENTIAL_US, ProxyCollection.BUY_HAPROXY))
         .setUrl(url)
         .setSendUserAgent(true)
         .setHeaders(headers)
         .setCookies(cookies)
         .setFollowRedirects(true)
         .build();
      Response response = dataFetcher.get(session, request);

      String content = response.getBody().replace("\\", "");

      return Jsoup.parse(content);
   }

   @Override
   public void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 24;
      this.log("Página " + this.currentPage);

      String url = "https://www.lowes.com/search?searchTerm=" + this.keywordEncoded + "&offset=" + (this.pageSize * (this.currentPage - 1));
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, "[data-selector='splp-prd-lst-ttl']", false, 0);

      JSONArray products = getProductsJSON(this.currentDoc);

      if (!products.isEmpty()) {
         for (Object productObj : products) {
            if (productObj instanceof JSONObject) {
               JSONObject productJSON = (JSONObject) productObj;

               JSONObject product = productJSON.optJSONObject("product");
               JSONObject location = productJSON.optJSONObject("location");

               String internalPid = product.optString("omniItemId");
               String productUrl = CrawlerUtils.completeUrl(product.optString("pdURL"), "https", "www.lowes.com");
               String name = product.optString("description");
               String imgUrl = CrawlerUtils.completeUrl(product.optString("imageUrl"), "https", "mobileimages.lowes.com");
               Integer price = scrapPrice(location);
               boolean isAvailable = scrapAvailability(location);
               boolean sponsored = product.optBoolean("sponsored");

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(null)
                  .setInternalPid(internalPid)
                  .setImageUrl(imgUrl)
                  .setName(name)
                  .setPriceInCents(price)
                  .setAvailability(isAvailable)
                  .setIsSponsored(sponsored)
                  .build();

               saveDataProduct(productRanking);
            }
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultados!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

   public JSONArray getProductsJSON(Document doc) throws JSONException, ArrayIndexOutOfBoundsException, IllegalArgumentException, UnsupportedEncodingException {
      JSONArray products = new JSONArray();
      Elements scripts = doc.select("body > script");
      String jsonScript = "";

      for (Element e : scripts) {
         String script = e.html();
         if (script.contains("window['__PRELOADED_STATE__']")) {
            jsonScript = CrawlerUtils.extractSpecificStringFromScript(script, "window['__PRELOADED_STATE__'] = \"", true, "}", true) + "}";
            break;
         }
      }

      JSONArray itemList = getJSONArrayFromMatch(jsonScript, "itemList\":(.*)?,\"breadcrumbs\"");
      JSONArray newItemList = getJSONArrayFromMatch(jsonScript, "newItemList\":(.*)?,\"ABTConfig\"");

      if (itemList != null && !itemList.isEmpty()) {
         for (int i = 0; i < itemList.length(); i++) {
            products.put(itemList.getJSONObject(i));
         }
      }

      if (newItemList != null && !newItemList.isEmpty()) {
         for (int i = 0; i < newItemList.length(); i++) {
            products.put(newItemList.getJSONObject(i));
         }
      }

      return products;
   }

   private JSONArray getJSONArrayFromMatch(String script, String regex) {
      String json = null;
      Pattern pattern = Pattern.compile(regex);
      Matcher matcher = pattern.matcher(script);
      if (matcher.find()) {
         json = matcher.group(1);
      }

      return JSONUtils.stringToJsonArray(json);
   }

   private Integer scrapPrice(JSONObject location) {
      int price = 0;

      if (location != null && location.has("price")) {
         JSONObject priceJSON = location.optJSONObject("price");
         if (priceJSON.has("minPrice")) {
            price = JSONUtils.getPriceInCents(priceJSON, "minPrice");
         } else {
            price = JSONUtils.getPriceInCents(priceJSON, "sellingPrice");
         }
      }

      return price != 0 ? price : null;
   }

   private boolean scrapAvailability(JSONObject location) {
      boolean isAvailable = false;

      if (location != null && location.has("itemInventory")) {
         JSONArray itemAvailList = (JSONArray) location.optQuery("/itemInventory/itemAvailList");
         isAvailable = IntStream
            .range(0, itemAvailList.length())
            .mapToObj(itemAvailList::optJSONObject)
            .anyMatch(json -> json.optBoolean("isAvlSts"));
      } else if (location != null && location.has("locator")) {
         JSONObject locator = location.optJSONObject("locator");
         isAvailable = locator.optBoolean("lookupStatus");
      }

      return isAvailable;
   }
}
