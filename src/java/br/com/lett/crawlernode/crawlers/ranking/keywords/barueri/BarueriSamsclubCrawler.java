package br.com.lett.crawlernode.crawlers.ranking.keywords.barueri;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class BarueriSamsclubCrawler extends CrawlerRankingKeywords {

   public BarueriSamsclubCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   private static final String HOME_PAGE = "https://www.samsclub.com.br/";
   private String hash;

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

      String url = HOME_PAGE + this.keywordEncoded + "?_q=" + this.keywordEncoded + "&map=ft&page=" + this.currentPage;
      this.hash = fetchSHA256Key(url.replace("+", "%20"));
      JSONObject searchApi = fetchSearchApi();
      JSONArray products = searchApi != null && searchApi.has("products") ? searchApi.optJSONArray("products") : new JSONArray();

      if (products.length() > 0) {

         if (this.totalProducts == 0) {
            setTotalProducts(searchApi);
         }

         for (Object object : products) {
            JSONObject product = (JSONObject) object;
            try {
               String productUrl = new URIBuilder(HOME_PAGE).setPath(product.optString("linkText") + "/p").build().toString();
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

   private String fetchSHA256Key(String url) {
      String returnHash = "";
      Map<String, String> headers = new HashMap<>();
      headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.182 Safari/537.36");
      headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setHeaders(headers)
         .build();

      Document doc = Jsoup.parse(new JsoupDataFetcher().get(session, request).getBody());

      if(doc != null){
         Elements scripts = doc.select("script");

         for (Element script : scripts) {
            if(script.html().contains("__STATE__")){
               String delimiter = "({\\\"hash\\\":\\\"";
               String jsonStr = script.html().substring(script.html().indexOf("$ROOT_QUERY.productSearch"), script.html().indexOf(delimiter) + 77);
               String hash256 = jsonStr.substring(jsonStr.indexOf(delimiter));

               if(!hash256.equals("")){
                  returnHash = hash256.replace(delimiter, "");
                  break;
               }
            }
         }
      }

      return returnHash;
   }

   private JSONObject fetchSearchApi() {
      JSONObject searchApi = new JSONObject();

      StringBuilder url = new StringBuilder();
      url.append(HOME_PAGE).append("_v/private/graphql/v1?")
         .append("workspace=master")
         .append("&maxAge=short")
         .append("&appsEtag=remove")
         .append("&domain=store")
         .append("&locale=pt-BR")
         .append("&operationName=productSearchV3")
         .append("&variables=%7B%7D");

      JSONObject extensions = new JSONObject();
      JSONObject persistedQuery = new JSONObject();

      persistedQuery.put("version", 1);
      persistedQuery.put("sha256Hash", hash);

      persistedQuery.put("sender", "vtex.store-resources@0.x");
      persistedQuery.put("provider", "vtex.search-graphql@0.x");

      extensions.put("persistedQuery", persistedQuery);
      extensions.put("variables", createVariablesBase64());

      StringBuilder payload = new StringBuilder();
      try {
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
      search.put("facetsBehavior", "Static");
      search.put("categoryTreeBehavior", "default");
      search.put("withFacets", false);

      return Base64.getEncoder().encodeToString(search.toString().getBytes());
   }
}
