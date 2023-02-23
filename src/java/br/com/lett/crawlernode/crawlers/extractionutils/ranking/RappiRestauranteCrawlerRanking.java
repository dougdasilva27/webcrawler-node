package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class RappiRestauranteCrawlerRanking extends CrawlerRankingKeywords {

   public RappiRestauranteCrawlerRanking(Session session) {
      super(session);
   }

   protected String getStoreId(){
      return session.getOptions().optString("storeId");
   }

   private String getCurrentLocation() {
      return session.getOptions().optString("currentLocation");
   }

   protected abstract String getApiDomain();

   protected abstract String getProductDomain();

   protected abstract String getMarketBaseUrl();

   protected abstract String getImagePrefix();

   @Override
   protected void processBeforeFetch() {
      BasicClientCookie cookie = new BasicClientCookie("currentLocation", getCurrentLocation());
      cookie.setDomain(".www." + getProductDomain());
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   private List<JSONObject> getAllPageProducts() {
      List<JSONObject> allProducts = new ArrayList<>();

      JSONObject pageJson = CrawlerUtils.selectJsonFromHtml(this.currentDoc, "#__NEXT_DATA__", null, null, false, false);

      JSONArray corridors = new JSONArray();
      JSONObject fallback = JSONUtils.getValueRecursive(pageJson, "props.pageProps.fallback", JSONObject.class, new JSONObject());
      if (!fallback.isEmpty()) {
         Iterator<String> keys = fallback.keys();
         while(keys.hasNext()) {
            String key = keys.next();
            if (fallback.get(key) instanceof JSONObject) {
               corridors = JSONUtils.getValueRecursive(fallback.get(key), "corridors", JSONArray.class, new JSONArray());
               break;
            }
         }
      }

      for (Object corridor : corridors) {
         JSONObject jsonCorridor = (JSONObject) corridor;
         JSONArray products = jsonCorridor.optJSONArray("products");
         for (Object product : products) {
            JSONObject jsonProduct = (JSONObject) product;
            allProducts.add(jsonProduct);
         }
      }

      return allProducts;
   }

   private boolean isSearchProduct(JSONObject product) {
      String keywordSearch = this.keywordWithoutAccents.replace(" ", "").toLowerCase(Locale.ROOT);

      String nameWithoutSpaces = product.optString("name").replace(" ", "").toLowerCase(Locale.ROOT);
      String descriptionWithoutSpaces = product.optString("description").replace(" ", "").toLowerCase(Locale.ROOT);

      return CommonMethods.removeAccents(nameWithoutSpaces).contains(keywordSearch) || CommonMethods.removeAccents(descriptionWithoutSpaces).contains(keywordSearch);
   }

   private List<JSONObject> getSearchProducts() {
      List<JSONObject> searchProducts = new ArrayList<>();

      List<JSONObject> allProducts = getAllPageProducts();

      // populate popular products
      for (JSONObject product : allProducts) {
         if (isSearchProduct(product)) {
            if (!product.optBoolean("popular")) {
               continue;
            }
            searchProducts.add(product);
         }
      }

      // populate search products
      for (JSONObject product : allProducts) {
         if (isSearchProduct(product)) {
            searchProducts.add(product);
         }
      }

      return searchProducts;
   }

   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {

      this.log("Página " + this.currentPage);

      String marketUrl = getMarketBaseUrl() + getStoreId();
      this.currentDoc = fetchDocument(marketUrl);
      JSONObject pageJson = CrawlerUtils.selectJsonFromHtml(this.currentDoc, "#__NEXT_DATA__", null, null, false, false);

      String storeIdAndSlug = JSONUtils.getValueRecursive(pageJson, "props.pageProps.storeIdQueryParam", ".", String.class, "");

      List<JSONObject> products = getSearchProducts();

      if (!products.isEmpty()) {
         for (JSONObject product : products) {
            String internalId = product.optString("productId");
            String url = getMarketBaseUrl() + storeIdAndSlug + "?productDetail=" + internalId;
            String name = product.optString("name");
            boolean isAvailable = product.optBoolean("isAvailable");
            Integer priceInCents = isAvailable ? scrapPrice(product) : null;
            String imageUrl = product.optString("image");

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(url)
               .setInternalId(internalId)
               .setName(name)
               .setPriceInCents(priceInCents)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
               .build();


            saveDataProduct(productRanking);


            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private Integer scrapPrice(JSONObject product) {
      double price = product.optDouble("priceNumber");
      Integer priceInCents = null;
      if (price != 0.0) {
         priceInCents = Integer.parseInt(Double.toString(price).replace(".", ""));
      }
      return priceInCents;
   }


}
