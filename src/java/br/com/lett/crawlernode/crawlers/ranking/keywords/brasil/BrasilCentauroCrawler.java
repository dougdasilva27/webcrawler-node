package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilCentauroCrawler extends CrawlerRankingKeywords {

   public BrasilCentauroCrawler(Session session) {
      super(session);
   }

   private String nextPageurl = "";
   private JSONObject json;

   //This token is hardcoded because contains information about location and store id.
   private static final String BEARER_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1bmlxdWVfbmFtZSI6ImZyb250LWVuZCBjZW50YXVybyIsIm5iZiI6MTU4OTkxOTgxMywiZXhwIjoxOTA1NDUyNjEzLCJpYXQiOjE1ODk5MTk4MTN9.YeCTBYcWozaQb4MnILtfeKTeyCwApNgLSOfGeVVM8D0";

   protected JSONObject fetchJson(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("authorization", "Bearer " + BEARER_TOKEN);

      Request request = Request.RequestBuilder.create()
         .setHeaders(headers)
         .setUrl(url)
         .build();
      Response response = this.dataFetcher.get(session, request);

      return CrawlerUtils.stringToJson(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 40;
      this.log("Página " + this.currentPage);

      String url = "https://gateway.plataforma.centauro.com.br/yantar/api/search?term=" + this.keywordEncoded
         + "&resultsPerPage=40&page=" + this.currentPage + "&sorting=relevance&scoringProfile=scoreByRelevance&restrictSearch=true&multiFilters=true";

      this.log("Link onde são feitos os crawlers: " + url);

      json = fetchJson(url);
      JSONArray products = json.optJSONArray("products");

      if (!products.isEmpty()) {
         if (this.currentPage == 1) {
            this.totalProducts = json.optInt("size");
         }

         for (Object o : products) {
            JSONObject product = (JSONObject) o;
            String internalPid = product.optString("id");
            String productUrl = product.optString("url").replace("//", "");

            saveDataProduct(null, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return JSONUtils.getValueRecursive(json, "pagination.next", String.class) != null;
   }
}
