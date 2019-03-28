package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class ArgentinaDiscoCrawler extends CrawlerRankingKeywords {

  public ArgentinaDiscoCrawler(Session session) {
    super(session);
  }


  @Override
  protected void processBeforeFetch() {
    this.cookies = CrawlerUtils.fetchCookiesFromAPage("https://www.disco.com.ar/Comprar/Home.aspx?", null, "www.disco.com.ar", "/", cookies, session,
        null, dataFetcher);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);

    JSONObject jsonSearch = crawlProductsApi(CommonMethods.encondeStringURLToISO8859(this.location, logger, session));
    JSONArray products = new JSONArray();

    if (jsonSearch.has("ResultadosBusquedaLevex")) {
      products = jsonSearch.getJSONArray("ResultadosBusquedaLevex");
    }

    // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
    if (products.length() >= 1) {
      // se o total de busca não foi setado ainda, chama a função para setar
      if (this.totalProducts == 0) {
        this.totalProducts = products.length();
      }

      for (int i = 0; i < products.length(); i++) {
        JSONObject product = products.getJSONObject(i);

        // InternalPid
        String internalPid = crawlInternalPid(product);

        // InternalId
        String internalId = crawlInternalId(product);

        // Url do produto
        String urlProduct = crawlProductUrl(product);

        saveDataProduct(internalId, internalPid, urlProduct);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
        if (this.arrayProducts.size() == productsLimit) {
          break;
        }

      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    // número de produtos por página do market
    this.pageSize = this.totalProducts;

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }

  @Override
  protected boolean hasNextPage() {
    // Nesse market não existe proximas páginas
    return false;
  }

  private String crawlInternalId(JSONObject product) {
    String internalId = null;

    if (product.has("IdArticulo")) {
      internalId = product.getString("IdArticulo");
    }

    return internalId;
  }

  private String crawlInternalPid(JSONObject product) {
    String internalPid = null;

    return internalPid;
  }

  private String crawlProductUrl(JSONObject product) {
    String productUrl = null;

    if (product.has("DescripcionArticulo")) {
      String name = product.getString("DescripcionArticulo");

      productUrl = "https://www.disco.com.ar/Comprar/Home.aspx?#_atCategory=false&_atGrilla=true&_query="
          + CommonMethods.encondeStringURLToISO8859(name, logger, session);
    }

    return productUrl;
  }

  /**
   * Crawl api of search when probably has only one product
   * 
   * @param url
   * @return
   */
  private JSONObject crawlProductsApi(String keyword) {
    JSONObject json = new JSONObject();

    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");

    String urlSearch = "https://www.disco.com.ar/Comprar/HomeService.aspx/ObtenerArticulosPorDescripcionMarcaFamiliaLevex";
    String payload = "{IdMenu:\"\",textoBusqueda:\"" + keyword + "\","
        + " producto:\"\", marca:\"\", pager:\"\", ordenamiento:0, precioDesde:\"\", precioHasta:\"\"}";

    this.log("Payload: " + payload);
    this.log("Cookies: " + this.cookies);

    String jsonString = fetchStringPOST(urlSearch, payload, headers, this.cookies);

    if (jsonString != null) {
      if (jsonString.startsWith("{")) {
        json = parseJsonLevex(new JSONObject(jsonString));
      }
    }

    return json;
  }

  private JSONObject parseJsonLevex(JSONObject json) {
    JSONObject jsonD = new JSONObject();

    if (json.has("d")) {
      String dParser = JSONObject.stringToValue(json.getString("d")).toString();
      jsonD = new JSONObject(dParser);
    }

    return jsonD;
  }
}
