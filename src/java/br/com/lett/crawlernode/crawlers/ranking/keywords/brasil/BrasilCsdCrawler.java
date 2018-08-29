package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilCsdCrawler extends CrawlerRankingKeywords {

  public BrasilCsdCrawler(Session session) {
    super(session);
  }

  private static final String HOME_PAGE =
      "https://www.sitemercado.com.br/supermercadoscidadecancao/londrina-loja-londrina-19-rodocentro-avenida-tiradentes/";
  private String apiVersion = "true";

  @Override
  public void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);
    JSONObject search = crawlProductInfo();

    // se obter 1 ou mais links de produtos e essa página tiver resultado
    if (search.has("products") && search.getJSONArray("products").length() > 0) {
      JSONArray products = search.getJSONArray("products");

      this.totalProducts = products.length();
      this.log("Total da busca: " + this.totalProducts);

      for (int i = 0; i < products.length(); i++) {
        JSONObject product = products.getJSONObject(i);

        // Url do produto
        String productUrl = crawlProductUrl(product);

        // InternalPid
        String internalPid = crawlInternalPid(product);

        // InternalId
        String internalId = crawlInternalId(product);

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
    return false;
  }

  private String crawlInternalId(JSONObject product) {
    String internalId = null;

    if (product.has("idLojaProduto")) {
      internalId = product.get("idLojaProduto").toString();
    }

    return internalId;
  }

  private String crawlInternalPid(JSONObject product) {
    String internalPid = null;

    if (product.has("idProduct")) {
      internalPid = product.get("idProduct").toString();
    }

    return internalPid;
  }

  private String crawlProductUrl(JSONObject product) {
    String productUrl = null;

    if (product.has("url")) {
      productUrl = product.getString("url");

      if (!productUrl.contains("sitemercado")) {
        productUrl = HOME_PAGE + "/" + productUrl;
      }
    }

    return productUrl;
  }

  private JSONObject crawlProductInfo() {
    JSONObject products = new JSONObject();
    String payload = "{phrase: \"" + this.keywordWithoutAccents + "\"}";

    Map<String, String> headers = new HashMap<>();
    headers.put("referer", HOME_PAGE);
    headers.put("sm-b2c",
        "{\"platform\":1,\"lojaName\":\"londrina-loja-londrina-19-rodocentro-avenida-tiradentes\",\"redeName\":\"supermercadoscidadecancao\"}");
    headers.put("sm-mmc", this.apiVersion);
    headers.put("accept", "application/json, text/plain, */*");
    headers.put("Content-Type", "application/json");

    String page = fetchStringPOST("https://www.sitemercado.com.br/core/api/v1/b2c/product/loadSearch", payload, headers, null);

    if (page == null || page.trim().isEmpty()) {
      this.apiVersion = new SimpleDateFormat("yyyy.MM.dd").format(new Date()) + "-0";
      headers.put("sm-mmc", this.apiVersion);
      page = fetchStringPOST("https://www.sitemercado.com.br/core/api/v1/b2c/product/loadSearch", payload, headers, null);
    }

    if (page != null && page.startsWith("{") && page.endsWith("}")) {
      try {
        products = new JSONObject(page);
      } catch (JSONException e) {
        logError(CommonMethods.getStackTrace(e));
      }
    }

    return products;
  }
}
