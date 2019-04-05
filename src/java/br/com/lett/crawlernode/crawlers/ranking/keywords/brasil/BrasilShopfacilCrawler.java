package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

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
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilShopfacilCrawler extends CrawlerRankingKeywords {

  public BrasilShopfacilCrawler(Session session) {
    super(session);
    super.fetchMode = FetchMode.FETCHER;
  }


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
    url.append("&appsEtag=%228350902F07D18A2390FF776EF5DA435A%22");
    url.append("&operationName=ListOffers");
    url.append("&variables=%7B%7D");

    String substantive = fetchSubstantive();

    JSONObject search = new JSONObject();
    search.put("pageSize", this.productsLimit);
    search.put("sort", "score.desc");
    search.put("searchPath", "/busca/");
    search.put("selectedSubstantive", substantive == null ? JSONObject.NULL : substantive);
    search.put("source", "page");
    search.put("categories", JSONObject.NULL);
    search.put("clusters", JSONObject.NULL);
    search.put("sellers", JSONObject.NULL);

    JSONArray metadata = new JSONArray();

    if (substantive != null) {
      JSONObject substantiveJson = new JSONObject();
      substantiveJson.put("name", "substantive");
      substantiveJson.put("values", new JSONArray().put(substantive));
      metadata.put(substantiveJson);
    }

    JSONObject keywordJson = new JSONObject();
    keywordJson.put("name", "other_details");
    keywordJson.put("values", new JSONArray().put(this.keywordWithoutAccents));
    metadata.put(keywordJson);

    search.put("metadata", metadata);

    url.append("&extensions=")
        .append("%7B%22persistedQuery%22%3A%7B%22version%22%3A%22omnilogic.search%400.4.47%22%2C"
            + "%22sha256Hash%22%3A%22efbc48c69c0714f7ecde0a0d1a66b5b36dd7c30dc111400cf2b2114dcb7ad729%22%7D%2C" + "%22variables%22%3A%22")
        .append(Base64.getEncoder().encodeToString(search.toString().getBytes())).append("%3D%3D%22%7D");


    this.log("Link onde são feitos os crawlers: " + url);

    Request request = RequestBuilder.create().setUrl(url.toString()).setCookies(cookies).mustSendContentEncoding(false).build();
    JSONObject response = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

    if (response.has("data")) {
      JSONObject data = response.getJSONObject("data");

      if (data.has("search")) {
        searchApi = data.getJSONObject("search");
      }
    }

    return searchApi;
  }

  /**
   * This function crawl the redirect category of a keyword
   * 
   * We need access a api for get this information
   * 
   * @return
   */
  private String fetchSubstantive() {
    String substantiveLower = null;

    String url = "https://search.oppuz.com/api-v2/search";
    String payload = "{\"token\":\"81b329602db72ba0b698ad17cfbc318c\",\"sort\":\"score.des\",\"pageSize\":\"3\","
        + "\"query\":[{\"name\":\"other_details\",\"values\":[\"" + this.keywordWithoutAccents + "\"]}]}";

    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");

    Request request = RequestBuilder.create().setUrl(url.toString()).setCookies(cookies).setHeaders(headers).mustSendContentEncoding(false)
        .setPayload(payload).build();
    JSONObject response = CrawlerUtils.stringToJson(this.dataFetcher.post(session, request).getBody());

    if (response.has("substantives")) {
      JSONArray substantives = response.getJSONArray("substantives");

      if (substantives.length() > 0) {
        substantiveLower = substantives.get(0).toString().toLowerCase();
      }
    }

    return substantiveLower;
  }
}
