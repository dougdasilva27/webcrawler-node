package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public abstract class CarrefourCrawler extends CrawlerRankingKeywords {

   public CarrefourCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   protected String locale = "pt-BR";

   private String hash;

   @Override
   protected void processBeforeFetch() {
      super.processBeforeFetch();

      BasicClientCookie cookie = new BasicClientCookie("vtex_segment", getLocation());
      try {
         cookie.setDomain(new URL(getHomePage()).getHost());
      } catch (MalformedURLException e) {
         throw new IllegalStateException(e);
      }
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   protected abstract String getHomePage();

   protected abstract String getLocation();

   private void setTotalProducts(JSONObject data) {
      if (data.has("recordsFiltered") && data.get("recordsFiltered") instanceof Integer) {
         this.totalProducts = data.getInt("recordsFiltered");
         this.log("Total da busca: " + this.totalProducts);
      }
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);
      this.pageSize = 24;

      String url = getHomePage() + this.keywordEncoded + "?_q=" + this.keywordEncoded + "&map=ft&page=" + this.currentPage;

      Map<String, String> headers = new HashMap<>();

      Request request = Request.RequestBuilder.create().setUrl(url.replace("+", "%20")).setCookies(cookies).setHeaders(headers).build();
      String response = this.dataFetcher.get(session, request).getBody();

      JSONObject searchApi = fetchSearchApi(response);
      JSONArray products = searchApi != null && searchApi.has("products") ? searchApi.optJSONArray("products") : new JSONArray();

      if (products.length() > 0) {

         if (this.totalProducts == 0) {
            setTotalProducts(searchApi);
         }

         for (Object object : products) {
            JSONObject product = (JSONObject) object;
            try {
               String productUrl = new URIBuilder(getHomePage()).setPath(product.optString("linkText") + "/p").build().toString();
               String internalPid = product.optString("productId");
               saveDataProduct(null, internalPid, productUrl);
               this.log("Position: " + this.position + " - InternalPid: " + internalPid + " - Url: " + productUrl);
            } catch (URISyntaxException e) {
               logger.error("Error get url", e);
            }

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }

      }
   }

   private String fetchSHA256Key(String response) {

      if (hash != null) {
         return hash;
      }

      if (response != null && !response.isEmpty()) {
         Document doc = Jsoup.parse(response);
         String nonFormattedJson = doc.selectFirst("template[data-varname=__STATE__] script").html();
         JSONObject stateJson = CrawlerUtils.stringToJson(nonFormattedJson);

         for (String key : stateJson.keySet()) {
            String firstIndexString = "@runtimeMeta(";
            String keyIdentifier = ").correction";

            if (key.contains(firstIndexString) && key.contains(keyIdentifier)) {
               int x = key.indexOf(firstIndexString) + firstIndexString.length();
               int y = key.indexOf(')', x);

               JSONObject hashJson = CrawlerUtils.stringToJson(key.substring(x, y).replace("\\\"", "\""));
               if (hashJson.has("hash") && !hashJson.isNull("hash")) {
                  hash = hashJson.get("hash").toString();
               }

               break;
            }
         }
      }
      return hash;
   }

   private String createVariablesBase64() {
      JSONObject search = new JSONObject();
      search.put("hideUnavailableItems", false);
      search.put("skusFilter", "ALL");
      search.put("simulationBehavior", "default");
      search.put("installmentCriteria", "MAX_WITHOUT_INTEREST");
      search.put("productOriginVtex", true);
      search.put("map", "ft");
      search.put("query", keywordEncoded);
      search.put("orderBy", "");
      search.put("from", this.arrayProducts.size());
      search.put("to", this.arrayProducts.size() + (this.pageSize - 1));

      JSONArray selectedFacets = new JSONArray();
      JSONObject obj = new JSONObject();
      obj.put("key", "ft");
      obj.put("value", this.keywordEncoded);

      selectedFacets.put(obj);

      search.put("selectedFacets", selectedFacets);
      search.put("fullText", this.location);
      search.put("operator", "and");
      search.put("fuzzy", "0");
      search.put("facetsBehavior", "Static");
      search.put("searchState", JSONObject.NULL);
      search.put("withFacets", false);

      return Base64.getEncoder().encodeToString(search.toString().getBytes());
   }

   private JSONObject fetchSearchApi(String response) {
      JSONObject searchApi = new JSONObject();

      StringBuilder url = new StringBuilder();
      url.append(getHomePage()).append("_v/segment/graphql/v1?")
         .append("workspace=master")
         .append("&maxAge=medium")
         .append("&appsEtag=remove")
         .append("&domain=store")
         .append("&locale=").append(locale)
         .append("&operationName=productSearchV3")
         .append("&variables=%7B%7D");

      JSONObject extensions = new JSONObject();
      JSONObject persistedQuery = new JSONObject();

      persistedQuery.put("version", "1");
      persistedQuery.put("sha256Hash", fetchSHA256Key(response));

      persistedQuery.put("sender", "vtex.store-resources@0.x");
      persistedQuery.put("provider", "vtex.search-graphql@0.x");

      extensions.put("variables", createVariablesBase64());
      extensions.put("persistedQuery", persistedQuery);

      StringBuilder payload = new StringBuilder();
      try {
         payload.append("&variables=").append(URLEncoder.encode("{}", "UTF-8"));
         payload.append("&extensions=").append(URLEncoder.encode(extensions.toString(), "UTF-8"));
      } catch (UnsupportedEncodingException e) {
         Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      }

      url.append(payload);


      Map<String, String> headers = new HashMap<>();

      Request request = Request.RequestBuilder.create().setUrl(url.toString()).setCookies(cookies).setHeaders(headers).build();
      JSONObject resp = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

      if (resp.has("data")) {
         JSONObject data = resp.optJSONObject("data");

         if (data != null && data.has("productSearch")) {
            searchApi = data.optJSONObject("productSearch");
         }
      }

      return searchApi;
   }

}
