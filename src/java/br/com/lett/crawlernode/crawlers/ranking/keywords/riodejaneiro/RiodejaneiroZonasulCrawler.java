package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class RiodejaneiroZonasulCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "https://www.zonasul.com.br/";
   private static final String COOKIES = "azion_balancer=B; vtex_session=eyJhbGciOiJFUzI1NiIsImtpZCI6IjA5RDFDRDUwRDM5RjVFREVCQzU0ODc0RUEyQkQ3RkZEQzIxNzVEQUQiLCJ0eXAiOiJqd3QifQ.eyJhY2NvdW50LmlkIjoiMmIyYjYxMTktNjM0Zi00ZjRiLWJmYzQtMmE0Y2Y5YzdiNTEzIiwiaWQiOiIyNWNmYTIwZS1mNjQ1LTQ5NGEtYTgyMC1iZTdmMzBhM2Q3ZDMiLCJ2ZXJzaW9uIjoyLCJzdWIiOiJzZXNzaW9uIiwiYWNjb3VudCI6InNlc3Npb24iLCJleHAiOjE2MTA0NzUwNjMsImlhdCI6MTYwOTc4Mzg2MywiaXNzIjoidG9rZW4tZW1pdHRlciIsImp0aSI6IjQyOWQzNGViLTg0YTUtNGQyZC1iZWFiLTNmMWRmNTYyY2ZmZSJ9.tAaLBagIcJP7FTQZc6QnYSIo60jgRpiVjPtobRYgxiZrNWtJ2kJn3mily06ZGxEhUFsI0uPv2993eoAA3ehD2A";
   //This is not the best way to set cookies but it was the only way found when this crawler was created

  public RiodejaneiroZonasulCrawler(Session session) {
    super(session);
     fetchMode = FetchMode.APACHE;
  }


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

      String url = HOME_PAGE + this.keywordEncoded+"?_q="+this.keywordEncoded+"&map=ft&page=" + this.currentPage;

      Map<String,String> headers = new HashMap<>();
      headers.put("cookie", COOKIES);

      Request request = RequestBuilder.create().setUrl(url.replace("+", "%20")).setCookies(cookies).setHeaders(headers).build();
      String response = this.dataFetcher.get(session, request).getBody();

      JSONObject searchApi = fetchSearchApi(response);
      JSONArray products = searchApi != null && searchApi.has("products") ? searchApi.optJSONArray("products") : new JSONArray();

      if (products.length() > 0) {

         if (this.totalProducts == 0) {
            setTotalProducts(searchApi);
         }

         for (Object object : products) {
            JSONObject product = (JSONObject) object;
            String productUrl = CrawlerUtils.completeUrl(product.optString("linkText") + "/p", "https","zonasul.com.br");
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

   private String hash;

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
      if (hash == null) {
         hash = "2da1e09e3e9fb5f2e1ad240b91afa44914933ec59f8ab99cc52d0be296923299";
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
      url.append(HOME_PAGE + "_v/segment/graphql/v1?")
         .append("workspace=master")
         .append("&maxAge=medium")
         .append("&appsEtag=remove")
         .append("&domain=store")
         .append("&locale=pt-BR")
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
         payload.append("&variables=" + URLEncoder.encode("{}", "UTF-8"));
         payload.append("&extensions=" + URLEncoder.encode(extensions.toString(), "UTF-8"));
      } catch (UnsupportedEncodingException e) {
         Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      }

      url.append(payload.toString());

      this.log("Link onde são feitos os crawlers: " + url);

      Map<String,String> headers = new HashMap<>();
      headers.put("cookie", COOKIES);

      Request request = RequestBuilder.create().setUrl(url.toString()).setCookies(cookies).setHeaders(headers).build();
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
