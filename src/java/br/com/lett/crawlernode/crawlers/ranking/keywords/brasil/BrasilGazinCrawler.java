package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;

import br.com.lett.crawlernode.core.fetcher.models.Request;
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

  public BrasilGazinCrawler(Session session) {
    super(session);
  }


  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 20;
    this.log("Página " + this.currentPage);

    JSONObject searchJson = fetchJsonResponse();

    this.totalProducts = searchJson.optInt("total");
    JSONArray products = searchJson.optJSONArray("data");

    if (!products.isEmpty()) {

      for (Object e: products) {

         JSONObject product = (JSONObject) e;

         String internalId = scrapInternalId(product);

         String internalPid = product.optString("id");

         String productUrl = scrapProductUrl(product,internalId);

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

  /**
   * For this site you need to click on a button to follow search pages, even on the first page has a
   * button to list products, if you click the page reload and appears the search page
   * 
   * For categories this site only redirect you to categories page
   * 
   * @return
   */
  private JSONObject fetchJsonResponse(){

     String originalKeyword = this.keywordEncoded.replace("+", "%20");

     HashMap<String, String> headers = new HashMap<>();
     headers.put("canal", "gazin-ecommerce");

     String api = "https://marketplace-api.gazin.com.br/v1/canais/produtos?page="
        + this.currentPage + "&per_page=20&busca="
        + originalKeyword + "&order=titulo&sort=asc&per_page=20";

     Request request = Request.RequestBuilder.create().setUrl(api).setHeaders(headers).build();

     return CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());



  }

  private String scrapProductUrl(JSONObject product, String internalId){

     String slugName = "";
     String combination = "sem-cor";
     JSONObject variation = product.optJSONObject("variacao");

     if(variation != null){
        JSONObject productObject = variation.optJSONObject("produto");
        JSONArray combinationArray = variation.optJSONArray("combinacoes");

        if(!combinationArray.isEmpty()){
           combination = ((JSONObject) combinationArray.get(0)).optString("valor_slug");
        }

        if(productObject != null){
           slugName = productObject.optString("slug");
        }
     }

     StringBuilder url = new StringBuilder();
     url.append("https://www.gazin.com.br/produto/")
        .append(internalId + "/")
        .append(slugName)
        .append("?cor=" + combination);


      return url.toString();
  }

  private String scrapInternalId(JSONObject product){

     String internalId = null;
     JSONObject variation = product.optJSONObject("variacao");

     if(variation != null){

        JSONObject productJson = variation.optJSONObject("produto");

        if(productJson != null){
           internalId = productJson.optString("id");
        }
     }
     return  internalId;
  }

}
