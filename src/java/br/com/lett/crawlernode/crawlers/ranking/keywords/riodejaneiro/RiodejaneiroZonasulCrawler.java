package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.models.RankingProducts;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.ApiResponseException;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class RiodejaneiroZonasulCrawler extends CrawlerRankingKeywords {

   private static final Integer API_VERSION = 1;
   private static final String SENDER = "vtex.store-resources@0.x";
   private static final String PROVIDER = "vtex.search-graphql@0.x";
   private static final String keySHA256 = "e1d5228ca8b18d1f83b777d849747a7f55c9597f670f20c8557d2b0428bf6be7";

   public RiodejaneiroZonasulCrawler(Session session) {
      super(session);
   }

   protected String getHomePage() {
      return session.getOptions().getString("home_page");
   }

   protected String getVtexSegment() {
      return session.getOptions().getString("vtex_segment");
   }

   protected String createVariablesBase64() {
      JSONObject search = new JSONObject();
      JSONObject key = new JSONObject();
      JSONArray facets = new JSONArray();
      search.put("hideUnavailableItems", true);
      search.put("skusFilter", "ALL");
      search.put("simulationBehavior", "default");
      search.put("installmentCriteria", "MAX_WITHOUT_INTEREST");
      search.put("productOriginVtex", true);
      search.put("map", "ft");
      search.put("query", this.keywordWithoutAccents.replace(" ", "%20"));
      search.put("orderBy", "OrderByScoreDESC");
      search.put("from", arrayProducts.size());
      search.put("to", arrayProducts.size() + 47);
      search.put("facetsBehavior", "Static");
      search.put("categoryTreeBehavior", "default");
      search.put("withFacets", false);
      search.put("operator", "and");
      search.put("fuzzy", "0");
      search.put("fullText", this.keywordWithoutAccents.replace(" ", "%20"));
      key.put("key", "ft");
      key.put("value", this.keywordWithoutAccents.replace(" ", "%20"));
      facets.put(key);
      search.put("selectedFacets", facets);

      return Base64.getEncoder().encodeToString(search.toString().getBytes());
   }

   protected JSONObject fetchSearchApi() {
      JSONObject searchApi = new JSONObject();
      StringBuilder url = new StringBuilder();
      url.append(getHomePage() + "_v/segment/graphql/v1?");

      JSONObject extensions = new JSONObject();
      JSONObject persistedQuery = new JSONObject();

      persistedQuery.put("version", API_VERSION);
      persistedQuery.put("sha256Hash", keySHA256);
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


   protected void extractProductsFromCurrentPage() throws MalformedProductException {
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
            String name = product.optString("productName");
            String imageUrl = JSONUtils.getValueRecursive(product, "items.0.images.0.imageUrl", String.class);
            int price = getPrice(product);
            boolean isAvailable = price != 0;

            //New way to send products to save data product
            RankingProducts productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(null)
               .setInternalPid(internalPid)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
               .build();

            saveDataProduct(productRanking);

            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);

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

   protected void setTotalProducts(JSONObject data) {
      this.totalProducts = CrawlerUtils.getIntegerValueFromJSON(data, "total", 0);
      this.log("Total da busca: " + this.totalProducts);
   }

   private int getPrice(JSONObject product) {
      int priceCents = 0;
      Double price = JSONUtils.getValueRecursive(product, "priceRange.sellingPrice.lowPrice", Double.class);
      String text = price.toString().replaceAll("[^0-9]", "");

      if (!text.isEmpty()) {
         priceCents = Integer.parseInt(text);
      }
      return priceCents;
   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }
}

