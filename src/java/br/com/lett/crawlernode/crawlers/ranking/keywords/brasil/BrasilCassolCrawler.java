package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

public class BrasilCassolCrawler extends CrawlerRankingKeywords {

   public BrasilCassolCrawler(Session session) {
      super(session);
   }

   private static final Integer API_VERSION = 1;
   private static final String SENDER = "vtex.store-resources@0.x";
   private static final String PROVIDER = "vtex.search-graphql@0.x";
   private static final String HOME_PAGE = "https://www.cassol.com.br/";

   private String keySHA256 = "f8ce152c3f5902db2a9480caca68d84e6834401e30278b9c938d4bb6a957acd4";

   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);
      this.pageSize = 20;

      if (this.currentPage == 1) {
         StringBuilder searchPage = new StringBuilder();

         searchPage.append(HOME_PAGE)
               .append("busca/")
               .append(this.keywordEncoded)
               .append("?page=")
               .append(this.currentPage);

         String apiUrl = searchPage.toString().replace("+", "%20");

         this.currentDoc = fetchDocument(apiUrl);
         scrapHashCode();
      }

      JSONObject searchApi = fetchSearchApi();
      JSONArray products = searchApi.has("products") ? searchApi.getJSONArray("products") : new JSONArray();

      if (products.length() > 0) {

         if (this.totalProducts == 0) {
            setTotalProducts(searchApi);
         }

         for (Object object : products) {
            JSONObject product = (JSONObject) object;
            String productUrl = CrawlerUtils.completeUrl(product.optString("linkText") + "/p", "https",
                  HOME_PAGE.replace("https://", "").replace("/", ""));
            String internalPid = product.optString("productId");

            saveDataProduct(null, internalPid, productUrl);

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

   private void setTotalProducts(JSONObject data) {
      this.totalProducts = CrawlerUtils.getIntegerValueFromJSON(data, "recordsFiltered", 0);
      this.log("Total da busca: " + this.totalProducts);
   }

   private void scrapHashCode() {
      String url = null;

      Request requestHome =
            RequestBuilder.create().setUrl(HOME_PAGE).setCookies(cookies).mustSendContentEncoding(false).build();
      Document doc = Jsoup.parse(this.dataFetcher.get(session, requestHome).getBody());

      Elements scripts = doc.select("body > script[crossorigin]");
      for (Element e : scripts) {
         String scriptUrl = CrawlerUtils.scrapUrl(e, null, "src", "https", "cassol.vtexassets.com");
         if (scriptUrl.contains("productSearch")) {
            url = scriptUrl;
            break;
         }
      }

      if (url != null) {
         Request request =
               RequestBuilder.create().setUrl(url).setCookies(cookies).mustSendContentEncoding(false).build();
         String response = this.dataFetcher.get(session, request).getBody().replace(" ", "");

         String searchProducts = CrawlerUtils.extractSpecificStringFromScript(response, "productSearch(",
               false, "',", false);
         String firstIndexString = "@runtimeMeta(hash:";
         if (searchProducts.contains(firstIndexString) && searchProducts.contains(")")) {
            int x = searchProducts.indexOf(firstIndexString) + firstIndexString.length();
            int y = searchProducts.indexOf(')', x);

            this.keySHA256 = searchProducts.substring(x, y).replace("\"", "");

         }
      }
   }

   /**
    * This function request a api with a JSON encoded on BASE64
    *
    * This json has informations like: pageSize, keyword and substantive {@link fetchSubstantive}
    *
    * @return
    */
   private JSONObject fetchSearchApi() {
      JSONObject searchApi = new JSONObject();
      StringBuilder url = new StringBuilder();
      url.append(HOME_PAGE + "_v/segment/graphql/v1?");

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

      if (response.has("data") && !response.isNull("data")) {
         JSONObject data = response.getJSONObject("data");

         if (data.has("productSearch") && !data.isNull("productSearch")) {
            searchApi = data.getJSONObject("productSearch");
         }
      }

      return searchApi;
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
}
