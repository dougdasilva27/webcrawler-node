package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilPassarelaCrawler extends CrawlerRankingKeywords {

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

    JSONObject json = getJSON(url);
    JSONArray products = json.has("records") ? json.getJSONArray("records") : new JSONArray();

    if (products.length() > 0) {
      if (this.totalProducts == 0) {
        setTotalProducts(json);
      }

      for (Object o : products) {
        JSONObject product = (JSONObject) o;

        if (product.has("record.id") && product.has("sku.listingId")) {
          String internalId = null;
          String internalPid = getInternalPid(product);
          String productUrl = getUrl(product);

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

  protected JSONObject getJSON(String url) {
    JSONObject json = fetchJSONObject(url);

    if (json.has("searchEventSummary")) {
      json = json.getJSONObject("searchEventSummary");

      if (json.has("resultsSummary")) {
        JSONArray products = json.getJSONArray("resultsSummary");

        if (products.length() > 0) {
          json = (JSONObject) products.get(0);
        }
      }
    }

    return json;
  }

  protected String getInternalPid(JSONObject json) {
    String internalPid = json.getString("sku.listingId");

    if (internalPid.contains("--")) {
      internalPid = internalPid.substring(0, internalPid.indexOf("--"));
    }

    return internalPid;
  }

  protected String getUrl(JSONObject json) {
    String productUrlEnding = json.getString("record.id");

    return "https://www.passarela.com.br/produto/" + productUrlEnding.replace("..", "/");
  }
}
