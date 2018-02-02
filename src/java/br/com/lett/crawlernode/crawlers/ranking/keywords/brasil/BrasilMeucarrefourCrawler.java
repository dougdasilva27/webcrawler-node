package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.GETFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.POSTFetcher;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilMeucarrefourCrawler extends CrawlerRankingKeywords {

  public BrasilMeucarrefourCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 60;

    this.log("Página " + this.currentPage);

    String url = "https://api.carrefour.com.br/mobile-food/v1/products/search?term=" + this.keywordEncoded + "&pageSize=2000";
    JSONObject productsInfo = crawlSearchApi(url);
    JSONArray products = productsInfo.has("data") ? productsInfo.getJSONArray("data") : new JSONArray();

    if (products.length() > 0) {
      if (totalProducts == 0) {
        this.totalProducts = productsInfo.has("total") ? productsInfo.getInt("total") : 0;
        this.log("Total: " + this.totalProducts);
      }

      for (int i = 0; i < products.length(); i++) {
        JSONObject product = products.getJSONObject(i);

        String internalId = crawlInternalId(product);
        String productUrl = crawlProductUrl(internalId);

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

  @Override
  protected boolean hasNextPage() {
    if (this.arrayProducts.size() < this.totalProducts) {
      return true;
    }

    return false;
  }

  private String crawlInternalId(JSONObject product) {
    String internalId = null;

    if (product.has("sku")) {
      internalId = product.getString("sku");
    }

    return internalId;
  }

  private String crawlProductUrl(String id) {
    return "https://api.carrefour.com.br/mobile-food/v1/product/" + id;
  }

  private JSONObject crawlSearchApi(String url) {
    Map<String, String> headers = new HashMap<>();
    headers.put("User-Agent", DataFetcher.randMobileUserAgent());

    // request with fetcher
    JSONObject fetcherResponse = POSTFetcher.fetcherRequest(url, null, headers, null, DataFetcher.GET_REQUEST, session);
    String page = null;

    if (fetcherResponse.has("response") && fetcherResponse.has("request_status_code") && fetcherResponse.getInt("request_status_code") >= 200
        && fetcherResponse.getInt("request_status_code") < 400) {
      JSONObject response = fetcherResponse.getJSONObject("response");

      if (response.has("body")) {
        page = response.getString("body");
      }
    } else {
      // normal request
      page = GETFetcher.fetchPageGETWithHeaders(session, session.getOriginalURL(), null, headers, 1);
    }

    return page == null ? new JSONObject() : new JSONObject(page);
  }
}
