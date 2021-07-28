package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ChilePreunicCrawler extends CrawlerRankingKeywords {

   public ChilePreunicCrawler(Session session) {
      super(session);
   }

   @Override
   protected JSONObject fetchJSONObject(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36");
      headers.put("Accept-Encoding", "gzip, deflate, br");
      headers.put("accept", "*/*");
      headers.put("content-type", "application/json");
      headers.put("Referer", "https://preunic.cl/");

      String jsonParams = "query=" + this.keywordEncoded.replace(" ", "%20") + "&hitsPerPage=20&maxValuesPerFacet=500&page=" + (this.currentPage - 1) + "&facetingAfterDistinct=true&filters=(state%3A%22active%22%20OR%20state%3A%22discontinue%22)&facets=%5B%22normal_price%22%2C%22brand%22%2C%22has_promotions%22%2C%22sku%22%2C%22has_sbpay_promotions%22%2C%22categories.lvl0%22%2C%22categories.lvl0%22%5D&tagFilters=";

      String payload = "{\"requests\":[{\"indexName\":\"PreunicVariants_production\",\"params\":\"" + jsonParams + "\"}]}";

      Request request = Request.RequestBuilder.create()
         .setHeaders(headers)
         .setPayload(payload)
         .setUrl(url)
         .build();

      //For some reason, passing the data fetcher in the constructor was not working. The only way that I managed to do this request was instantiating here, using Jsoup.
      //With apache data fetcher all the requests return 415 (unsupported media type)
      //With fetcher data fetcher, we have this: "message":"unrecognized Content-Encoding: gzip","status":415
      Response response = new JsoupDataFetcher().post(session, request);

      return JSONUtils.stringToJson(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 24;
      this.log("Página " + this.currentPage);

      String url = "https://7gdqzike3q-dsn.algolia.net/1/indexes/*/queries?x-algolia-agent=Algolia%20for%20vanilla%20JavaScript%20(lite)%203.27.0%3Binstantsearch.js%202.7.4%3BJS%20Helper%202.26.0&x-algolia-application-id=7GDQZIKE3Q&x-algolia-api-key=c9e280888ffa03e7def9cff5b5a33f3c";
      this.log("Link onde são feitos os crawlers: " + url);
      JSONObject json = fetchJSONObject(url);

      JSONArray products = JSONUtils.getValueRecursive(json, "results.0.hits", JSONArray.class);

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            this.totalProducts = JSONUtils.getValueRecursive(json, "results.0.nbHits", Integer.class);
         }

         for (Object e : products) {
            JSONObject product = (JSONObject) e;
            String internalPid = product.optString("sku");
            String productUrl = "https://preunic.cl/products/" + product.optString("product_slug") + "?default_sku=" + internalPid;

            saveDataProduct(null, internalPid, productUrl);

            this.log(
               "Position: " + this.position +
                  " - InternalId: " + null +
                  " - InternalPid: " + internalPid +
                  " - Url: " + productUrl);

            if (this.arrayProducts.size() == productsLimit)
               break;
         }

      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");

   }
}
