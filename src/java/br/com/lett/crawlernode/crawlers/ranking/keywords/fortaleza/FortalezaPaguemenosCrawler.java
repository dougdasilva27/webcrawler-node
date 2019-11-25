package br.com.lett.crawlernode.crawlers.ranking.keywords.fortaleza;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

public class FortalezaPaguemenosCrawler extends CrawlerRankingKeywords {

  public FortalezaPaguemenosCrawler(Session session) {
    super(session);
    super.fetchMode = FetchMode.APACHE;
  }

  private String keySHA256;
  private static final Integer API_VERSION = 1;
  private static final String SENDER = "biggy.biggy-search@2.x";
  private static final String PROVIDER = "biggy.biggy-search@2.x";

  @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);
    this.pageSize = 20;

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
        String productUrl = product.has("url") && !product.isNull("url") ? CrawlerUtils.completeUrl(product.getString("url"), "https",
            "www.paguemenos.com.br") : null;
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
    this.totalProducts = CrawlerUtils.getIntegerValueFromJSON(data, "total", 0);
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
    url.append("https://www.paguemenos.com.br/_v/public/graphql/v1?");
    url.append("workspace=master");
    url.append("&maxAge=long");
    url.append("&appsEtag=remove");
    url.append("&domain=store");
    url.append("&locale=pt-BR");
    url.append("&operationName=searchResult");


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

      if (data.has("searchResult") && !data.isNull("searchResult")) {
        searchApi = data.getJSONObject("searchResult");
      }
    }

    return searchApi;
  }

  private String createVariablesBase64() {
    JSONObject search = new JSONObject();
    search.put("query", this.location);
    search.put("store", "paguemenos");
    search.put("count", 12);
    search.put("page", this.currentPage);
    search.put("sort", JSONObject.NULL);

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
    // When sha256Hash is not found, this key below works (on 12/09/2019)
    String hash = "5aaa9669bc6d238a82001a056eb5409b5a666480c0f43a6c4a6202eefc57bfcc";
    String url = "https://www.paguemenos.com.br/search?query=" + this.keywordEncoded;

    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).mustSendContentEncoding(false).build();
    String response = this.dataFetcher.get(session, request).getBody();

    if (response != null) {
      Document doc = Jsoup.parse(response);
      JSONObject stateJson = CrawlerUtils.selectJsonFromHtml(doc, "script", "__STATE__ =", null, false, true);

      for (String key : stateJson.keySet()) {
        String firstIndexString = "@runtimeMeta(";
        String keyIdentifier = "$ROOT_QUERY.productSearch";

        if (key.contains(firstIndexString) && key.contains(keyIdentifier) && key.endsWith(")")) {
          int x = key.indexOf(firstIndexString) + firstIndexString.length();
          int y = key.indexOf(')', x);

          JSONObject hashJson = CrawlerUtils.stringToJson(key.substring(x, y));

          if (hashJson.has("hash") && !hashJson.isNull("hash")) {
            hash = hashJson.get("hash").toString();
          }

          break;
        }
      }
    }

    return hash;
  }
}
