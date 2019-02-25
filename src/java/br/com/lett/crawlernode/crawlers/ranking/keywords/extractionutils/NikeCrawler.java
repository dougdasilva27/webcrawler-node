package br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils;

import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class NikeCrawler extends CrawlerRankingKeywords {

  public NikeCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 60;

    this.log("Página " + this.currentPage);

    String url = buildUrl();
    this.log("Url: " + url);

    JSONObject json = CrawlerUtils.stringToJson(DataFetcher.fetchPageWithHttpURLConnectionUsingStormProxies(url, new HashMap<>(), session, 1));
    if (this.totalProducts == 0) {
      setTotalProducts(json);
    }

    if (json.has("foundResults") && json.getBoolean("foundResults") && json.has("sections")) {
      JSONArray array = json.getJSONArray("sections");

      if (array.length() > 0) {
        json = array.getJSONObject(0);

        if (json.has("items")) {
          array = json.getJSONArray("items");

          for (Object o : array) {
            JSONObject product = (JSONObject) o;

            String internalPid = null;
            String internalId = null;
            String productUrl = product.has("pdpUrl") ? product.getString("pdpUrl") : null;

            saveDataProduct(internalId, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);

            if (this.arrayProducts.size() == productsLimit) {
              break;
            }
          }
        }
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }

  /**
   * Create the request url. <br>
   * <b>OBS:</b> Method must be implemented on child.
   * 
   * @return
   */
  protected String buildUrl() {
    return null;
  }

  protected void setTotalProducts(JSONObject json) {
    if (json.has("trackingData")) {
      String subJson = json.getString("trackingData");

      json = new JSONObject(subJson);
      if (json.has("response")) {
        json = json.getJSONObject("response");

        if (json.has("totalResults")) {
          this.totalProducts = json.getInt("totalResults");

          this.log("Total da busca: " + this.totalProducts);
        }
      }
    }
  }

}
