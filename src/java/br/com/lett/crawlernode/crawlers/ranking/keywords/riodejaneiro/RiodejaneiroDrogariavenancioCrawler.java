package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RiodejaneiroDrogariavenancioCrawler extends CrawlerRankingKeywords {

   private static final String HOST = "www.drogariavenancio.com.br";

   public RiodejaneiroDrogariavenancioCrawler(Session session) {
      super(session);
   }

   @Override
   protected void processBeforeFetch() {
      super.processBeforeFetch();

      Logging.printLogDebug(logger, session, "Adding cookie...");

      BasicClientCookie cookie = new BasicClientCookie("VTEXSC", "sc=1");
      cookie.setDomain(".drogariavenancio.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   protected void extractProductsFromCurrentPage() {
      this.pageSize = 12;

      this.log("Página " + this.currentPage);

      JSONArray products = getInfoFromApi(this.keywordEncoded, this.currentPage, this.pageSize);

      if (!products.isEmpty()) {
         for (Object o : products) {

            JSONObject product = (JSONObject) o;

            String internalId = null;
            String internalPid = product.optString("id");
            String productUrlIncomplete =  product.optString("url");
            String productUrl = productUrlIncomplete != null ? "https:" + productUrlIncomplete : null;
            saveDataProduct(null, internalPid, productUrl);

            this.log(
               "Position: " + this.position +
                  " - InternalId: " + internalId +
                  " - InternalPid: " + internalPid +
                  " - Url: " + productUrl
            );

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
   protected void setTotalProducts() {
      this.log("Total da busca: " + this.totalProducts);
   }

   private JSONArray getInfoFromApi(String keyword, int page, int resultsPerPage){

      StringBuilder urlBuilder = new StringBuilder();
      urlBuilder.append("https://api.linximpulse.com/engage/search/v3/search?");
      urlBuilder.append("apiKey=drogariavenancio-v7");
      urlBuilder.append("&page=" + page);
      urlBuilder.append("&resultsPerPage=" + resultsPerPage);
      urlBuilder.append("&terms=" + keyword);
      urlBuilder.append("&sortBy=relevance");

      this.log("Link onde são feitos os crawlers: " + urlBuilder.toString());

      Map<String,String> headers = new HashMap<>();
      headers.put("origin","https://www.drogariavenancio.com.br");
      headers.put("sec-fetch-dest","sec-fetch-dest");
      headers.put("referer","https://www.drogariavenancio.com.br/");
      headers.put("if-none-match","W/\"22ffb-l2FFtp/bptPucODPomcRxP7uLps\"");

      Request request = Request.RequestBuilder.create().setHeaders(headers).setUrl(urlBuilder.toString()).build();

      Response response = this.dataFetcher.get(session,request);
      JSONObject json = CrawlerUtils.stringToJson(response.getBody());
      this.totalProducts = json.optInt("size");

      return JSONUtils.getJSONArrayValue(json, "products");

   }
}
