package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.POSTFetcher;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class ColombiaRappiexitobogotaCrawler extends CrawlerRankingKeywords {

  public ColombiaRappiexitobogotaCrawler(Session session) {
    super(session);
  }

  @Override
  public void extractProductsFromCurrentPage() {
    this.pageSize = 20;
    this.log("Página " + this.currentPage);

    JSONObject search = fetchProductsFromAPI(br.com.lett.crawlernode.crawlers.corecontent.colombia.ColombiaRappiexitobogotaCrawler.STORES);

    if (search.has("hits") && search.getJSONArray("hits").length() > 0) {
      JSONArray products = search.getJSONArray("hits");

      if (this.totalProducts == 0) {
        setTotalProducts(search);
      }

      for (int i = 0; i < products.length(); i++) {
        JSONObject product = products.getJSONObject(i);

        String internalPid = crawlInternalPid(product);
        String internalId = crawlInternalId(product);
        String productUrl = crawlProductUrl(product, internalPid);

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

  @Override
  protected boolean hasNextPage() {
    return this.arrayProducts.size() < this.totalProducts;
  }

  protected void setTotalProducts(JSONObject search) {
    if (search.has("total_results") && search.get("total_results") instanceof Integer) {
      this.totalProducts = search.getInt("total_results");
      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String crawlInternalId(JSONObject product) {
    String internalId = null;

    if (product.has("id")) {
      internalId = product.getString("id");
    }

    return internalId;
  }

  private String crawlInternalPid(JSONObject product) {
    String internalPid = null;

    if (product.has("product_id")) {
      internalPid = product.getString("product_id");
    }

    return internalPid;
  }

  private String crawlProductUrl(JSONObject product, String internalPid) {
    String productUrl = null;

    if (product.has("store_type")) {
      productUrl = "https://www.rappi.com.co/search?store_type=" + product.get("store_type") + "&query=" + internalPid;
    }

    return productUrl;
  }

  private JSONObject fetchProductsFromAPI(List<String> storeIds) {
    JSONObject obj = new JSONObject();

    String payload =
        "{\"query\":\"" + this.location + "\",\"stores\":" + storeIds.toString() + ",\"store_type\":\"hiper\",\"page\":" + this.currentPage
            + ",\"size\":40,\"options\":{},\"helpers\":{\"home_type\":\"by_categories\",\"store_type_group\":\"market\",\"type\":\"by_categories\"}}";

    String url = "https://services.grability.rappi.com/api/search-client/search/v2/products?page=" + this.currentPage;

    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");

    String page = POSTFetcher.fetchPagePOSTWithHeaders(url, session, payload, null, 1, headers, DataFetcher.randUserAgent(), null);

    if (page.startsWith("{") && page.endsWith("}")) {
      try {
        obj = new JSONObject(page);
      } catch (Exception e) {
        Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
      }
    }

    return obj;
  }
}
