package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.POSTFetcher;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class ColombiaMercadoniCrawler extends CrawlerRankingKeywords {

  public static final String PRODUCTS_API_URL =
      "https://j9xfhdwtje-3.algolianet.com/1/indexes/live_products_boost_desc/query"
          + "?x-algolia-agent=Algolia%20for%20vanilla%20JavaScript%203.30.0&x-algolia-application-id=J9XFHDWTJE"
          + "&x-algolia-api-key=2065b01208843995dbf34b4c58e8b7be";

  public ColombiaMercadoniCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 20;
    this.log("Página " + this.currentPage);

    JSONObject search = fetchProductsFromAPI();
    System.err.println(search);
    JSONArray arraySkus =
        search != null && search.has("hits") ? search.getJSONArray("hits") : new JSONArray();

    for (Object o : arraySkus) {
      JSONObject jsonSku = (JSONObject) o;
      String internalId = crawlInternalId(jsonSku);
      String internalPid = crawlInternalPid(jsonSku);
      String productUrl = crawlProductUrl(o, internalPid);
      saveDataProduct(internalId, internalPid, productUrl);

      if (this.arrayProducts.size() == productsLimit) {
        break;

      } else {

        this.result = false;
        this.log("Keyword sem resultado!");
      }
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
        + this.arrayProducts.size() + " produtos crawleados");


  }

  private String crawlInternalPid(JSONObject json) {
    String internalPid = null;

    if (json.has("product_simple")) {
      internalPid = json.getString("product_simple");
    }

    return internalPid;
  }

  private String crawlProductUrl(Object o, String internalPid) {
    return null;
  }

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("retailer_sku")) {
      internalId = json.get("retailer_sku").toString();
    }

    return internalId;
  }

  private JSONObject fetchProductsFromAPI() {
    JSONObject products = new JSONObject();

    String payload = "{\"params\":\"query=" + this.keywordEncoded + "s&hitsPerPage=15&page="
        + this.currentPage
        + "&facets=&facetFilters=%5B%5B%22location%3A%20557b4c374e1d3b1f00793e12%22%5D%2C%5B%5D%2C%22active%3A%20true%22%2C%22product_simple_active%3A%20true%22%2C%22visible%3A%20true%22%5D&numericFilters=%5B%22stock%3E0%22%5D&typoTolerance=strict&restrictSearchableAttributes=%5B%22name%22%5D\"}";
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");

    String page = POSTFetcher.fetchPagePOSTWithHeaders(PRODUCTS_API_URL, session, payload, cookies,
        1, headers, DataFetcher.randUserAgent(), null).trim();

    if (page.startsWith("{") && page.endsWith("}")) {
      try {
        System.err.println(page);
        // Using google JsonObject to get a JSONObject because this json can have a duplicate key.
        JsonObject JsonParser = new JsonParser().parse(page).getAsJsonObject();

        products = new JSONObject(JsonParser.toString());

      } catch (Exception e) {
        Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      }
    }

    return products;
  }
}

