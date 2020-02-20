package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

public class BrasilThebeautyboxCrawler extends CrawlerRankingKeywords {

   public BrasilThebeautyboxCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.APACHE;
   }

   private String keySHA256;
   private static final Integer API_VERSION = 1;
   private static final String SENDER = "vtex.store-resources@0.x";
   private static final String PROVIDER = "vtex.search-graphql@0.x";

   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);
      this.pageSize = 9;

      if (this.currentPage == 1) {
         this.keySHA256 = fetchSHA256Key();
      }

      JSONObject searchApi = fetchSearchApi();
      JSONArray products = searchApi.has("products") ? searchApi.getJSONArray("products") : new JSONArray();

      if (products.length() > 0) {

         if (this.totalProducts == 0) {
            setTotalProducts(searchApi);
         }

         for (Object object : products) {
            JSONObject product = (JSONObject) object;
            String productUrl = product.has("linkText") && !product.isNull("linkText") ? "https://www.beautybox.com.br/" + (product.get("linkText").toString()) + "/p" : null;
            String internalPid = product.has("productId") && !product.isNull("productId") ? product.get("productId").toString() : null;

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
      url.append("https://www.beautybox.com.br/_v/segment/graphql/v1?");
      url.append("workspace=master");
      url.append("&maxAge=short");
      url.append("&appsEtag=remove");
      url.append("&domain=store");
      url.append("&locale=pt-BR");
      url.append("&operationName=productSearchV2");

      JSONObject extensions = new JSONObject();
      JSONObject persistedQuery = new JSONObject();

      persistedQuery.put("version", API_VERSION);
      persistedQuery.put("sha256Hash", this.keySHA256);
      persistedQuery.put("sender", SENDER);
      persistedQuery.put("provider", PROVIDER);
      extensions.put("variables", createVariablesBase64());
      extensions.put("persistedQuery", persistedQuery);

      StringBuilder payload = new StringBuilder();
      try {
         payload.append("&variables=" + URLEncoder.encode("{}", "UTF-8"));
         payload.append("&extensions=" + URLEncoder.encode(extensions.toString(), "UTF-8"));
      } catch (UnsupportedEncodingException e) {
         Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      }

      url.append(payload.toString());
      this.log("Link onde são feitos os crawlers: " + url);

      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/json");

      Request request = RequestBuilder.create()
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

      search.put("withFacets", false);
      search.put("hideUnavailableItems", false);
      search.put("skusFilter", "ALL_AVAILABLE");
      search.put("simulationBehavior", "default");
      search.put("query", this.keywordWithoutAccents);
      search.put("map", "c");
      search.put("orderBy", "OrderByTopSaleDESC");
      search.put("from", this.arrayProducts.size());
      search.put("to", this.arrayProducts.size() + this.pageSize);
      search.put("facetsBehavior", "Static");

      return Base64.getEncoder().encodeToString(search.toString().getBytes());
   }

   private String fetchSHA256Key() {
      // When sha256Hash is not found, this key below works (on 03/02/2020)
      String hash = "e7089d34509aca4eeee981c1cc619cbc13f09e6a9d4c2abbd02183a8f0d9fd92";
      // When script with hash is not found, we use this url
      String url =
            "https://tbb.vtexassets.com/_v/public/assets/v1/published/bundle/public/react/asset.min.js?v=1"
                  +
                  "&files=vtex.css-handles@0.4.1,useCssHandles,applyModifiers&files=vtex.react-portal@0.3.0,common,Overlay"
                  + "&files=vtex.format-currency@0.1.3,common,FormattedCurrency,formatCurrency"
                  +
                  "&files=vtex.store-resources@0.47.0,common,0,1,Mutations,OrderFormContext,7,4,8,6,9,3,2,5,Queries&workspace=master";

      String homePage = "https://www.exito.com/";

      Request requestHome =
            RequestBuilder.create().setUrl(homePage).setCookies(cookies).mustSendContentEncoding(false).build();
      Document doc = Jsoup.parse(this.dataFetcher.get(session, requestHome).getBody());

      Elements scripts = doc.select("body > script[crossorigin]");
      for (Element e : scripts) {
         String scriptUrl = CrawlerUtils.scrapUrl(e, null, "src", "https", "tbb.vtexassets.com");
         if (scriptUrl.contains("vtex.store-resources@")) {
            url = scriptUrl;
            break;
         }
      }

      Request request =
            RequestBuilder.create().setUrl(url).setCookies(cookies).mustSendContentEncoding(false).build();
      String response = this.dataFetcher.get(session, request).getBody().replace(" ", "");

      String searchProducts = CrawlerUtils.extractSpecificStringFromScript(response, "productSearch(",
            false, "',", false);
      String firstIndexString = "@runtimeMeta(hash:";
      if (searchProducts.contains(firstIndexString) && searchProducts.contains(")")) {
         int x = searchProducts.indexOf(firstIndexString) + firstIndexString.length();
         int y = searchProducts.indexOf(')', x);

         hash = searchProducts.substring(x, y).replace("\"", "");

      }

      return hash;
   }
}
