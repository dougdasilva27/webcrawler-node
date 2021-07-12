package br.com.lett.crawlernode.crawlers.ranking.keywords.costarica;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class CostaricaAutomercadoCrawler extends CrawlerRankingKeywords {


   public CostaricaAutomercadoCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;

   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException {
      this.pageSize = 30;


      JSONObject apiJson = fetchProducts();


      if (apiJson != null && !apiJson.isEmpty()) {
         JSONObject result = JSONUtils.getValueRecursive(apiJson, "results.0", JSONObject.class);
         if (this.totalProducts == 0) {
            this.totalProducts = result.optInt("nbHits", 0);
         }
         JSONArray products = result.optJSONArray("hits");
         for (Object o : products) {
            JSONObject product = (JSONObject) o;

            String internalId = product.optString("productID");
            String internalPid = product.optString("productNumber");
            String url = getUrl(internalId, internalPid);

            saveDataProduct(internalId, internalPid, url);
            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + url);

         }


      } else {
         log("keyword sem resultado");
      }


   }

   private String getUrl(String internalId, String internalPid) {
      String url = null;
      if (internalId != null && internalPid != null) {
         url = "https://www.automercado.cr/shop/" + internalPid + "?objectID=" + internalId;

      }
      return url;

   }

   private JSONObject fetchProducts() {

      String payload = "{\"requests\":[{\"indexName\":\"Product_Catalogue\",\"params\":\"query=" + this.keywordWithoutAccents + "&getRankingInfo=true&clickAnalytics=true&filters=storeid%3A%2203%22&highlightPreTag=__ais-highlight__&highlightPostTag=__%2Fais-highlight__&maxValuesPerFacet=50&page=" + (this.currentPage - 1) + "&facets=%5B%22catecom%22%2C%22parentProductid2%22%2C%22parentProductid%22%2C%22marca%22%5D&tagFilters=\"}]}";

      Map<String, String> headers = new HashMap<>();
      headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36");
      headers.put("content-type", "application/x-www-form-urlencoded");
      headers.put("Accept", "*/*");
      Request request = Request.RequestBuilder.create()
         .setUrl("https://fu5xfx7knl-dsn.algolia.net/1/indexes/*/queries?x-algolia-api-key=113941a18a90ae0f17d602acd16f91b2&x-algolia-application-id=FU5XFX7KNL")
         .setPayload(payload)
         .setHeaders(headers)
         .mustSendContentEncoding(false)
         .build();
      String content = this.dataFetcher.post(session, request).getBody();

      return CrawlerUtils.stringToJson(content);

   }
}
