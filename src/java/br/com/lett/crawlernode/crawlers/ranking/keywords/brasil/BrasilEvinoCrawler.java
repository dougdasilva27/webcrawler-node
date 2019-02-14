package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilEvinoCrawler extends CrawlerRankingKeywords {
  public String HOME_PAGE = "https://www.evino.com.br";

  public BrasilEvinoCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 51;

    String url = HOME_PAGE + "/api/product-list/slug/?q=" + this.keywordEncoded + "&perPage=51&page=" + this.currentPage;
    JSONObject productsJson = fetchJSONObject(url);
    JSONObject data = productsJson.has("data") ? productsJson.getJSONObject("data") : new JSONObject();
    JSONObject meta = data.has("meta") ? data.getJSONObject("meta") : new JSONObject();
    JSONArray products = data.has("products") ? data.getJSONArray("products") : new JSONArray();

    this.log("Página " + this.currentPage);

    this.log("Link onde são feitos os crawlers: " + url);

    if (productsJson.length() != 0) {

      if (this.totalProducts == 0) {
        setTotalProducts(meta);
      }

      for (Object object : products) {
        JSONObject product = (JSONObject) object;
        String productUrl = product.has("url") ? product.getString("url") : null;
        String internalId = product.has("sku") ? product.getString("sku") : null;

        productUrl = CrawlerUtils.completeUrl(productUrl, "https", "www.evino.com.br");
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

  private void setTotalProducts(JSONObject meta) {
    if (meta.has("total")) {
      this.totalProducts = meta.getInt("total");
    }
  }

}
