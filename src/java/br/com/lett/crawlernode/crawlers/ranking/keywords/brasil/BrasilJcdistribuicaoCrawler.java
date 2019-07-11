package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

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
import br.com.lett.crawlernode.util.CrawlerUtils;

/**
 * Date: 10/07/2019
 * 
 * @author gabriel
 *
 */
public class BrasilJcdistribuicaoCrawler extends CrawlerRankingKeywords {

  private static final String API_URL = "https://api.heylabs.io/graphql";
  private static final String SUBSIDIARY_ID = "11";
  private static final String COMPANY_ID = "6486bb697694a5aed9c0b7729734372f";

  public BrasilJcdistribuicaoCrawler(Session session) {
    super(session);
    super.fetchMode = FetchMode.FETCHER;
  }

  @Override
  public void extractProductsFromCurrentPage() {
    this.pageSize = 20;
    this.log("Página " + this.currentPage);

    JSONObject search = fetchProductsFromAPI();

    if (search.has("items") && search.getJSONArray("items").length() > 0) {
      JSONArray products = search.getJSONArray("items");

      if (this.totalProducts == 0) {
        setTotalProducts(search);
      }

      for (int i = 0; i < products.length(); i++) {
        JSONObject product = products.getJSONObject(i);

        String internalPid = crawlInternalPid(product);
        String internalId = crawlInternalId(product);
        String productUrl = crawlProductUrl(product);

        saveDataProduct(internalId, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);

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

  protected void setTotalProducts(JSONObject search) {
    if (search.has("pagination") && search.get("pagination") instanceof JSONObject) {
      this.totalProducts = CrawlerUtils.getIntegerValueFromJSON(search.getJSONObject("pagination"), "total", 0);
      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String crawlInternalId(JSONObject product) {
    String internalId = null;

    if (product.has("id") && !product.isNull("id")) {
      internalId = product.get("id").toString();
    }

    return internalId;
  }

  private String crawlInternalPid(JSONObject product) {
    String internalPid = null;

    if (product.has("factoryCode") && !product.isNull("factoryCode")) {
      internalPid = product.get("factoryCode").toString();
    }

    return internalPid;
  }

  // url must be in this format:
  // https://jcdistribuicao.maxb2b.com.br/{subsidiaryId}/search/{barCode}?id={cpde}
  private String crawlProductUrl(JSONObject product) {
    String productUrl = null;

    if (product.has("barCode") && !product.isNull("barCode") && product.has("code") && !product.isNull("code")) {
      productUrl = "https://jcdistribuicao.maxb2b.com.br/" + SUBSIDIARY_ID +
          "/search/" + product.get("barCode") + "?id=" + product.get("code");
    }

    return productUrl;
  }

  private JSONObject fetchProductsFromAPI() {
    JSONObject products = new JSONObject();

    JSONObject payload = new JSONObject();
    payload.put("operationName", "products");

    // this field was extracted on api call, this will not change
    payload.put("query", "query products($subsidiaryId: String, $campaignId: String, $sectionId: String, $subsectionId: String, $offset: Int, "
        + "$limit: Int, $search: String, $paymentPlanId: String, $billingTypeId: String, $inCampaign: Boolean, $supplierId: String) {\n  "
        + "products(subsidiaryId: $subsidiaryId, campaignId: $campaignId, sectionId: $sectionId, subsectionId: $subsectionId, offset: $offset, "
        + "limit: $limit, search: $search, paymentPlanId: $paymentPlanId, billingTypeId: $billingTypeId, inCampaign: $inCampaign, supplierId: "
        + "$supplierId) {\n    items {\n      ...BasicProduct\n      __typename\n    }\n    pagination {\n      ...BasicPagination\n      __typename\n"
        + "    }\n    __typename\n  }\n}\n\nfragment BasicProduct on Product {\n  id\n  code\n  barCode\n  isAvailable\n  name\n  image\n  price\n  "
        + "formerPrice\n  pricePerUnit\n  showStock\n  validateStock\n  quantityInStock\n  wrapper\n  unit\n  multiplier\n  factoryCode\n  originalCode\n  "
        + "hasCampaign\n  campaignMessage\n  campaignCode\n  __typename\n}\n\nfragment BasicPagination on Pagination {\n  total\n  "
        + "offset\n  limit\n  __typename\n}\n");

    JSONObject variables = new JSONObject();
    variables.put("limit", 20);
    variables.put("offset", this.arrayProducts.size());
    variables.put("search", this.location);
    variables.put("subsidiaryId", SUBSIDIARY_ID);

    payload.put("variables", variables);

    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
    headers.put("CompanyId", COMPANY_ID);

    Request request = RequestBuilder.create()
        .setUrl(API_URL)
        .setCookies(cookies)
        .setHeaders(headers)
        .setPayload(payload.toString())
        .mustSendContentEncoding(false)
        .build();

    JSONObject apiResponse = CrawlerUtils.stringToJson(this.dataFetcher.post(session, request).getBody());

    if (apiResponse.has("data") && apiResponse.get("data") instanceof JSONObject) {
      JSONObject data = apiResponse.getJSONObject("data");

      if (data.has("products") && data.get("products") instanceof JSONObject) {
        products = data.getJSONObject("products");
      }
    }

    return products;
  }
}
