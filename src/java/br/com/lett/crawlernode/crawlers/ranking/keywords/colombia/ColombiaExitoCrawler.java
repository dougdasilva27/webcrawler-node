package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.ApiResponseException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ColombiaExitoCrawler extends CrawlerRankingKeywords {

   private static final Integer API_VERSION = 1;
   private static final String SENDER = "vtex.store-resources@0.x";
   private static final String PROVIDER = "vtex.search-graphql@0.x";
   private static final String keySHA256 = "e1d5228ca8b18d1f83b777d849747a7f55c9597f670f20c8557d2b0428bf6be7";

   public ColombiaExitoCrawler(Session session) {
      super(session);
   }

   protected String getHomePage() {
      return session.getOptions().getString("homePage");
   }

   protected String getVtexSegment() {
      return session.getOptions().getString("vtex_segment");
   }

   protected String createVariablesBase64() {
      JSONObject search = new JSONObject();
      search.put("hideUnavailableItems", true);
      search.put("skusFilter", "ALL_AVAILABLE");
      search.put("simulationBehavior", "default");
      search.put("installmentCriteria", "MAX_WITHOUT_INTEREST");
      search.put("productOriginVtex", false);
      search.put("map", "ft");
      search.put("query", this.keywordWithoutAccents.replace(" ", "%20"));
      search.put("orderBy", "OrderByScoreDESC");
      search.put("from", arrayProducts.size());
      search.put("to", arrayProducts.size() + 20);
      search.put("facetsBehavior", "dynamic");
      search.put("categoryTreeBehavior", "default");
      search.put("withFacets", false);
      search.put("fullText", this.keywordWithoutAccents);

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
      payload.append("&locale=es-CO");
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

            saveDataProduct(internalId, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);

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

   @Override
   protected boolean hasNextPage(){
      return true;
   }
}
