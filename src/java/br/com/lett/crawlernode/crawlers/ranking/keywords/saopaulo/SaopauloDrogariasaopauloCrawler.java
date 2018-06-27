package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class SaopauloDrogariasaopauloCrawler extends CrawlerRankingKeywords {

  public SaopauloDrogariasaopauloCrawler(Session session) {
    super(session);
  }

  private JSONArray productsJSONArray;

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 15;
    this.log("Página " + this.currentPage);

    this.productsJSONArray = new JSONArray();
    int index = this.arrayProducts.size();

    String url = "https://www.drogariasaopaulo.com.br/api/catalog_system/pub/products/search/" + this.keywordEncoded + "?_from=" + index + "&_to="
        + (index + 14) + "&O=OrderByReleaseDateDESC";
    this.log("Link onde são feitos os crawlers: " + url);

    String response = fetchGETString(url, null);

    if (response.startsWith("[") && response.endsWith("]")) {
      productsJSONArray = new JSONArray(response);
    }

    if (productsJSONArray.length() > 0) {
      for (Object o : productsJSONArray) {
        JSONObject item = (JSONObject) o;
        String internalPid = crawlInternalPid(item);
        String productUrl = crawlProductUrl(item);

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
    return this.productsJSONArray.length() == 15;
  }

  private String crawlProductUrl(JSONObject item) {
    String productUrl = null;

    if (item.has("link")) {
      productUrl = item.getString("link");
    }

    return productUrl;
  }

  private String crawlInternalPid(JSONObject item) {
    String internalPid = null;

    if (item.has("productId")) {
      internalPid = item.getString("productId");
    }

    return internalPid;
  }
}
