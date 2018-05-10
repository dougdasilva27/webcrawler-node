package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilCentralarCrawler extends CrawlerRankingKeywords {

  public BrasilCentralarCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 10;

    this.log("Página " + this.currentPage);

    JSONObject productsInfo = crawlProductInfo();
    JSONArray products = productsInfo.has("products") ? productsInfo.getJSONArray("products") : new JSONArray();

    if (products.length() > 0) {
      if (totalProducts == 0) {
        this.setTotalProducts(productsInfo);
      }

      for (int i = 0; i < products.length(); i++) {
        JSONObject product = products.getJSONObject(i);

        String internalId = crawlInternalId(product);
        String productUrl = crawlProductUrl(product);

        saveDataProduct(internalId, null, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);

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

  protected void setTotalProducts(JSONObject json) {
    if (json.has("pagination")) {
      JSONObject pagination = json.getJSONObject("pagination");

      if (pagination.has("totalProducts") && pagination.get("totalProducts") instanceof Integer) {
        this.totalProducts = pagination.getInt("totalProducts");
        this.log("Total: " + this.totalProducts);
      }
    }
  }

  private String crawlInternalId(JSONObject product) {
    String internalId = null;

    if (product.has("code")) {
      internalId = product.getString("code");
    }

    return internalId;
  }

  private String crawlProductUrl(JSONObject product) {
    String productUrl = null;

    if (product.has("productSlug")) {
      productUrl = "https://www.centralar.com.br/produto/" + product.get("productSlug");
    }

    return productUrl;
  }

  private JSONObject crawlProductInfo() {
    JSONObject products = new JSONObject();
    String payload =
        "{\"searchWords\":\"" + this.location + "\",\"filter\":{\"page\":" + this.currentPage + ",\"rpp\":50,\"orderBy\":1,\"categories\":null,"
            + "\"categorySlug\":null,\"brands\":null,\"btus\":null,\"energyRating\":null,\"operatingCycle\":null,\"newReleases\":null}}";

    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    headers.put("Authorization", "123456");
    headers.put("Referer", "https://www.centralar.com.br/");

    String page = fetchPostFetcher("http://api-services.centralar.com.br/mds/rest/products/search/v1", payload, headers, null);

    if (page != null && page.startsWith("{") && page.endsWith("}")) {
      try {
        products = new JSONObject(page);
      } catch (JSONException e) {
        logError(CommonMethods.getStackTrace(e));
      }
    }

    return products;
  }
}
