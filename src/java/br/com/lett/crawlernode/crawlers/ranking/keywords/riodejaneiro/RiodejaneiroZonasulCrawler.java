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
import org.jsoup.nodes.Element;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLOutput;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class RiodejaneiroZonasulCrawler extends CrawlerRankingKeywords {

   private static final String COOKIES = "azion_balancer=B; vtex_session=eyJhbGciOiJFUzI1NiIsImtpZCI6IjA5RDFDRDUwRDM5RjVFREVCQzU0ODc0RUEyQkQ3RkZEQzIxNzVEQUQiLCJ0eXAiOiJqd3QifQ.eyJhY2NvdW50LmlkIjoiMmIyYjYxMTktNjM0Zi00ZjRiLWJmYzQtMmE0Y2Y5YzdiNTEzIiwiaWQiOiIyNWNmYTIwZS1mNjQ1LTQ5NGEtYTgyMC1iZTdmMzBhM2Q3ZDMiLCJ2ZXJzaW9uIjoyLCJzdWIiOiJzZXNzaW9uIiwiYWNjb3VudCI6InNlc3Npb24iLCJleHAiOjE2MTA0NzUwNjMsImlhdCI6MTYwOTc4Mzg2MywiaXNzIjoidG9rZW4tZW1pdHRlciIsImp0aSI6IjQyOWQzNGViLTg0YTUtNGQyZC1iZWFiLTNmMWRmNTYyY2ZmZSJ9.tAaLBagIcJP7FTQZc6QnYSIo60jgRpiVjPtobRYgxiZrNWtJ2kJn3mily06ZGxEhUFsI0uPv2993eoAA3ehD2A";
   //This is not the best way to set cookies but it was the only way found when this crawler was created

  public RiodejaneiroZonasulCrawler(Session session) {
    super(session);
     fetchMode = FetchMode.APACHE;
  }

   @Override
   protected Document fetchDocument(String url) {

      Map<String,String> headers = new HashMap<>();
      headers.put("cookie", COOKIES);

      Request request = Request.RequestBuilder.create().setUrl(url).setHeaders(headers).build();
      Document doc = Jsoup.parse(dataFetcher.get(session,request).getBody());

      return doc;

   }


   private void setTotalProducts(JSONObject data) {
      if (data.has("total") && data.get("total") instanceof Integer) {
         this.totalProducts = data.getInt("total");
         this.log("Total da busca: " + this.totalProducts);
      }
   }

   @Override
  protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);
      this.pageSize = 24;

      JSONObject searchApi = fetchSearchApi();
      JSONArray products = searchApi.has("products") ? searchApi.getJSONArray("products") : new JSONArray();

      if (products.length() > 0) {

         if (this.totalProducts == 0) {
            setTotalProducts(searchApi);
         }

         for (Object object : products) {
            JSONObject product = (JSONObject) object;
            String productUrl = CrawlerUtils.completeUrl(product.optString("linkText") + "/p", "https","zonasul.com.br");
            String internalId = product.optString("productId");

            saveDataProduct(internalId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);

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

   private String fetchSHA256Key() {

      if (hash != null) {
         return hash;
      }
      String url = "https://www.zonasul.com.br/"+this.keywordWithoutAccents+"?_q="+this.keywordWithoutAccents+"&map=ft&page=" + this.currentPage;

      Map<String,String> headers = new HashMap<>();
      headers.put("cookie", COOKIES);

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).build();
      String response = this.dataFetcher.get(session, request).getBody();

      if (response != null && !response.isEmpty()) {
         Document doc = Jsoup.parse(response);
         String nonFormattedJson = doc.selectFirst("template[data-varname=__STATE__] script").html();
         JSONObject stateJson = CrawlerUtils.stringToJson(nonFormattedJson);

         for (String key : stateJson.keySet()) {
            String firstIndexString = "@runtimeMeta(";
            String keyIdentifier = "$ROOT_QUERY.searchMetadata";

            if (key.contains(firstIndexString) && key.contains(keyIdentifier) && key.endsWith(")")) {
               int x = key.indexOf(firstIndexString) + firstIndexString.length();
               int y = key.indexOf(')', x);

               JSONObject hashJson = CrawlerUtils.stringToJson(key.substring(x, y).replace("\\\"", "\""));
               System.err.println(hashJson);
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
      search.put("hideUnavailableItems", true);
      search.put("skusFilter", "ALL");
      search.put("simulationBehavior", "default");
      search.put("installmentCriteria", "MAX_WITHOUT_INTEREST");
      search.put("productOriginVtex", true);
      search.put("map", "ft");
      search.put("query", this.keywordWithoutAccents);
      search.put("orderBy", "");
      search.put("from", 0);
      search.put("to", 47);


      JSONArray selectedFacets = new JSONArray();

      JSONObject keywordJson = new JSONObject();
      keywordJson.put("key", "ft");
      keywordJson.put("values", this.keywordWithoutAccents);

      selectedFacets.put(keywordJson);

      search.put("selectedFacets", selectedFacets);

      search.put("fullText", this.keywordWithoutAccents);
      search.put("facetsBehavior", "Static");
      search.put("withFacets", false);


      return Base64.getEncoder().encodeToString(search.toString().getBytes());
   }

   private static final String API_VERSION = "vtex.render-runtime@8.126.5";

   private JSONObject fetchSearchApi() {
      JSONObject searchApi = new JSONObject();

      StringBuilder url = new StringBuilder();
      url.append("https://www.zonasul.com.br/_v/segment/graphql/v1?")
         .append("workspace=master")
         .append("&maxAge=medium")
         .append("&appsEtag=remove")
         .append("&domain=store")
         .append("&locale=pt-BR")
         .append("&operationName=productSearchV3")
         .append("&variables=%7B%7D");

        // .append("&extensions=%7B%22persistedQuery%22%3A%7B%22version%22%3A1%2C%22sha256Hash%22%3A%2212935bec17ebd1fee1c09a5a53bd4464eb2fe2cd8d6d25881a85ce708eb283f7%22%2C%22sender%22%3A%22vtex.store-resources%400.x%22%2C%22provider%22%3A%22vtex.search-graphql%400.x%22%7D%2C%22variables%22%3A%22eyJoaWRlVW5hdmFpbGFibGVJdGVtcyI6dHJ1ZSwic2t1c0ZpbHRlciI6IkFMTCIsInNpbXVsYXRpb25CZWhhdmlvciI6ImRlZmF1bHQiLCJpbnN0YWxsbWVudENyaXRlcmlhIjoiTUFYX1dJVEhPVVRfSU5URVJFU1QiLCJwcm9kdWN0T3JpZ2luVnRleCI6dHJ1ZSwibWFwIjoiZnQiLCJxdWVyeSI6ImNvY2EiLCJvcmRlckJ5IjoiIiwiZnJvbSI6MCwidG8iOjQ3LCJzZWxlY3RlZEZhY2V0cyI6W3sia2V5IjoiZnQiLCJ2YWx1ZSI6ImNvY2EifV0sImZ1bGxUZXh0IjoiY29jYSIsImZhY2V0c0JlaGF2aW9yIjoiU3RhdGljIiwid2l0aEZhY2V0cyI6ZmFsc2V9%22%7D");


      JSONObject extensions = new JSONObject();



      JSONObject persistedQuery = new JSONObject();

      persistedQuery.put("version", "1");
      persistedQuery.put("sha256Hash", "07b7d5f8ff5032b3c21578422c4d0f3d6cc2d74294f55b3cadf423ddda5625a5");

      System.err.println(fetchSHA256Key());

      persistedQuery.put("sender", "vtex.store-resources@0.x");
      persistedQuery.put("provider", "vtex.search-graphql@0.x");

      extensions.put("variables", "eyJwcm9kdWN0T3JpZ2luVnRleCI6dHJ1ZSwiaGlkZVVuYXZhaWxhYmxlSXRlbXMiOnRydWUsInF1ZXJ5IjoicGVpeGUiLCJvcmRlckJ5IjoiIiwiZnVsbFRleHQiOiJwZWl4ZSIsImluc3RhbGxtZW50Q3JpdGVyaWEiOiJNQVhfV0lUSE9VVF9JTlRFUkVTVCIsInNlbGVjdGVkRmFjZXRzIjpbeyJ2YWx1ZXMiOiJwZWl4ZSIsImtleSI6ImZ0In1dLCJmYWNldHNCZWhhdmlvciI6IlN0YXRpYyIsInNrdXNGaWx0ZXIiOiJBTEwiLCJzaW11bGF0aW9uQmVoYXZpb3IiOiJkZWZhdWx0IiwiZnJvbSI6MCwidG8iOjQ3LCJ3aXRoRmFjZXRzIjpmYWxzZSwibWFwIjoiZnQifQ==\n");
      extensions.put("persistedQuery", persistedQuery);


      try {
         url.append("&extensions=" + URLEncoder.encode(extensions.toString(), "UTF-8"));

         //  payload.append("&variables=").append(URLEncoder.encode("{}", "UTF-8"));
        // payload.append("&extensions=").append(URLEncoder.encode(extensions.toString(), "UTF-8"));
      } catch (UnsupportedEncodingException e) {
         Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      }


      this.log("Link onde são feitos os crawlers: " + url);


      Map<String,String> headers = new HashMap<>();
      headers.put("cookie", COOKIES);



      Request request = RequestBuilder.create().setUrl(url.toString()).setCookies(cookies).setHeaders(headers).mustSendContentEncoding(false).build();
      JSONObject response = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

      if (response.has("data")) {
         JSONObject data = response.getJSONObject("data");

         if (data.has("productSearch")) {
            searchApi = data.getJSONObject("productSearch");
         }
      }

      return searchApi;
   }

}
