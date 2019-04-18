package br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class ChileJumboCrawler extends CrawlerRankingKeywords {

  public ChileJumboCrawler(Session session) {
    super(session);
  }

  protected String storeCode;
  protected static final String API_KEY = "IuimuMneIKJd3tapno2Ag1c1WcAES97j";
  protected static final String HOST = br.com.lett.crawlernode.crawlers.corecontent.extractionutils.ChileJumboCrawler.HOST;

  @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);

    this.pageSize = 12;

    String url = "https://" + HOST + "/busca/?ft=" + this.keywordWithoutAccents.replace(" ", "%20") + "&page=" + this.currentPage;
    String apiUrl =
        "https://api.smdigital.cl:8443/v0/cl/jumbo/vtex/front/dev/proxy/api/v1/catalog_system/pub/products/search?_from=" + this.arrayProducts.size()
            + "&_to=" + (this.pageSize * this.currentPage) + "&ft=" + this.keywordWithoutAccents.replace(" ", "%20") + "&sc=" + storeCode;

    Map<String, String> headers = new HashMap<>();
    headers.put("Referer", url);
    headers.put("x-api-key", API_KEY);

    Request request = RequestBuilder.create().setUrl(apiUrl).setHeaders(headers).setCookies(cookies).build();
    String res = new ApacheDataFetcher().get(session, request).getBody();

    JSONArray products = CrawlerUtils.stringToJsonArray(res);

    if (products.length() > 0) {
      for (Object e : products) {
        JSONObject product = (JSONObject) e;
        String internalPid = product.has("productId") && !product.isNull("productId") ? product.get("productId").toString() : null;
        String productUrl = crawlProductUrl(product);

        saveDataProduct(null, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit)
          break;
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }

  @Override
  protected boolean hasNextPage() {
    return !this.arrayProducts.isEmpty() && ((this.arrayProducts.size() / this.currentPage) >= this.pageSize);
  }

  private String crawlProductUrl(JSONObject product) {
    String productUrl = product.has("linkText") && !product.isNull("linkText") ? product.get("linkText").toString() : null;

    if (productUrl != null) {
      productUrl += productUrl.endsWith("/p") ? "" : "/p";
    }

    return CrawlerUtils.completeUrl(productUrl, "https", HOST);
  }

}
