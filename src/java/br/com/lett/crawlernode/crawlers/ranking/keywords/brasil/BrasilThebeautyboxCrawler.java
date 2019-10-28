package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpHeaders;
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

public class BrasilThebeautyboxCrawler extends CrawlerRankingKeywords {

  public BrasilThebeautyboxCrawler(Session session) {
    super(session);
    super.fetchMode = FetchMode.APACHE;
  }

  private String keySHA256;
  private static final Integer API_VERSION = 1;
  private static final String SENDER = "tbb.main-theme@0.x";
  private static final String PROVIDER = "tbb.chaordic-graphql@0.x";

  @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);
    this.pageSize = 9;

    if (this.currentPage == 1) {
      this.keySHA256 = fetchSHA256Key();
    }

    JSONObject searchApi = fetchSearchApi();
    JSONArray products = searchApi.has("products") ? searchApi.getJSONArray("products") : new JSONArray();

    if (products.length() > 0) {

      if (this.totalProducts == 0) {
        setTotalProducts(searchApi);
      }

      for (Object object : products) {
        JSONObject product = (JSONObject) object;
        String productUrl = product.has("url") && !product.isNull("url") ? CrawlerUtils.completeUrl(product.get("url").toString(), "https",
            "www.beautybox.com.br") : null;
        String internalPid = product.has("id") && !product.isNull("id") ? product.get("id").toString() : null;

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
    this.totalProducts = CrawlerUtils.getIntegerValueFromJSON(data, "size", 0);
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
    url.append("https://www.beautybox.com.br/_v/segment/graphql/v1?");
    url.append("workspace=wfrc");
    url.append("&maxAge=short");
    url.append("&appsEtag=remove");
    url.append("&domain=store");
    url.append("&locale=pt-BR");
    url.append("&operationName=getSearchProducts");

    JSONObject extensions = new JSONObject();
    JSONObject persistedQuery = new JSONObject();

    persistedQuery.put("version", API_VERSION);
    persistedQuery.put("sha256Hash", this.keySHA256);
    persistedQuery.put("sender", SENDER);
    persistedQuery.put("provider", PROVIDER);
    extensions.put("variables", createVariablesBase64());
    extensions.put("persistedQuery", persistedQuery);

    StringBuilder payload = new StringBuilder();
    try {
      payload.append("&variables=" + URLEncoder.encode("{}", "UTF-8"));
      payload.append("&extensions=" + URLEncoder.encode(extensions.toString(), "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
    }

    url.append(payload.toString());
    this.log("Link onde são feitos os crawlers: " + url);

    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.CONTENT_TYPE, "application/json");

    Request request = RequestBuilder.create()
        .setUrl(url.toString())
        .setCookies(cookies)
        .setPayload(payload.toString())
        .build();

    JSONObject response = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

    if (response.has("data") && !response.isNull("data")) {
      JSONObject data = response.getJSONObject("data");

      if (data.has("searchProducts") && !data.isNull("searchProducts")) {
        searchApi = data.getJSONObject("searchProducts");
      }
    }

    return searchApi;
  }

  private String createVariablesBase64() {
    JSONObject search = new JSONObject();

    search.put("terms", this.keywordWithoutAccents.replace(" ", "-"));
    search.put("page", this.currentPage);
    search.put("sortBy", "ascPrice");
    search.put("resultsPerPage", 23);

    return Base64.getEncoder().encodeToString(search.toString().getBytes());
  }

  /**
   * This function accesses the search url and extracts a hash that will be required to access the
   * search api.
   * 
   * This hash is inside a key in json STATE. Ex:
   * 
   * "$ROOT_QUERY.productSearch({\"from\":0,\"hideUnavailableItems\":true,\"map\":\"ft\",\"orderBy\":\"OrderByTopSaleDESC\",\"query\":\"ACONDICIONADOR\",\"to\":19}) @runtimeMeta({\"hash\":\"0be25eb259af62c2a39f305122908321d46d3710243c4d4ec301bf158554fa71\"})"
   * 
   * Hash: 1d1ad37219ceb86fc281aa774971bbe1fe7656730e0a2ac50ba63ed63e45a2a3
   * 
   * @return
   */
  private String fetchSHA256Key() {
    String hash = "6d98ffeb45b2706036dedc2189973929924433d158b5af169beafa14d3f2898c";
    String url = "https://tbb.vteximg.com.br/_v/public/assets/v1/published/bundle/public/react/asset.min.js?"
        + "v=0&files=tbb.main-theme@0.16.0,40,4,6,10,12,15,28,32,Header,20,ChaordicShelfPage,Trends,ChaordicSugestionsPage"
        + "&files=vtex.store@2.69.0,common,StoreWrapper,DefaultChallenge,SearchWrapper,SearchContext"
        + "&files=vtex.store-components@3.79.0,common,Animation,Container&workspace=wfrc";

    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).mustSendContentEncoding(false).build();
    String response = this.dataFetcher.get(session, request).getBody().replace(" ", "");

    String searchProducts = CrawlerUtils.extractSpecificStringFromScript(response, "querygetSearchProducts(", false, "',", false);

    String firstIndexString = "@runtimeMeta(hash:";
    if (searchProducts.contains(firstIndexString) && searchProducts.contains(")")) {
      int x = searchProducts.indexOf(firstIndexString) + firstIndexString.length();
      int y = searchProducts.indexOf(')', x);

      hash = searchProducts.substring(x, y).replace("\"", "");
    }

    return hash;
  }
}
