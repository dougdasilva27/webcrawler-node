package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ChileJumboCrawler extends CrawlerRankingKeywords {

   public ChileJumboCrawler(Session session) {
      super(session);
   }

   protected String storeCode = getStoreCode();
   protected static final String API_KEY = "IuimuMneIKJd3tapno2Ag1c1WcAES97j";
   protected static final String HOST = br.com.lett.crawlernode.crawlers.extractionutils.core.ChileJumboCrawler.HOST;

   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);

      this.pageSize = 40;

      JSONObject bodyJson = fetchProducts();

      if (this.currentPage == 1) {
         this.totalProducts = bodyJson.optInt("recordsFiltered");
      }

      JSONArray products = JSONUtils.getJSONArrayValue(bodyJson, "products");

      if (!products.isEmpty()) {
         for (Object o : products) {
            JSONObject product = (JSONObject) o;
            String internalPid = product.optString("productReference", null);
            String productUrl = crawlProductUrl(product);

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

   private JSONObject fetchProducts() {
      String url = "https://apijumboweb.smdigital.cl/catalog/api/v1/search/" + keywordWithoutAccents.toLowerCase().replace(" ", "%20") + "?page=" + this.currentPage;

      Map<String, String> headers = new HashMap<>();
      headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_1_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Safari/537.36");
      headers.put("x-api-key", API_KEY);
      headers.put("content-type", "application/json");
      headers.put("accept", "*/*");
      headers.put("origin", "https://www.jumbo.cl");
      headers.put("referer", "https://www.jumbo.cl/");

      String payload = "{\"selectedFacets\":[{\"key\":\"trade-policy\",\"value\":\""+getStoreCode()+"\"}]}";

      Request request = RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .setProxyservice(
            Arrays.asList(
               ProxyCollection.BUY_HAPROXY,
               ProxyCollection.LUMINATI_SERVER_BR_HAPROXY,
               ProxyCollection.INFATICA_RESIDENTIAL_BR_HAPROXY
               )
         )
         .build();

      Response response = new JsoupDataFetcher().post(session, request);

      return CrawlerUtils.stringToJson(response.getBody());
   }

   protected String getStoreCode() {
      return session.getOptions().optString("code_locale");
   }

   private String crawlProductUrl(JSONObject product) {
      String productUrl = product.optString("linkText", null);

      if (productUrl != null) {
         productUrl += productUrl.endsWith("/p") ? "" : "/p";
      }

      return CrawlerUtils.completeUrl(productUrl, "https", HOST);
   }

}
