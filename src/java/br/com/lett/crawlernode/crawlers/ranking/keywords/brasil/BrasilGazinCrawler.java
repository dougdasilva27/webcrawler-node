package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.util.JSONUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

public class BrasilGazinCrawler extends CrawlerRankingKeywords {

   private static final String API_KEY = "j33o%2BMRkwsxYR2XUKVQBQw%3D%3D";
  public BrasilGazinCrawler(Session session) {
    super(session);
  }


  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 20;
    this.log("Página " + this.currentPage);

    JSONObject searchJson = fetchJsonResponse();

    this.totalProducts = searchJson.optInt("size");
    JSONArray products = JSONUtils.getJSONArrayValue(searchJson, "products");

    if (!products.isEmpty()) {

      for (Object e: products) {

         JSONObject product = (JSONObject) e;

         String internalId = product.optString("selectedSku");

         String internalPid = scrapInternalPid(product);

         String productUrl =  CrawlerUtils.completeUrl(product.optString("url"), "https", "gazin.com.br");

        saveDataProduct(internalId, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit)
          break;

      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }

  private JSONObject fetchJsonResponse(){

     String resultsPerPage = "20";

     HashMap<String, String> headers = new HashMap<>();
     headers.put("canal", "gazin-ecommerce");

     String api = "https://api.linximpulse.com/engage/search/v3/search?apikey=gazin&secretkey="
        + API_KEY + "&terms=" + this.keywordEncoded + "&page=" + this.currentPage + "&resultsPerPage="
        + resultsPerPage + "&sortBy=relevance&productFormat=complete&showOnlyAvailable=false&deviceId=null";

     Request request = Request.RequestBuilder.create().setUrl(api).setHeaders(headers).build();

     return CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

  }

  private String scrapInternalPid(JSONObject product){

     String idText = product.optString("id");
     String internalPid = null;

     if(idText != null){

        internalPid = idText;

        if(internalPid.contains("-")){

           internalPid = idText.substring(0, idText.indexOf("-"));
        }
     }
     return  internalPid;
  }
}
