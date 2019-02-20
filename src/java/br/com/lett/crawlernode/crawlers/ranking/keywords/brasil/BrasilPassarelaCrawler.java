package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilPassarelaCrawler extends CrawlerRankingKeywords {

  private final String BASE_URL = "https://www.passarela.com.br/produto/";

  public BrasilPassarelaCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 300;

    this.log("Página " + this.currentPage);

    String key = this.keywordWithoutAccents.replaceAll(" ", "%20");
    String url = "https://www.passarela.com.br/ccstoreui/v1/search?Ntt=" + key
        + "&No=0&Nrpp=300&Nr=product.x_visibilidade:1,sku.availabilityStatus:INSTOCK&language=pt_BR&searchType=simple";
    this.log("Link onde são feitos os crawlers: " + url);

    JSONObject json = fetchJSONObject(url);
    json = json.has("searchEventSummary") ? json.getJSONObject("searchEventSummary") : new JSONObject();
    JSONArray products = json.has("resultsSummary") ? json.getJSONArray("resultsSummary") : new JSONArray();

    json = products.length() > 0 ? (JSONObject) products.get(0) : new JSONObject();
    products = json.has("records") ? json.getJSONArray("records") : new JSONArray();

    if (products.length() > 0) {
      if (this.totalProducts == 0) {
        setTotalProducts(json);
      }

      for (Object o : products) {
        JSONObject product = (JSONObject) o;

        if (product.has("record.id") && product.has("sku.listingId")) {
          String internalId = null;

          String internalPid = product.getString("sku.listingId");
          internalPid = internalPid.substring(0, internalPid.indexOf("--"));

          String productUrlEnding = product.getString("record.id");
          String productUrl = BASE_URL + productUrlEnding.replace("..", "/");

          saveDataProduct(internalId, internalPid, productUrl);

          this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
        }
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }

  protected void setTotalProducts(JSONObject json) {
    if (json.has("totalMatchingRecords")) {
      this.totalProducts = json.getInt("totalMatchingRecords");
      this.log("Total products: " + this.totalProducts);
    }
  }
}
