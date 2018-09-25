package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilBalarotiCrawler extends CrawlerRankingKeywords {

  public BrasilBalarotiCrawler(Session session) {
    super(session);
  }

  private boolean hasNextPage = true;

  @Override
  public void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 16;
    int productsCount = this.arrayProducts.size();

    String url =
        "https://www.balaroti.com.br/api/catalog_system/pub/products/search/janela?map=ft&_from=" + productsCount + "&_to=" + (productsCount + 49);

    this.log("Página " + this.currentPage);
    JSONArray products = new JSONArray();

    try {
      products = new JSONArray(fetchGETString(url, null));
    } catch (JSONException e) {
      this.logError(CommonMethods.getStackTrace(e));
    }

    int pageSize = products.length();

    if (pageSize < 50) {
      this.hasNextPage = false;
    }

    if (products.length() > 0) {
      for (int i = 0; i < products.length(); i++) {
        JSONObject product = products.getJSONObject(i);

        String productUrl = crawlProductUrl(product);
        String internalPid = crawlInternalPid(product);

        saveDataProduct(null, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);

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

  @Override
  protected boolean hasNextPage() {
    return this.hasNextPage;
  }

  private String crawlInternalPid(JSONObject product) {
    String internalPid = null;

    if (product.has("productId")) {
      internalPid = product.getString("productId");
    }

    return internalPid;
  }

  private String crawlProductUrl(JSONObject product) {
    String urlProduct = null;

    if (product.has("link")) {
      urlProduct = product.getString("link");
    }

    return urlProduct;
  }
}
