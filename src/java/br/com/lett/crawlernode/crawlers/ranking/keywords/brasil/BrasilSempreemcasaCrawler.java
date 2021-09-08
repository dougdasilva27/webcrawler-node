package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.HashMap;
import java.util.Map;

import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilSempreemcasaCrawler extends CrawlerRankingKeywords {

   protected String lat;
   protected String longi;

   public BrasilSempreemcasaCrawler(Session session) {
      super(session);
      this.dataFetcher = new JsoupDataFetcher();
      lat = session.getOptions().optString("latitude");
      longi = session.getOptions().optString("longitude");
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 24;

      this.log("Página " + this.currentPage);

      // monta a url com a keyword e a página
      String url = "https://api.sempreemcasa.com.br/products?page=" + this.currentPage + "&limit=24&text=" + this.keywordEncoded+"&latitude=" + lat + "&longitude=" + longi;
      this.log("Link onde são feitos os crawlers: " + url);

      JSONObject json = fetchApi(url);

      JSONArray products = json.optJSONArray("items");

      if (products != null && !products.isEmpty()) {
         this.totalProducts = json.optInt("total");

         for (Object e : products) {
            JSONObject product = (JSONObject) e;

            String internalPid = product.optString("id");
            String productUrl = "https://sempreemcasa.com.br/produtos/" + product.optString("slug") + "?latitude=" + lat + "&longitude=" + longi;

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

   private JSONObject fetchApi(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("Accept", "*/*");
      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.182 Safari/537.36");

      Request request = Request.RequestBuilder.create()
         .setCookies(cookies)
         .setUrl(url)
         .setHeaders(headers)
         .build();
      Response response = new JsoupDataFetcher().get(session, request);

      return CrawlerUtils.stringToJson(response.getBody());
   }
}
