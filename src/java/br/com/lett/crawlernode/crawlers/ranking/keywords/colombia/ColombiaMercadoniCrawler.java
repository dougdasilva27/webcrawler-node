package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import com.google.gson.JsonParser;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.POSTFetcher;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class ColombiaMercadoniCrawler extends CrawlerRankingKeywords {

  public static final String PRODUCTS_API_URL = "https://j9xfhdwtje-3.algolianet.com/1/indexes/live_products_boost_desc/query"
      + "?x-algolia-agent=Algolia%20for%20vanilla%20JavaScript%203.30.0&x-algolia-application-id=J9XFHDWTJE"
      + "&x-algolia-api-key=2065b01208843995dbf34b4c58e8b7be";

  public ColombiaMercadoniCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 15;
    this.log("Página " + this.currentPage);

    JSONObject search = fetchProductsFromAPI();

    JSONArray arraySkus = search != null && search.has("hits") ? search.getJSONArray("hits") : new JSONArray();

    if (arraySkus.length() > 0) {

      if (this.totalProducts == 0) {
        setTotalProducts(search);
      }

      for (Object product : arraySkus) {
        JSONObject jsonSku = (JSONObject) product;
        String internalId = crawlInternalId(jsonSku);
        String internalPid = crawlInternalPid(jsonSku);
        String productUrl = crawlProductUrl(jsonSku, internalId);

        saveDataProduct(internalId, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);

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

  private void setTotalProducts(JSONObject search) {
    if (search.has("nbHits")) {
      this.totalProducts = search.getInt("nbHits");
    }
  }

  private String crawlInternalPid(JSONObject json) {
    String internalPid = null;

    if (json.has("retailer_sku")) {
      internalPid = json.getString("retailer_sku");
    }

    return internalPid;
  }

  // https://www.mercadoni.com.co/tienda/jumbo-colombia/p/Woolite-Detergente-L%C3%ADquido-Todos-Los-Dias-2000Ml-X-2-Bi-Pack?retailer_sku=75009842549
  private String crawlProductUrl(JSONObject product, String internalId) {
    String slug = product.has("slug") ? product.getString("slug") : null;
    String url = null;

    if (slug != null) {
      url = "https://www.mercadoni.com.co/buscar?q=" + slug + "l&retailer=jumbo-colombia&product_simple=" + internalId;
    }

    return url;
  }

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("product_simple")) {
      internalId = json.get("product_simple").toString();
    }

    return internalId;
  }

  private JSONObject fetchProductsFromAPI() {
    JSONObject products = new JSONObject();


    String payload = "{\"params\":\"query=" + this.keywordEncoded + "s&hitsPerPage=15&page=" + this.currentPage
        + "&facets=&facetFilters=%5B%5B%22location%3A%20557b4c374e1d3b1f00793e12%22%5D%2C%5B%5D%2C%22active%3A%20true%22%2C%22product_simple_active%3A%20true%22%2C%22visible%3A%20true%22%5D&numericFilters=%5B%22stock%3E0%22%5D&typoTolerance=strict&restrictSearchableAttributes=%5B%22name%22%5D\"}";
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");

    String page =
        POSTFetcher.fetchPagePOSTWithHeaders(PRODUCTS_API_URL, session, payload, cookies, 1, headers, DataFetcher.randUserAgent(), null).trim();

    if (page.startsWith("{") && page.endsWith("}")) {
      try {

        // Using google JsonObject to get a JSONObject because this json can have a duplicate key.
        products = new JSONObject(new JsonParser().parse(page).getAsJsonObject().toString());

      } catch (Exception e) {
        Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
      }
    }

    return products;
  }
}

