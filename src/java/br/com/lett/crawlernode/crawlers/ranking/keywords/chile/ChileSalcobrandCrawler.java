package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class ChileSalcobrandCrawler extends CrawlerRankingKeywords {

  public ChileSalcobrandCrawler(Session session) {
    super(session);
  }


   protected JSONObject requestApi(){
      String url = "https://gm3rp06hjg-dsn.algolia.net/1/indexes/" +
         "*/queries?x-algolia-agent=Algolia%20for%20JavaScript%20(4.8.5)" +
         "%3B%20Browser%20(lite)%3B%20JS%20Helper%20(3.4.4)%3B%20react%20(16.13.1)" +
         "%3B%20react-instantsearch%20(6.9.0)&x-algolia-api-key=51f403f1055fee21d9e54d028dc19eba&x" +
         "-algolia-application-id=GM3RP06HJG";
      this.log("Link onde são feitos os crawlers: " + url);

      String payload = "{\"requests\":[{\"indexName\":\"sb_variant_production\",\"params\":\"highlightPreTag=<ais-highlight-0000000000>&highlightPostTag=</ais-highlight-0000000000>&query=]" + this.keywordEncoded + "&clickAnalytics=true&hitsPerPage=24&maxValuesPerFacet=50&page=" + (this.currentPage - 1) +"\"}]}";

      Request request = RequestBuilder.create().setUrl(url).mustSendContentEncoding(false).setPayload(payload).build();
      String content = this.dataFetcher.post(session, request).getBody();
      JSONObject contentJson =  CrawlerUtils.stringToJson(content);

      return JSONUtils.getValueRecursive(contentJson, "results.0", JSONObject.class);
   }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);

    this.pageSize = 24;

    JSONObject searchedJson = requestApi();

    JSONArray products = JSONUtils.getValueRecursive(searchedJson, "hits", JSONArray.class);

    if (products.length() > 0) {

      if (this.totalProducts == 0) {

        setTotalProducts(searchedJson);

      }

      for (Object object : products) {

        JSONObject product = (JSONObject) object;

        String internalId = product.optString("sku");
        String internalPid = product.optString("id");
        String productUrl = crawlProductUrl(product, internalId);

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


  private String crawlProductUrl(JSONObject product, String internalId) {
    String slug = null;

    if (product.has("slug")) {

      slug = product.getString("slug");

    }

     return "https://salcobrand.cl/products/" + slug + "?default_sku=" + internalId;
  }


  protected void setTotalProducts(JSONObject searchedJson) {

    if (searchedJson.has("nbHits")) {

      if (searchedJson.get("nbHits") instanceof Integer) {

        this.totalProducts = searchedJson.getInt("nbHits");
        this.log("Total da busca: " + this.totalProducts);

      }

    }

  }


}
