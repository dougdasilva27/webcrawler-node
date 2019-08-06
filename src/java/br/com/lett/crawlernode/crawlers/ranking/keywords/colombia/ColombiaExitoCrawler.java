package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

public class ColombiaExitoCrawler extends CrawlerRankingKeywords {

  public ColombiaExitoCrawler(Session session) {
    super(session);
    super.fetchMode = FetchMode.FETCHER;
  }

  private static final String HOME_PAGE = "https://www.exito.com/";
  private static final String SHA256_SEARCH = "fbbd2c497ef9cbdf36cf9d5ffc1da892dc6265a8b2cbecc6bc0be5347d04303e";
  private static final String API_VERSION = "1";
  private static final String SENDER = "vtex.store-resources@0.x";
  private static final String PROVIDER = "vtex.store-graphql@2.x";

  @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);
    this.pageSize = 20;

    JSONObject searchApi = fetchSearchApi();
    JSONArray products = searchApi.has("products") ? searchApi.getJSONArray("products") : new JSONArray();

    if (products.length() > 0) {

      if (this.totalProducts == 0) {
        setTotalProducts(searchApi);
      }

      for (Object object : products) {
        JSONObject product = (JSONObject) object;
        String productUrl = HOME_PAGE + (product.has("linkText") && !product.isNull("linkText") ? product.get("linkText").toString() : null) + "/p";
        String internalPid = product.has("productId") ? product.get("productId").toString() : null;

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

  private void setTotalProducts(JSONObject data) {
    this.totalProducts = CrawlerUtils.getIntegerValueFromJSON(data, "recordsFiltered", 0);
    this.log("Total da busca: " + this.totalProducts);
  }

  /**
   * This function request a api with a JSON encoded on BASE64
   * 
   * This json has informations like: pageSize, keyword and substantive {@link fetchSubstantive}
   * 
   * @return
   */
  private JSONObject fetchSearchApi() {
    JSONObject searchApi = new JSONObject();

    StringBuilder url = new StringBuilder();
    url.append("https://www.exito.com/_v/segment/graphql/v1?");
    url.append("workspace=master");
    url.append("&maxAge=short");
    url.append("&domain=store");
    url.append("&appsEtag=remove");

    StringBuilder payload = new StringBuilder();
    payload.append("&operationName=search");

    JSONObject extensions = new JSONObject();
    JSONObject persistedQuery = new JSONObject();

    persistedQuery.put("version", API_VERSION);
    persistedQuery.put("sha256Hash", SHA256_SEARCH);
    persistedQuery.put("sender", SENDER);
    persistedQuery.put("provider", PROVIDER);
    extensions.put("persistedQuery", persistedQuery);
    extensions.put("variables", createVariablesBase64());

    try {
      payload.append("&variables=" + URLEncoder.encode("{}", "UTF-8"));
      payload.append("&extensions=" + URLEncoder.encode(extensions.toString(), "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
    }

    url.append(payload.toString());
    this.log("Link onde são feitos os crawlers: " + url);

    Map<String, String> headers = new HashMap<>();
    headers.put("content-type", "application/json");

    Request request =
        RequestBuilder.create().setUrl(url.toString()).setCookies(cookies).setPayload(payload.toString()).mustSendContentEncoding(false).build();
    JSONObject response = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

    if (response.has("data") && !response.isNull("data")) {
      JSONObject data = response.getJSONObject("data");

      if (data.has("productSearch") && !data.isNull("productSearch")) {
        searchApi = data.getJSONObject("productSearch");
      }
    }

    return searchApi;
  }

  private String createVariablesBase64() {
    int capturedProductsNumber = this.arrayProducts.size();

    JSONObject search = new JSONObject();
    search.put("withFacets", true);
    search.put("hideUnavailableItems", true);
    search.put("query", this.location);
    search.put("orderBy", "OrderByReleaseDateDESC");
    search.put("from", capturedProductsNumber);
    search.put("to", capturedProductsNumber + (this.pageSize - 1));
    search.put("facetQuery", this.location);

    return Base64.getEncoder().encodeToString(search.toString().getBytes());
  }
}
