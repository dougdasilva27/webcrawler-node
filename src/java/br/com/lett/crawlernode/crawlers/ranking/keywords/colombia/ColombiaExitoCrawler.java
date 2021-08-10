package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;

public class ColombiaExitoCrawler extends CrawlerRankingKeywords {

   private static final Integer API_VERSION = 1;
   private static final String SENDER = "vtex.search@0.x";
   private static final String PROVIDER = "vtex.search@0.x";
   private String keySHA256 = "ab6840a1cadd874965fd737f2568bf4c3648944cd343e3da94db8e8e650dd5c2";

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
      search.put("count", "20");
      search.put("sort", "");
      search.put("attributePath", "");
      search.put("page", this.currentPage);
      search.put("productOriginVtex", "VTEX");
      search.put("indexingType", "API");
      search.put("query", keywordEncoded);
      search.put("leap", false);

      return Base64.getEncoder().encodeToString(search.toString().getBytes());
   }

   protected JSONObject fetchSearchApi() {
      JSONObject searchApi;
      StringBuilder url = new StringBuilder();
      url.append(getHomePage() + "_v/segment/graphql/v1?");

      JSONObject extensions = new JSONObject();
      JSONObject persistedQuery = new JSONObject();

      persistedQuery.put("version", API_VERSION);
      persistedQuery.put("sha256Hash", this.keySHA256);
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
      payload.append("&operationName=searchResult");
      try {
         payload.append("&variables=" + URLEncoder.encode("{}", "UTF-8"));
         payload.append("&extensions=" + URLEncoder.encode(extensions.toString(), "UTF-8"));
      } catch (UnsupportedEncodingException e) {
         Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      }
      url.append(payload);

      log("Link onde são feitos os crawlers:" + url);

      Request request = Request.RequestBuilder.create()
         .setUrl(url.toString())
         .setCookies(cookies)
         .setPayload(payload.toString())
         .build();

      JSONObject response = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

      searchApi = JSONUtils.getValueRecursive(response, "data.searchResult", JSONObject.class, new JSONObject());

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
}
