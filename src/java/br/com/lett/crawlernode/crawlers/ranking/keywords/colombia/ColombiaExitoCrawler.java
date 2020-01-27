package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

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

public class ColombiaExitoCrawler extends CrawlerRankingKeywords {

  public ColombiaExitoCrawler(Session session) {
    super(session);
    super.fetchMode = FetchMode.APACHE;
  }

  private static final String HOME_PAGE = "https://www.exito.com/";
  private String keySHA256;
  private static final Integer API_VERSION = 1;
  private static final String SENDER = "vtex.search@0.x";
  private static final String PROVIDER = "vtex.search@0.x";

  @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);
    this.pageSize = 21;

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
    url.append("https://www.exito.com/_v/segment/graphql/v1?");
    url.append("workspace=master");
    url.append("&maxAge=short");
    url.append("&appsEtag=remove");
    url.append("&domain=store");
    url.append("&locale=es-CO");
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
    search.put("productOrigin", "VTEX");
    search.put("query", this.location);
    search.put("page", this.currentPage);
    search.put("attributePath", "");
    search.put("sort", "");
    search.put("count", this.pageSize);
    search.put("leap", false);

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
    // When sha256Hash is not found, this key below works (on 03/02/2020)
    String hash = "c934a0763acad40d4c1377dec290a91ea4b99d425c194c893360009dc5488c0e";
    String url = "https://exitocol.vtexassets.com/_v/public/assets/v1/published/bundle/public/react/asset.min.js"
        + "?v=1&files=exito.home-components@1.0.12,0,4,WrapperGalleryGrid,GalleryGird,CategoryBanner,Shelf,5,"
        + "CarouselByMoments,7,PatternLink,TitlePage&files=vtex.search@0.6.2,common,1,0,Autocomplete&files=vtex.shelf@1.35.1,"
        + "common,1,0,2,Shelf&files=exito.category-menu@3.0.6,common&workspace=master";

    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).mustSendContentEncoding(false).build();
    String response = this.dataFetcher.get(session, request).getBody().replace(" ", "");

    String searchProducts = CrawlerUtils.extractSpecificStringFromScript(response, "searchResult(", false, "',", false);
    String firstIndexString = "@runtimeMeta(hash:";
    if (searchProducts.contains(firstIndexString) && searchProducts.contains(")")) {
      int x = searchProducts.indexOf(firstIndexString) + firstIndexString.length();
      int y = searchProducts.indexOf(')', x);

      hash = searchProducts.substring(x, y).replace("\"", "");

    }

    return hash;
  }
}
