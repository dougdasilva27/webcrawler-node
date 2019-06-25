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
    url.append("&appsEtag=ddb7a8f356b06ad1241e47a4a721406879e55466");

    JSONObject payload = new JSONObject();
    payload.put("operationName", "ListOffers");
    payload.put("variables", new JSONObject());

    JSONObject extensions = new JSONObject();
    extensions.put("variables", createVariablesBase64());

    JSONObject persistedQuery = new JSONObject();
    persistedQuery.put("version", "omnilogic.search@0.4.74");
    persistedQuery.put("sha256Hash", "c17d5f3289f56c6a5f9b143a63f8d6105cbc0ee61eba8bb58e2be31be643ef54");

    payload.put("extensions", extensions);
    payload.put("query",
        "query ListOffers($pageSize: Int, $sort: String, $metadata: [Metadatum], $searchPath: String, $text: String, $selectedSubstantive: String,"
            + " $source: String, $categories: [String], $clusters: [String], $priceRange: [Int], $sellers: [String], $ignoreSuggestions: Boolean)"
            + " @context(sender: \"omnilogic.search@0.4.74\") {\n search(pageSize: $pageSize, sort: $sort, metadata: $metadata, searchPath: $searchPath, "
            + "text: $text, selectedSubstantive: $selectedSubstantive, source: $source, categories: $categories, clusters: $clusters, priceRange: $priceRange, "
            + "sellers: $sellers, ignoreSuggestions: $ignoreSuggestions) @runtimeMeta(hash: \"c17d5f3289f56c6a5f9b143a63f8d6105cbc0ee61eba8bb58e2be31be643ef54\")"
            + " {\n store\n total\n selectedSubstantive\n suggestions {\n term\n values\n __typename\n }\n substantives\n metadata {\n name\n total\n values "
            + "{\n value\n total\n priceRange {\n min\n max\n __typename\n }\n __typename\n }\n __typename\n }\n sellers {\n value\n total\n priceRange "
            + "{\n min\n max\n __typename\n }\n __typename\n }\n categories {\n value\n total\n priceRange {\n min\n max\n __typename\n }\n __typename\n }\n "
            + "results {\n name\n url\n img\n price\n listPrice\n priceDiscount\n installments\n installmentValue\n sku\n label {\n name\n value\n __typename\n }\n "
            + "categories\n clusters\n __typename\n }\n query {\n name\n values\n __typename\n }\n __typename\n }\n}\n");

    this.log("Link onde são feitos os crawlers: " + url);

    Map<String, String> headers = new HashMap<>();
    headers.put("content-type", "application/json");

    Request request =
        RequestBuilder.create().setUrl(url.toString()).setCookies(cookies).setPayload(payload.toString()).mustSendContentEncoding(false).build();
    JSONObject response = CrawlerUtils.stringToJson(this.dataFetcher.post(session, request).getBody());

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
    search.put("clusters", JSONObject.NULL);
    search.put("sellers", JSONObject.NULL);

    JSONArray metadata = new JSONArray();
    JSONObject keywordJson = new JSONObject();
    keywordJson.put("name", "other_details");
    keywordJson.put("values", new JSONArray().put(this.keywordWithoutAccents));
    metadata.put(keywordJson);

    search.put("metadata", metadata);

    return Base64.getEncoder().encodeToString(search.toString().getBytes());
  }
}
