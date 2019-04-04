package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilDrogariapovaoCrawler extends CrawlerRankingKeywords {

  public BrasilDrogariapovaoCrawler(Session session) {
    super(session);
  }

  private static final String HOME_PAGE = "http://www.drogariaspovao.com.br/";

  @Override
  protected void processBeforeFetch() {
    super.processBeforeFetch();
    this.log("Adding cookie ...");

    List<Cookie> cookiesResponse = fetchCookies(HOME_PAGE);

    for (Cookie cookieResponse : cookiesResponse) {
      BasicClientCookie cookie = new BasicClientCookie(cookieResponse.getName(), cookieResponse.getValue());
      cookie.setDomain("www.drogariaspovao.com.br");
      cookie.setPath("/");
      cookies.add(cookie);
    }

    String payload = "origem=site&controle=navegacao&arrapara=[\"Carregar_Home\"]";

    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/x-www-form-urlencoded");

    // Request para validar o cookie para requests de busca
    fetchCookiesPOST("http://www.drogariaspovao.com.br/ct/atende_geral.php", payload, headers, cookies);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 12;

    this.log("Página " + this.currentPage);

    JSONObject productsInfo = crawlProductInfo();
    JSONArray products = productsInfo.has("products") ? productsInfo.getJSONArray("products") : new JSONArray();

    // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
    if (products.length() > 0) {
      if (totalProducts == 0) {
        this.totalProducts = productsInfo.has("total") ? productsInfo.getInt("total") : 0;
        this.log("Total: " + this.totalProducts);
      }

      for (int i = 0; i < products.length(); i++) {
        JSONArray product = products.getJSONArray(i);

        if (product.length() > 1) {
          String internalPid = null;
          String internalId = product.getString(0);
          String productUrl = crawlProductUrl(product.getString(1).trim(), internalId);

          saveDataProduct(internalId, internalPid, productUrl);

          this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
          if (this.arrayProducts.size() == productsLimit) {
            break;
          }
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
    if (this.arrayProducts.size() < this.totalProducts) {
      return true;
    }

    return false;
  }

  private String crawlProductUrl(String name, String internalId) {
    String productUrl = null;

    if (!name.isEmpty()) {
      productUrl =
          HOME_PAGE + "detalhes_produto/" + internalId + "/" + name.toLowerCase().replace(" ", "-").replace("%", "").replace("&", "") + ".html";
    }

    return productUrl;
  }

  private JSONObject crawlProductInfo() {
    JSONObject products = new JSONObject();
    String payload = "origem=site&controle=navegacao&arrapara=[\"Busca_Produtos\",\"" + this.location + "\",\"" + this.currentPage
        + "\",\"\",\"\",\"\",\"\",\"\"]";

    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/x-www-form-urlencoded");
    headers.put("Referer", HOME_PAGE);

    String page = fetchStringPOST("http://www.drogariaspovao.com.br/ct/atende_geral.php", payload, headers, cookies).trim();

    if (page != null && page.startsWith("[") && page.endsWith("]")) {
      try {
        JSONArray infos = new JSONArray(page);

        if (infos.length() > 1) {
          products.put("products", infos.getJSONArray(0)); // First position of array has products info
          products.put("total", infos.getInt(1)); // First position of array has products info
        }
      } catch (JSONException e) {
        logError(CommonMethods.getStackTrace(e));
      }
    }

    return products;
  }
}
