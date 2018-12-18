package br.com.lett.crawlernode.crawlers.ranking.keywords.bauru;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;

import br.com.lett.crawlernode.core.fetcher.methods.GETFetcher;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BauruConfiancaCrawler extends CrawlerRankingKeywords {

  public BauruConfiancaCrawler(Session session) {
    super(session);
  }

  private List<Cookie> cookies = new ArrayList<>();

  @Override
  public void processBeforeFetch() {
    this.log("Adding cookie...");

    BasicClientCookie cookie = new BasicClientCookie("current_website", "bauru");
    cookie.setDomain("www.confianca.com.br");
    cookie.setPath("/");
    this.cookies.add(cookie);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 24;

    this.log("Página " + this.currentPage);

    JSONObject search = crawlSearchApi();

    if (search.has("products") && search.getJSONArray("products").length() > 0) {
      JSONArray products = search.getJSONArray("products");

      if (this.totalProducts == 0) {
        setTotalProducts(search);
      }

      for (int i = 0; i < products.length(); i++) {
        JSONObject product = products.getJSONObject(i);

        String productUrl = crawlProductUrl(product);
        String internalPid = crawlInternalPid(product);

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

  protected void setTotalProducts(JSONObject search) {
    if (search.has("total_products") && search.get("total_products") instanceof Integer) {
      this.totalProducts = search.getInt("total_products");
      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String crawlInternalPid(JSONObject product) {
    String internalPid = null;

    if (product.has("product_id")) {
      internalPid = product.get("product_id").toString();
    }

    return internalPid;
  }

  private String crawlProductUrl(JSONObject product) {
    String urlProduct = null;

    if (product.has("url")) {
      urlProduct = product.getString("url");
    }

    return urlProduct;
  }

  private JSONObject crawlSearchApi() {
    String url = "https://www.confianca.com.br/bizrest/action/products?q=" + this.keywordEncoded + "&p=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    headers.put("Referer", "https://www.confianca.com.br/catalogsearch/result/?q=" + this.keywordEncoded + "&p=" + this.currentPage);

    return CrawlerUtils.stringToJson(GETFetcher.fetchPageGETWithHeaders(session, url, cookies, headers, 1));
  }
}
