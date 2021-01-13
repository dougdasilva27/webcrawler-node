package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
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

      this.pageSize = 12;

      String url = "https://" + HOST + "/busca/?ft=" + keywordWithoutAccents.replace(" ", "%20") +
         "&page=" + currentPage;
      String apiUrl = "https://api.smdigital.cl:8443/v0/cl/jumbo/vtex/front/prod/proxy/api/v2/products/search/busca?ft=" +
         keywordWithoutAccents.replace(" ", "%20") +
         "&page=" + currentPage +
         "&sc=" + storeCode;

      Map<String, String> headers = new HashMap<>();
      headers.put("Referer", url);
      headers.put("x-api-key", API_KEY);

      Request request = RequestBuilder.create().setUrl(apiUrl).setHeaders(headers).setCookies(cookies).build();
      String res = new ApacheDataFetcher().get(session, request).getBody();

      JSONArray products = CrawlerUtils.stringToJsonArray(res);

      if (products.length() > 0) {
         for (Object e : products) {
            JSONObject product = (JSONObject) e;
            String internalPid = product.has("productId") && !product.isNull("productId") ? product.get("productId").toString() : null;
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

   protected String getStoreCode() {
      return storeCode;
   }

   @Override
   protected void processBeforeFetch() {
      super.processBeforeFetch();

      Logging.printLogDebug(logger, session, "Adding cookie...");

      BasicClientCookie cookie = new BasicClientCookie("VTEXSC", "sc=" + storeCode);
      cookie.setDomain("." + ChileJumboCrawler.HOST);
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   @Override
   protected boolean hasNextPage() {
      return !this.arrayProducts.isEmpty() && ((this.arrayProducts.size() / this.currentPage) >= this.pageSize);
   }

   private String crawlProductUrl(JSONObject product) {
      String productUrl = product.has("linkText") && !product.isNull("linkText") ? product.get("linkText").toString() : null;

      if (productUrl != null) {
         productUrl += productUrl.endsWith("/p") ? "" : "/p";
      }

      return CrawlerUtils.completeUrl(productUrl, "https", HOST);
   }

}
