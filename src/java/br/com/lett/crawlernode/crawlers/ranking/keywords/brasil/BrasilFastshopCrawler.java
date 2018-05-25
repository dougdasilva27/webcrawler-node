package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilFastshopCrawler extends CrawlerRankingKeywords {

  public BrasilFastshopCrawler(Session session) {
    super(session);
  }


  @Override
  protected void extractProductsFromCurrentPage() {

    this.log("Página " + this.currentPage);

    String apiUrl = "https://fastshop-v6.neemu.com/searchapi/v3/search?apiKey=fastshop-v6&secretKey=7V0dpc8ZFxwCRyCROLZ8xA%253D%253D&terms="
        + this.keywordWithoutAccents.replace(" ", "%20") + "&resultsPerPage=9&page=" + this.currentPage;

    this.log("Link onde são feitos os crawlers: " + apiUrl);
    Map<String, String> headers = new HashMap<>();
    headers.put("origin", "https://www.fastshop.com.br");

    String json = fetchGetFetcher(apiUrl, null, headers, null);

    JSONObject api = new JSONObject(json);
    extractProductFromJSON(api);
  }

  private void extractProductFromJSON(JSONObject api) {
    if (api.has("products")) {
      if (this.totalProducts == 0) {
        setTotalProductsFromJSON(api);
      }

      JSONArray products = api.getJSONArray("products");
      for (int i = 0; i < products.length(); i++) {
        JSONObject product = products.getJSONObject(i);

        String internalPid = crawlinternalPid(product);
        String productUrl = crawlProductUrlFromJson(product, internalPid);
        JSONArray internalIds = crawlInternalId(product);

        this.position++;

        for (int j = 0; j < internalIds.length(); j++) {
          String internalId = internalIds.getString(j);
          saveDataProduct(internalId, internalPid, productUrl, this.position);

          this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
        }

        if (this.arrayProducts.size() == productsLimit) {
          break;
        }
      }
    }
  }

  @Override
  protected boolean hasNextPage() {
    return this.arrayProducts.size() < this.totalProducts;
  }

  private void setTotalProductsFromJSON(JSONObject api) {
    if (api.has("size") && api.get("size") instanceof Integer) {
      this.totalProducts = api.getInt("size");
      this.log("Total de produtos: " + this.totalProducts);
    }
  }

  private JSONArray crawlInternalId(JSONObject product) {
    JSONArray internalIds = new JSONArray();

    if (product.has("details")) {
      JSONObject details = product.getJSONObject("details");

      if (details.has("catalogEntryId")) {
        internalIds = details.getJSONArray("catalogEntryId");
      }
    }

    return internalIds;
  }

  private String crawlinternalPid(JSONObject product) {
    String pid = null;

    if (product.has("id")) {
      pid = product.get("id").toString();
    }

    return pid;
  }

  private String crawlProductUrlFromJson(JSONObject product, String pid) {
    StringBuilder productUrl = new StringBuilder();

    if (pid != null && product.has("url")) {
      productUrl.append("https://www.fastshop.com.br/web/p/d/");
      productUrl.append(pid + "/");
      productUrl.append(CommonMethods.getLast(product.get("url").toString().split("/")));
    } else {
      return null;
    }

    return productUrl.toString();
  }

}
