package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilDrogariapovaoCrawler extends CrawlerRankingKeywords {

  public BrasilDrogariapovaoCrawler(Session session) {
    super(session);
    super.fetchMode = FetchMode.APACHE;
  }

  private static final String HOME_PAGE = "http://www.drogariaspovao.com.br/";

  @Override
  protected void processBeforeFetch() {
    super.processBeforeFetch();
    this.log("Adding cookie ...");

    List<Cookie> cookiesResponse = fetchCookies(HOME_PAGE + "index.php");
    for (Cookie cookieResponse : cookiesResponse) {
      BasicClientCookie cookie = new BasicClientCookie(cookieResponse.getName(), cookieResponse.getValue());
      cookie.setDomain("www.drogariaspovao.com.br");
      cookie.setPath("/");
      cookies.add(cookie);
    }

    String arrapara = "[\"Carregar_Home\"]";

    StringBuilder payload = new StringBuilder();
    payload.append("origem=site");
    payload.append("&controle=navegacao");

    try {
      payload.append("&arrapara=" + URLEncoder.encode(arrapara, "UTF-8"));
    } catch (UnsupportedEncodingException e1) {
      logError(CommonMethods.getStackTrace(e1));
    }

    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=UTF-8");
    headers.put(HttpHeaders.REFERER, HOME_PAGE + "index.php");
    headers.put(HttpHeaders.ACCEPT_LANGUAGE, "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
    headers.put(HttpHeaders.ACCEPT, "application/json, text/javascript, */*; q=0.01");
    headers.put("X-Requested-With", "XMLHttpRequest");
    headers.put("Origin", HOME_PAGE);

    Request request = RequestBuilder.create().setUrl("http://www.drogariaspovao.com.br/ct/atende_geral.php").setPayload(payload.toString())
        .setHeaders(headers).setCookies(cookies).build();
    List<Cookie> loadPageCookies = this.dataFetcher.post(session, request).getCookies();

    for (Cookie cookieResponse : loadPageCookies) {
      BasicClientCookie cookie = new BasicClientCookie(cookieResponse.getName(), cookieResponse.getValue());
      cookie.setDomain("www.drogariaspovao.com.br");
      cookie.setPath("/");
      cookies.add(cookie);
    }
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 12;

    this.log("Página " + this.currentPage);

    JSONObject productsInfo = crawlProductInfo();
    JSONArray products = productsInfo.has("products") ? productsInfo.getJSONArray("products") : new JSONArray();

    if (products.length() > 0) {
      if (totalProducts == 0) {
        setTotalProducts(productsInfo);
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

  protected void setTotalProducts(JSONObject productsInfo) {
    if (productsInfo.has("total") && productsInfo.get("total") instanceof Integer) {
      this.totalProducts = productsInfo.getInt("total");
      this.log("Total: " + this.totalProducts);
    }
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

    StringBuilder payload = new StringBuilder();
    payload.append("origem=site");
    payload.append("&controle=navegacao");

    String arrapara = "[\"Busca_Produtos\",\"" + this.location + "\",\"" + this.currentPage + "\",\"\",\"\",\"\",\"\",\"\"]";

    try {
      payload.append("&arrapara=" + URLEncoder.encode(arrapara, "UTF-8"));
    } catch (UnsupportedEncodingException e1) {
      logError(CommonMethods.getStackTrace(e1));
      return products;
    }

    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=UTF-8");
    headers.put(HttpHeaders.REFERER, HOME_PAGE + "index.php");

    String page = fetchStringPOST("http://www.drogariaspovao.com.br/ct/atende_geral.php", payload.toString(), headers, cookies).trim();
    JSONArray infos = CrawlerUtils.stringToJsonArray(page);

    if (infos.length() > 1) {
      products.put("products", infos.getJSONArray(0)); // First position of array has products info
      products.put("total", infos.getInt(1));
    }

    return products;
  }
}
