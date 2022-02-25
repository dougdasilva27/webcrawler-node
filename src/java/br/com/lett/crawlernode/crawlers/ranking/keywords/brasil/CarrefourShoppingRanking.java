package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilCarrefourFetchUtils;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class CarrefourShoppingRanking extends CrawlerRankingKeywords {

   public CarrefourShoppingRanking(Session session) {
      super(session);
      dataFetcher = new JsoupDataFetcher();
      this.pageSize = 15;
   }

   private static final String HOME_PAGE = "https://www.carrefour.com.br";
   private static final String OPERATION_NAME = "productSearchV3";
   private static final String SHA256 = "6869499be99f20964918e2fe0d1166fdf6c006b1766085db9e5a6bc7c4b957e5";
   private static final String SENDER = "vtex.store-resources@0.x";
   private static final String PROVIDER = "vtex.search-graphql@0.x";

   @Override
   protected void processBeforeFetch() {
      super.processBeforeFetch();

      if (getCep() != null) {
         BasicClientCookie userLocationData = new BasicClientCookie("userLocationData", getCep());
         userLocationData.setPath("/");
         cookies.add(userLocationData);
      }

      if (getLocation() != null) {
         BasicClientCookie vtexSegment = new BasicClientCookie("vtex_segment", getLocation());
         vtexSegment.setPath("/");
         this.cookies.add(vtexSegment);
      }
   }

   protected String getLocation(){
      return session.getOptions().optString("vtex_segment");
   }

   protected String getCep(){
      return session.getOptions().optString("userLocationData");
   }

   protected String getRegionId() {
      JSONObject locationJson = JSONUtils.stringToJson(new String(Base64.getDecoder().decode(getLocation()), StandardCharsets.UTF_8));
      return locationJson.optString("regionId", null);
   }

   protected String buildUrl(String homepage) {
      StringBuilder url = new StringBuilder();
      url.append(homepage);
      url.append("/_v/segment/graphql/v1?");
      url.append("workspace=master");
      url.append("&maxAge=short");
      url.append("&appsEtag=remove");
      url.append("&domain=store");
      url.append("&locale=pt-BR");
      url.append("&__bindingId=3bab9213-2811-4d32-856a-a4baa1b689b5");
      url.append("&operationName=" + OPERATION_NAME);
      url.append("&variables=%7B%7D");       //equals to '{}'
      url.append("&extensions=");

      String encodedKeyword = URLEncoder.encode(this.location, StandardCharsets.UTF_8);
      JSONObject variables = new JSONObject();
      variables.put("hideUnavailableItems", false);
      variables.put("skusFilter", "ALL_AVAILABLE");
      variables.put("simulationBehavior", "only1P");
      variables.put("installmentCriteria", "MAX_WITHOUT_INTEREST");
      variables.put("productOriginVtex", false);
      variables.put("map", "ft");
      variables.put("query", encodedKeyword);
      variables.put("orderBy", "OrderByScoreDESC");
      variables.put("from", this.arrayProducts.size());
      variables.put("to", this.arrayProducts.size() + 15);

      JSONObject selectedFacets = new JSONObject();
      selectedFacets.put("key","ft");
      selectedFacets.put("value", encodedKeyword);

      variables.put("selectedFacets", new JSONArray().put(selectedFacets));
      variables.put("fullText", this.keywordWithoutAccents);
      variables.put("operator", "and");
      variables.put("fuzzy", "0");
//      extensions.put("searchState", null);
      variables.put("facetsBehavior", "dynamic");
      variables.put("categoryTreeBehavior", "default");
      variables.put("withFacets", false);

      String variablesBase64 = Base64.getEncoder().encodeToString(variables.toString().getBytes(StandardCharsets.UTF_8));

      JSONObject persistedQuery = new JSONObject();
      persistedQuery.put("version", 1);
      persistedQuery.put("sha256Hash", SHA256);
      persistedQuery.put("sender", SENDER);
      persistedQuery.put("provider", PROVIDER);

      JSONObject extensions = new JSONObject();
      extensions.put("persistedQuery", persistedQuery);
      extensions.put("variables", variablesBase64);

      String encodedExtensions = URLEncoder.encode(extensions.toString(), StandardCharsets.UTF_8);
      url.append(encodedExtensions);

      return url.toString();
   }

   @Override
   protected void extractProductsFromCurrentPage()  {
      this.log("Página " + this.currentPage);

      String url = buildUrl(HOME_PAGE);

      JSONObject jsonResponse = JSONUtils.stringToJson(BrasilCarrefourFetchUtils.fetchPage(url, getLocation(), getCep(), session));
      JSONArray products = JSONUtils.getValueRecursive(jsonResponse, "data.productSearch.products",  JSONArray.class);

      if (products != null && !products.isEmpty()) {
         for (Object object : products) {

            JSONObject product = (JSONObject) object;
            String productUrl = CrawlerUtils.completeUrl(product.optString("linkText"), "https", HOME_PAGE.replace("https://", "".replace("http://", "")));
            String internalPid = product.optString("productId");
            String name = product.optString("productName");
            String image = scrapImage(product);
            Double priceDouble = JSONUtils.getValueRecursive(product, "priceRange.listPrice.lowPrice", Double.class);
            Integer price = 0;
            if(priceDouble != null) {
               price = ((Double) (priceDouble * 100d)).intValue();
            }

            try {
               RankingProduct rankingProduct = RankingProductBuilder.create()
                  .setInternalPid(internalPid)
                  .setUrl(productUrl)
                  .setName(name)
                  .setImageUrl(image)
                  .setPriceInCents(price)
                  .setAvailability(price != 0)
                  .setPageNumber(this.currentPage)
                  .build();

               saveDataProduct(rankingProduct);
            } catch (MalformedProductException e) {
               this.log(e.getMessage());
            }

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      }
   }

   private String scrapImage(JSONObject product) {
      String image = null;

      JSONArray items = product.optJSONArray("items");
      if(items != null && !items.isEmpty()) {
         JSONObject item = items.optJSONObject(0);

         JSONArray images = item.optJSONArray("images");
         if(images != null && !images.isEmpty()) {
            JSONObject imageJson = images.optJSONObject(0);

            image = imageJson.optString("imageUrl");
         }
      }

      return image;
   }

   private Integer scrapPrice(JSONObject product) {
      Integer price = null;

      JSONArray items = product.optJSONArray("items");
      if(items != null && !items.isEmpty()) {
         JSONObject item = items.optJSONObject(0);

         if(item != null) {
            JSONArray sellers = item.optJSONArray("sellers");
            if(sellers != null && !sellers.isEmpty()) {
               JSONObject seller = sellers.optJSONObject(0);

               Double priceDouble = JSONUtils.getValueRecursive(seller, "commercialOffer.price", Double.class);
               if(priceDouble != null) {
                  price = ((Double) (priceDouble * 100d)).intValue();
               }
            }
         }
      }

      return price;
   }

   @Override
   protected boolean hasNextPage() {
      return ((arrayProducts.size() - 1) % pageSize - currentPage) < 0;
   }
}
