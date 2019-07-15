package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

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

public class BrasilShopfacilCrawler extends CrawlerRankingKeywords {

  public BrasilShopfacilCrawler(Session session) {
    super(session);
    super.fetchMode = FetchMode.FETCHER;
  }


  private static final String SHA256_SEARCH = "2bbb28a7eef7c8c5ed321556479120eddbc1475940765942520a0f112ce486a8";
  private static final String API_VERSION = "omnilogic.search@0.4.92";

  @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);
    this.pageSize = 24;

    JSONObject searchApi = fetchSearchApi();
    JSONArray products = searchApi.has("results") ? searchApi.getJSONArray("results") : new JSONArray();

    if (products.length() > 0) {

      if (this.totalProducts == 0) {
        setTotalProducts(searchApi);
      }

      for (Object object : products) {
        JSONObject product = (JSONObject) object;
        String productUrl = product.has("url") ? CrawlerUtils.completeUrl(product.get("url").toString(), "https", "www.shopfacil.com.br") : null;
        String internalId = product.has("sku") ? product.get("sku").toString() : null;

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

  private void setTotalProducts(JSONObject data) {
    if (data.has("total") && data.get("total") instanceof Integer) {
      this.totalProducts = data.getInt("total");
      this.log("Total da busca: " + this.totalProducts);
    }
  }

  @Override
  protected boolean hasNextPage() {
    return false;
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
    url.append("https://www.shopfacil.com.br/api/io/_v/public/graphql/v1?");
    url.append("workspace=master");
    url.append("&maxAge=short");
    url.append("&appsEtag=ddb7a8f356b06ad1241e47a4a721406879e55466");

    StringBuilder payload = new StringBuilder();
    payload.append("&operationName=ListOffers");

    JSONObject extensions = new JSONObject();
    JSONObject persistedQuery = new JSONObject();

    persistedQuery.put("version", API_VERSION);
    persistedQuery.put("sha256Hash", SHA256_SEARCH);
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

    if (response.has("data")) {
      JSONObject data = response.getJSONObject("data");

      if (data.has("search")) {
        searchApi = data.getJSONObject("search");
      }
    }

    return searchApi;
  }

  private String createVariablesBase64() {
    JSONObject search = new JSONObject();
    search.put("pageSize", this.productsLimit);
    search.put("sort", "score.desc");
    search.put("searchPath", "/busca/");
    search.put("selectedSubstantive", "none");
    search.put("source", "page");
    search.put("categories", JSONObject.NULL);
    search.put("priceRange", JSONObject.NULL);
    search.put("priceDiscount", JSONObject.NULL);
    search.put("clusters", JSONObject.NULL);
    search.put("sellers", JSONObject.NULL);
    search.put("ignoreSuggestions", false);

    JSONArray metadata = new JSONArray();
    JSONObject keywordJson = new JSONObject();
    keywordJson.put("name", "other_details");
    keywordJson.put("values", new JSONArray().put(this.keywordWithoutAccents));
    metadata.put(keywordJson);

    search.put("metadata", metadata);

    return Base64.getEncoder().encodeToString(search.toString().getBytes());
  }
}
