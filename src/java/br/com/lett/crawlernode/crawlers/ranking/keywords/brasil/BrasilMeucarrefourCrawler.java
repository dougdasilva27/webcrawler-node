package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.FetchUtilities;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BrasilMeucarrefourCrawler extends CrawlerRankingKeywords {

  public BrasilMeucarrefourCrawler(Session session) {
    super(session);
    super.fetchMode = FetchMode.FETCHER;
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 60;

    this.log("Página " + this.currentPage);

    String url = "https://api.carrefour.com.br/mobile-food/v1/products/search?term=" + this.keywordEncoded + "&pageSize=2000";
    JSONObject productsInfo = crawlSearchApi(url);
    JSONArray products = productsInfo.has("data") ? productsInfo.getJSONArray("data") : new JSONArray();

    if (products.length() > 0) {
      if (totalProducts == 0) {
        this.totalProducts = productsInfo.has("total") ? productsInfo.getInt("total") : 0;
        this.log("Total: " + this.totalProducts);
      }

      for (int i = 0; i < products.length(); i++) {
        JSONObject product = products.getJSONObject(i);

        String internalId = crawlInternalId(product);
        String productUrl = crawlProductUrl(internalId);

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

  @Override
  protected boolean hasNextPage() {
    return this.arrayProducts.size() < this.totalProducts;
  }

  private String crawlInternalId(JSONObject product) {
    String internalId = null;

    if (product.has("sku")) {
      internalId = product.getString("sku");
    }

    return internalId;
  }

  private String crawlProductUrl(String id) {
    return "https://www.carrefour.com.br/meucarrefour#" + id;
  }

  private JSONObject crawlSearchApi(String url) {
    Map<String, String> headers = new HashMap<>();
    headers.put("User-Agent", FetchUtilities.randMobileUserAgent());

    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).build();

    return CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
  }
}
