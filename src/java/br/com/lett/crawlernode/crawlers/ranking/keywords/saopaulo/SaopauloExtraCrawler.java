package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.GPACrawler;
import br.com.lett.crawlernode.util.CommonMethods;

public class SaopauloExtraCrawler extends CrawlerRankingKeywords {

  public SaopauloExtraCrawler(Session session) {
    super(session);
  }

  private List<Cookie> cookies = new ArrayList<>();

  @Override
  protected void processBeforeFetch() {
    if (this.cookies.isEmpty()) {
      BasicClientCookie cookie = new BasicClientCookie("ep.selected_store", GPACrawler.SAO_PAULO_STORE_ID_EXTRA);
      cookie.setDomain(".deliveryextra.com");
      cookie.setPath("/");
      cookie.setExpiryDate(new Date(System.currentTimeMillis() + 604800000L + 604800000L));

      this.cookies.add(cookie);
    }
  }

  private String keyword = this.keywordEncoded;

  @Override
  public void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 0;

    this.log("Página " + this.currentPage);
    JSONObject search = crawlSearchApi();

    if (search.has("results") && search.getJSONArray("results").length() > 0) {
      JSONArray products = search.getJSONArray("results");

      if (this.totalProducts == 0) {
        setTotalProducts(search);
      }

      for (int i = 0; i < products.length(); i++) {
        JSONObject product = products.getJSONObject(i);

        // Url do produto
        String productUrl = crawlProductUrl(product);

        // InternalPid
        String internalPid = crawlInternalPid(product);

        // InternalId
        String internalId = crawlInternalId(productUrl);

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
    if (search.has("result_meta")) {
      JSONObject resultMeta = search.getJSONObject("result_meta");

      if (resultMeta.has("total")) {
        this.totalProducts = resultMeta.getInt("total");
        this.log("Total da busca: " + this.totalProducts);
      }
    }
  }

  private String crawlInternalId(String url) {
    String internalId = null;

    if (url != null) {
      internalId = CommonMethods.getLast(url.split("/"));
    }

    return internalId;
  }

  private String crawlInternalPid(JSONObject product) {
    String internalPid = null;

    if (product.has("sku")) {
      internalPid = product.getString("sku");
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
    String url = "https://deliveryextra.resultspage.com/search?af=&cnt=36&ep.selected_store=" + GPACrawler.SAO_PAULO_STORE_ID_EXTRA
        + "&isort=&lot=json&p=Q&ref=www.deliveryextra.com.br&srt=" + this.arrayProducts.size() + "&ts=json-full"
        + "&ua=Mozilla%2F5.0+(X11;+Linux+x86_64)+AppleWebKit%2F537.36+(KHTML,+like+Gecko)+Chrome%2F62.0.3202.62+Safari%2F537.36" + "&w="
        + this.keyword;

    JSONObject searchApi = fetchJSONObject(url, cookies);

    if (this.currentPage == 1 && searchApi.has("merch")) {
      JSONObject merch = searchApi.getJSONObject("merch");

      if (merch.has("jumpurl")) {
        String jumpurl = merch.get("jumpurl").toString();

        if (jumpurl.contains("especial")) {
          String newKeyword = CommonMethods.getLast(jumpurl.split("\\?")[0].split("/"));
          url = url.replace(this.keyword, newKeyword);
          this.keyword = newKeyword;

          searchApi = fetchJSONObject(url, cookies);
        }
      }
    }

    this.log("Link onde são feitos os crawlers: " + url);

    return searchApi;
  }
}
