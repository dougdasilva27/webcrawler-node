package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.util.JSONUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import org.jsoup.Jsoup;

import java.util.HashMap;
import java.util.Map;

public class BrasilFarmaciaindianaCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "http://www.farmaciaindiana.com.br/";
   private static final String VTEX_SEGMENT = "vtex_segment=eyJjYW1wYWlnbnMiOm51bGwsImNoYW5uZWwiOiIxIiwicHJpY2VUYWJsZXMiOm51bGwsInJlZ2lvbklkIjpudWxsLCJ1dG1fY2FtcGFpZ24iOm51bGwsInV0bV9zb3VyY2UiOm51bGwsInV0bWlfY2FtcGFpZ24iOm51bGwsImN1cnJlbmN5Q29kZSI6IkJSTCIsImN1cnJlbmN5U3ltYm9sIjoiUiQiLCJjb3VudHJ5Q29kZSI6IkJSQSIsImN1bHR1cmVJbmZvIjoicHQtQlIiLCJjaGFubmVsUHJpdmFjeSI6InB1YmxpYyJ9";

   public BrasilFarmaciaindianaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void processBeforeFetch() {
      BasicClientCookie cookie = new BasicClientCookie("vtex_segment", VTEX_SEGMENT);
      cookie.setDomain(HOME_PAGE);
      cookie.setPath("/");
      cookies.add(cookie);
   }

   @Override
   protected JSONObject fetchJSONObject(String url){
      Map<String, String> headers = new HashMap<>();
      headers.put("origin", "https://www.farmaciaindiana.com.br");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setHeaders(headers)
         .build();

      Response response = dataFetcher.get(session,request);

      return JSONUtils.stringToJson(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 16;

      this.log("Página " + this.currentPage);

      String url = "https://api.linximpulse.com/engage/search/v3/search?apiKey=farmaciaindiana&page=" + this.currentPage + "&resultsPerPage=24&terms=" + this.keywordEncoded + "&sortBy=relevance";

      this.log("Link onde são feitos os crawlers: " + url);

      JSONObject json = fetchJSONObject(url);

      JSONArray products = json.optJSONArray("products");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            this.totalProducts = json.optInt("size");
         }

         for (Object e : products) {
            JSONObject product = (JSONObject)e;
            String internalPid = product.optString("id");
            String urlProduct = "https:" + product.optString("url");

            saveDataProduct(null, internalPid, urlProduct);

            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }
}
