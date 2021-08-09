package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JavanetDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.RequestsStatistics;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public abstract class VTEXRankingKeywords extends CrawlerRankingKeywords {

   public VTEXRankingKeywords(Session session) {
      super(session);
   }

   private static final Integer API_VERSION = 1;
   private static final String SENDER = "vtex.store-resources@0.x";
   private static final String PROVIDER = "vtex.search-graphql@0.x";

   private String keySHA256 = "7ebc11bc7b0b6a33043102ea1f0a454d556efb88153a3ab7800a979d069ce789";
   private String vtex_segment = getVtexSegment();

   @Override
   protected void processBeforeFetch() {
      super.processBeforeFetch();

      if (!vtex_segment.equals("")) {
         BasicClientCookie cookie = new BasicClientCookie("vtex_segment", vtex_segment);
         cookie.setDomain(getHomePage().replace("https://", "").replace("/", ""));
         cookie.setPath("/");
         this.cookies.add(cookie);
      }

      BasicClientCookie cookie = new BasicClientCookie("userLocationData", getLocation());
      cookie.setDomain(getHomePage().replace("https://", "").replace("/", ""));
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   protected String fetchPage(String url) {

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setFetcheroptions(
            FetcherOptions.FetcherOptionsBuilder.create()
               .mustUseMovingAverage(false)
               .mustRetrieveStatistics(true)
               .build())
         .setProxyservice(Arrays.asList(
            ProxyCollection.INFATICA_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.LUMINATI_SERVER_BR))
         .build();

      int attempts = 0;
      Response response = this.dataFetcher.get(session, request);
      String body = response.getBody();

      Integer statusCode = 0;
      List<RequestsStatistics> requestsStatistics = response.getRequests();
      if (!requestsStatistics.isEmpty()) {
         statusCode = requestsStatistics.get(requestsStatistics.size() - 1).getStatusCode();
      }

      boolean retry = statusCode == null ||
         (Integer.toString(statusCode).charAt(0) != '2'
            && Integer.toString(statusCode).charAt(0) != '3'
            && statusCode != 404);

      // If fetcher don't return the expected response we try with apache
      // If apache do the same, we try with javanet
      if (retry) {
         do {
            if (attempts == 0) {
               body = new ApacheDataFetcher().get(session, request).getBody();
            } else if (attempts == 1) {
               body = new JavanetDataFetcher().get(session, request).getBody();
            }

            attempts++;
         } while (attempts < 2 && (body == null || body.isEmpty()));
      }

      return body;
   }

   protected Document fetchDocument() {

      StringBuilder searchPage = new StringBuilder();

      searchPage.append(getHomePage())
         .append("busca/")
         .append(this.keywordEncoded)
         .append("?page=")
         .append(this.currentPage);

      String apiUrl = searchPage.toString().replace("+", "%20");


      return Jsoup.parse(fetchPage(apiUrl));
   }

   private void scrapHashCode() {
      JSONObject runtimeJson = new JSONObject();

      Element nonFormattedJson = this.currentDoc.selectFirst("template[data-varname=__STATE__] script");

      if (nonFormattedJson != null) {
         runtimeJson = CrawlerUtils.stringToJson(nonFormattedJson.html());
      }

      if (runtimeJson != null) {

         for (String e : runtimeJson.keySet()) {

            if (e.contains("$ROOT_QUERY.productSearch")) {

               String[] splited = e.split("hash\\\":\\\"");

               if (splited.length > 0) {

                  String[] result = splited[1].split("\\\"");

                  if (result.length > 0) {
                     this.keySHA256 = result[0];
                  }
               }
            }
         }
      }
   }

   protected abstract String getHomePage();

   protected abstract String getLocation();

   protected String getVtexSegment() {
      return "";
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);
      this.pageSize = 50;

      if (this.currentPage == 1) {
         this.currentDoc = fetchDocument();
         scrapHashCode();
      }

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

            saveDataProduct(internalPid, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalPid + " - InternalPid: " + internalPid + " - Url: " + productUrl);

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
      this.totalProducts = CrawlerUtils.getIntegerValueFromJSON(data, "recordsFiltered", 0);
      this.log("Total da busca: " + this.totalProducts);
   }

   /**
    * This function request a api with a JSON encoded on BASE64
    *
    * @return
    */
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
      payload.append("&locale=pt-BR");
      payload.append("&operationName=productSearchV3");
      try {
         payload.append("&variables=" + URLEncoder.encode("{}", "UTF-8"));
         payload.append("&extensions=" + URLEncoder.encode(extensions.toString(), "UTF-8"));
      } catch (UnsupportedEncodingException e) {
         Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      }
      url.append(payload.toString());

      log("Link onde são feitos os crawlers:" + url);

      Request request = Request.RequestBuilder.create()
         .setUrl(url.toString())
         .setCookies(cookies)
         .setPayload(payload.toString())
         .build();

      JSONObject response = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

      searchApi = JSONUtils.getValueRecursive(response, "data.productSearch", JSONObject.class, new JSONObject());

      return searchApi;
   }

   protected String createVariablesBase64() {
      JSONObject search = new JSONObject();
      search.put("hideUnavailableItems", false);
      search.put("skusFilter", "ALL_AVAILABLE");
      search.put("simulationBehavior", "default");
      search.put("installmentCriteria", "MAX_WITHOUT_INTEREST");
      search.put("productOriginVtex", false);
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
      search.put("operator", JSONObject.NULL);
      search.put("fuzzy", JSONObject.NULL);
      search.put("facetsBehavior", "dynamic");
      search.put("withFacets", false);

      return Base64.getEncoder().encodeToString(search.toString().getBytes());
   }
}
