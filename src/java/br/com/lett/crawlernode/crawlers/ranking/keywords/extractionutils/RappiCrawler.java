package br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class RappiCrawler extends CrawlerRankingKeywords {

  private static final String STORES_API_URL = "https://services.rappi.com.br/api/search-client/search/v2/products?page=";
  public static final String PRODUCTS_API_URL = "https://services.rappi.com.br/chewbacca/search/v2/products?page=1";
  private List<String> markets;

  public RappiCrawler(Session session, List<String> markets) {
    super(session);
    this.markets = markets;
  }

  @Override
  public void extractProductsFromCurrentPage() {
    this.pageSize = 20;
    this.log("Página " + this.currentPage);

    JSONObject search;

    search = fetchProductsFromAPI("market", markets);

    // se obter 1 ou mais links de produtos e essa página tiver resultado
    if (search.has("hits") && search.getJSONArray("hits").length() > 0) {
      JSONArray products = search.getJSONArray("hits");

      // se o total de busca não foi setado ainda, chama a função para
      if (this.totalProducts == 0) {
        setTotalProducts(search);
      }

      for (int i = 0; i < products.length(); i++) {
        JSONObject product = products.getJSONObject(i);

        String internalPid = crawlInternalPid(product);
        String internalId = crawlInternalId(product);
        String productUrl = crawlProductUrl(product, internalId);

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

  @Override
  protected boolean hasNextPage() {
    return this.arrayProducts.size() < this.totalProducts;
  }

  protected void setTotalProducts(JSONObject search) {
    if (search.has("total_results") && search.get("total_results") instanceof Integer) {
      this.totalProducts = CrawlerUtils.getIntegerValueFromJSON(search, "total_results", 0);
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

    if (product.has("product_id") && !product.isNull("product_id")) {
      internalPid = product.get("product_id").toString();
    }

    return internalPid;
  }

  private String crawlProductUrl(JSONObject product, String internalId) {
    String productUrl = null;

    if (product.has("store_type")) {
      productUrl = "https://www.rappi.com.br/product/" + internalId + "?store_type=" + product.get("store_type");
    }

    return productUrl;
  }

  private JSONObject fetchProductsFromAPI(String storeType, List<String> storeIds) {
    String payload =
        "{\"query\":\"" + this.location + "\",\"stores\":" + storeIds.toString() + "," + "\"helpers\":{\"type\":\"by_products\",\"storeType\":\""
            + storeType + "\"},\"page\":" + this.currentPage + ",\"store_type\":\"" + storeType + "\",\"options\":{}}";

    String url = STORES_API_URL + this.currentPage;

    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");

    Request request =
        RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).setPayload(payload).mustSendContentEncoding(false).build();
    return CrawlerUtils.stringToJson(this.dataFetcher.post(session, request).getBody());
  }

}

