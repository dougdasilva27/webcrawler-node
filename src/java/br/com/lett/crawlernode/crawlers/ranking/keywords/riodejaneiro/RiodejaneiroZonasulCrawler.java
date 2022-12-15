package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.VTEXGraphQLRanking;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class RiodejaneiroZonasulCrawler extends VTEXGraphQLRanking {

   public RiodejaneiroZonasulCrawler(Session session) {
      super(session);
   }

   @Override
   protected String crawInternalPid(JSONObject product) {
      return null;
   }

   private String HOME_PAGE = getHomePage();

   private String getHomePage() {
      return session.getOptions().optString("homePage");
   }

   @Override
   protected JSONObject fetchSearchApi(Document doc, String redirect) {
      JSONObject searchApi = new JSONObject();
      StringBuilder url = new StringBuilder();

      String sha256Hash = getSha256Hash(doc);
      url.append(HOME_PAGE).append("_v/segment/graphql/v1?");
      JSONObject extensions = new JSONObject();
      JSONObject persistedQuery = new JSONObject();

      persistedQuery.put("version", "1");
      persistedQuery.put("sha256Hash", sha256Hash);
      persistedQuery.put("sender", "vtex.store-resources@0.x");
      persistedQuery.put("provider", "vtex.search-graphql@0.x");

      extensions.put("variables", createVariablesBase64(redirect));
      extensions.put("persistedQuery", persistedQuery);

      url.append("extensions=").append(URLEncoder.encode(extensions.toString(), StandardCharsets.UTF_8));
      this.log("Link onde são feitos os crawlers:" + url);

      Request request = Request.RequestBuilder.create()
         .setUrl(url.toString())
         .setCookies(cookies)
         .build();

      JSONObject response = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

      if (response.has("data") && !response.isNull("data")) {
         JSONObject data = response.optJSONObject("data");

         if (data.has("productSearch") && !data.isNull("productSearch")) {
            searchApi = data.optJSONObject("productSearch");

            if (searchApi.optString("redirect") != null && !searchApi.optString("redirect").isEmpty()) {
               redirect = searchApi.optString("redirect");
               searchApi = fetchSearchApi(doc, redirect);
            }
         }
      }

      if (searchApi.has("products") && searchApi.optJSONArray("products").length() == 0) {
         String categoryId = getCategoryId(redirect);
         searchApi = fetchSearchApiCategory(categoryId, sha256Hash);
      }

      return searchApi;
   }

   private String getCategoryId(String redirect) {
      String category = null;
      if(redirect!=null && !redirect.isEmpty()){
         String[] split = redirect.split("/");
         category = split[1];
      }
      else{
         category = this.location;
      }
      String url = "https://www.zonasul.com.br/" + category + "?__pickRuntime=appsEtag%2Cblocks%2CblocksTree%2Ccomponents%2CcontentMap%2Cextensions%2Cmessages%2Cpage%2Cpages%2Cquery%2CqueryData%2Croute%2CruntimeMeta%2Csettings";
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .build();

      JSONObject response = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

      if (response.has("extensions") && !response.isNull("extensions")) {
         JSONObject extensions = response.optJSONObject("extensions");
         String categoryId = JSONUtils.getValueRecursive(extensions, "vtex.store@2.x:store.custom#" + category + "/search-result-layout.customQuery#coleção!content!querySchema!queryField", "!", String.class, null);
         if (categoryId != null && !categoryId.isEmpty()) {
            return categoryId;
         }
      }
      return null;
   }

   private JSONObject fetchSearchApiCategory(String categoryId, String sha256Hash) {
      JSONObject searchApi = new JSONObject();
      StringBuilder url = new StringBuilder();

      url.append(HOME_PAGE).append("_v/segment/graphql/v1?");
      JSONObject extensions = new JSONObject();
      JSONObject persistedQuery = new JSONObject();

      persistedQuery.put("version", "1");
      persistedQuery.put("sha256Hash", sha256Hash);
      persistedQuery.put("sender", "vtex.store-resources@0.x");
      persistedQuery.put("provider", "vtex.search-graphql@0.x");

      extensions.put("variables", createVariablesBase64CategoryId(categoryId));
      extensions.put("persistedQuery", persistedQuery);

      url.append("extensions=").append(URLEncoder.encode(extensions.toString(), StandardCharsets.UTF_8));
      this.log("Link onde são feitos os crawlers:" + url);

      Request request = Request.RequestBuilder.create()
         .setUrl(url.toString())
         .setCookies(cookies)
         .build();

      JSONObject response = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

      if (response.has("data") && !response.isNull("data")) {
         JSONObject data = response.optJSONObject("data");

         if (data.has("productSearch") && !data.isNull("productSearch")) {
            searchApi = data.optJSONObject("productSearch");
         }
      }
      return searchApi;
   }

   protected String createVariablesBase64CategoryId(String categoryId) {
      JSONObject variables = new JSONObject();
      variables.put("hideUnavailableItems", false);
      variables.put("skusFilter", "ALL_AVAILABLE");
      variables.put("simulationBehavior", "default");
      variables.put("installmentCriteria", "MAX_WITHOUT_INTEREST");
      variables.put("productOriginVtex", true);
      variables.put("map", "productClusterIds");
      variables.put("orderBy", "OrderByTopSaleDESC");
      variables.put("facetsBehavior", "Static");
      variables.put("withFacets", false);

      variables.put("from", arrayProducts.size());
      variables.put("to", (arrayProducts.size() + this.pageSize) - 1);
      variables.put("query", categoryId);

      JSONArray selectedFacets = new JSONArray();
      JSONObject obj = new JSONObject();
      obj.put("key", "productClusterIds");
      obj.put("value", categoryId);
      selectedFacets.put(obj);
      variables.put("selectedFacets", selectedFacets);

      return Base64.getEncoder().encodeToString(variables.toString().getBytes());
   }
}
