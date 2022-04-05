package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.ApiResponseException;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class GigaCrawlerRanking extends CrawlerRankingKeywords {

   private final Integer API_VERSION = 1;
   private final String SENDER = "vtex.store-resources@0.x";
   private final String PROVIDER = "vtex.search-graphql@0.x";
   private String sha256Hash;

   public GigaCrawlerRanking(Session session) {
      super(session);
      this.sha256Hash = session.getOptions().optString("sha256Hash");
   }

   private String getHomePage() {
      return session.getOptions().optString("homePage");
   }

   private String getVtexSegment() {
      return session.getOptions().optString("vtex_segment");
   }

   private String createVariablesBase64() {
      JSONObject search = new JSONObject();
      search.put("hideUnavailableItems", false);
      search.put("skusFilter", "ALL");
      search.put("simulationBehavior", "default");
      search.put("installmentCriteria", "MAX_WITHOUT_INTEREST");
      search.put("productOriginVtex", false);
      search.put("map", "ft");
      search.put("query", this.keywordWithoutAccents);
      search.put("orderBy", "OrderByScoreDESC");
      search.put("from", arrayProducts.size());
      search.put("to", (arrayProducts.size() + 20) - 1);

      JSONArray selectedFacets = new JSONArray();
      JSONObject facet = new JSONObject();
      facet.put("key", "ft");
      facet.put("value", this.keywordWithoutAccents);
      selectedFacets.put(facet);

      search.put("selectedFacets", selectedFacets);
      search.put("fullText", this.keywordWithoutAccents);
      search.put("operator", "and");
      search.put("fuzzy", "0");
      search.put("facetsBehavior", "Static");
      search.put("categoryTreeBehavior", "default");
      search.put("withFacets", false);

      return Base64.getEncoder().encodeToString(search.toString().getBytes());
   }

   protected JSONObject fetchSearchApi() {
      JSONObject searchApi = new JSONObject();
      StringBuilder url = new StringBuilder();
      url.append(getHomePage() + "_v/segment/graphql/v1?");

      JSONObject extensions = new JSONObject();
      JSONObject persistedQuery = new JSONObject();

      persistedQuery.put("version", API_VERSION);
      persistedQuery.put("sha256Hash", sha256Hash);
      persistedQuery.put("sender", SENDER);
      persistedQuery.put("provider", PROVIDER);

      extensions.put("variables", createVariablesBase64());
      extensions.put("persistedQuery", persistedQuery);

      StringBuilder payload = new StringBuilder();
      payload.append("workspace=master");
      payload.append("&maxAge=short");
      payload.append("&appsEtag=remove");
      payload.append("&domain=store");
      payload.append("&locale=pt-BR");
      payload.append("&operationName=productSearchV3");
      payload.append("&variables=" + URLEncoder.encode("{}", StandardCharsets.UTF_8));
      payload.append("&extensions=" + URLEncoder.encode(extensions.toString(), StandardCharsets.UTF_8));
      url.append(payload);

      List<Cookie> cookies = new ArrayList<Cookie>();

      BasicClientCookie cookie = new BasicClientCookie("vtex_segment", getVtexSegment());
      cookie.setDomain("www.giga.com.vc");
      cookie.setPath("/");
      this.cookies.add(cookie);

      log("Link onde são feitos os crawlers:" + url);

      Request request = Request.RequestBuilder.create()
         .setUrl(url.toString())
         .setCookies(cookies)
         .setPayload(payload.toString())
         .build();

      JSONObject response = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
      try {
         if (!response.has("errors")) {
            searchApi = JSONUtils.getValueRecursive(response, "data.productSearch", JSONObject.class, new JSONObject());
         } else {
            throw new ApiResponseException(response.toString());
         }
      } catch (Exception ex) {
         Logging.printLogError(logger, session, CommonMethods.getStackTrace(ex));
      }

      return searchApi;
   }


   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);
      this.pageSize = 20;

      JSONObject searchApi = fetchSearchApi();
      JSONArray products = JSONUtils.getJSONArrayValue(searchApi, "products");

      if (products.length() > 0) {

         if (this.totalProducts == 0) {
            setTotalProducts(searchApi);
         }

         for (Object object : products) {
            JSONObject product = (JSONObject) object;
            String productUrl = CrawlerUtils.completeUrl(product.optString("linkText") + "/p", "https",
               getHomePage().replace("https://", "").replace("/", ""));
            String internalPid = product.optString("productId");
            String internalId = JSONUtils.getValueRecursive(product, "items.0.itemId", String.class);
            String name = product.optString("productName");

            JSONObject itemData = (JSONObject) product.optQuery("/items/0");
            String image = null;
            int priceInCents = 0;
            boolean isAvailable = false;

            if(itemData != null) {
               JSONArray images = itemData.optJSONArray("images");
               JSONArray sellers = itemData.optJSONArray("sellers");
               image = crawlImage(images);

               if(sellers != null && sellers.length() > 0) {
                  JSONObject seller = sellers.optJSONObject(0);
                  JSONObject commertialOffer = seller.optJSONObject("commertialOffer");

                  priceInCents = crawlPrice(commertialOffer);
                  isAvailable = commertialOffer.optInt("AvailableQuantity", 0) > 0;
               }
            }

            try {
               RankingProduct rankingProduct = RankingProductBuilder.create()
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setUrl(productUrl)
                  .setName(name)
                  .setImageUrl(image)
                  .setPriceInCents(priceInCents != 0 ? priceInCents : null)
                  .setAvailability(isAvailable)
                  .build();

               saveDataProduct(rankingProduct);
            } catch (MalformedProductException e) {
               this.log(e.getMessage());
            }

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

   private int crawlPrice(JSONObject commertialOffer) {
      int priceInCents = 0;

      if(commertialOffer != null) {
         Object price = commertialOffer.opt("Price");
         if (price instanceof Double) {
            priceInCents = (int) Math.round((Double) price * 100);
         } else if (price instanceof Integer) {
            priceInCents = (int) price * 100;
         }
      }
      return priceInCents;
   }

   private String crawlImage(JSONArray images) {
      if (images != null && images.length() > 0) {
         JSONObject image = images.optJSONObject(0);
         if(image != null) {
            return image.optString("imageUrl");
         }
      }
      return null;
   }

   protected void setTotalProducts(JSONObject data) {
      this.totalProducts = CrawlerUtils.getIntegerValueFromJSON(data, "total", 0);
      this.log("Total da busca: " + this.totalProducts);
   }

   @Override
   protected boolean hasNextPage(){
      return true;
   }
}
