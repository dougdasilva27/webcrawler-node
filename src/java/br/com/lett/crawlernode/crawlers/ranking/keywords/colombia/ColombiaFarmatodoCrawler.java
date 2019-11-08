package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import com.google.gson.JsonParser;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;

public class ColombiaFarmatodoCrawler extends CrawlerRankingKeywords {

  public static final String PRODUCTS_API_URL = "https://vcojeyd2po-dsn.algolia.net/1/indexes/" +
      "products/query?x-algolia-agent=Algolia%20for%20vanilla%20JavaScript%203.22.1" +
      "&x-algolia-application-id=VCOJEYD2PO&x-algolia-api-key=e6f5ccbcdea95ff5ccb6fda5e92eb25c";

  public ColombiaFarmatodoCrawler(Session session) {
    super(session);
    super.fetchMode = FetchMode.FETCHER;
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 24;
    this.log("Página " + this.currentPage);

    JSONObject search = fetchProductsFromAPI();
    JSONArray arraySkus = search.has("hits") ? search.getJSONArray("hits") : new JSONArray();

    if (arraySkus.length() > 0) {

      if (this.totalProducts == 0) {
        setTotalProducts(search);
      }

      for (Object product : arraySkus) {
        JSONObject jsonSku = (JSONObject) product;
        String internalPid = JSONUtils.getStringValue(jsonSku, "id");
        String productUrl = "https://www.farmatodo.com.co/product/" + internalPid;

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

  private void setTotalProducts(JSONObject search) {
    this.totalProducts = JSONUtils.getIntegerValueFromJSON(search, "nbHits", 0);
    this.log("Total: " + this.totalProducts);
  }

  private JSONObject fetchProductsFromAPI() {
    JSONObject products = new JSONObject();

    String payload = "{\"params\":\"getRankingInfo=true&hitsPerPage=24&page=" + (this.currentPage-1)
        + "&query=" + this.keywordEncoded
        + "&facets=marca%2CCategor%C3%ADa%2CSubCategor%C3%ADa%2CfullPrice%2CsubscribeAndSave&filters=idStoreGroup%3A26\"}";

    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/x-www-form-urlencoded");
    headers.put("Accept-Encoding", "no");

    Request request = RequestBuilder.create().setUrl(PRODUCTS_API_URL).setCookies(cookies).setHeaders(headers).setPayload(payload)
        .mustSendContentEncoding(false).build();
    String page = this.dataFetcher.post(session, request).getBody();

    if (page.startsWith("{") && page.endsWith("}")) {
      try {
        // Using google JsonObject to get a JSONObject because this json can have a duplicate key.
        JSONObject result = new JSONObject(new JsonParser().parse(page).getAsJsonObject().toString());

        if (result.has("results") && result.get("results") instanceof JSONArray) {
          JSONArray results = result.getJSONArray("results");
          if (results.length() > 0 && results.get(0) instanceof JSONObject) {
            products = results.getJSONObject(0);
          }
        } else {
          products = result;
        }

      } catch (Exception e) {
        Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
      }
    }

    return products;
  }
}
