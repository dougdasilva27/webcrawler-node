package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;

public class BrasilMoblyCrawler extends CrawlerRankingKeywords {

  public BrasilMoblyCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 60;

    this.log("Página " + this.currentPage);

    String url = "https://www.mobly.com.br/catalog/?terms=" + this.keywordEncoded + "&page=" + this.currentPage
        + "&api=true&partner=Neemu&bucketTest=A";

    Map<String, String> headers = new HashMap<>();
    headers.put("x-requested-with", "XMLHttpRequest");

    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).build();

    JSONObject productsInfo = JSONUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
    JSONObject products = JSONUtils.getJSONValue(productsInfo, "products");

    if (products.length() > 0) {
      if (totalProducts == 0) {
        this.totalProducts = CrawlerUtils.getIntegerValueFromJSON(productsInfo, "total", 0);
        this.log("Total: " + this.totalProducts);
      }

      for (String internalPid : products.keySet()) {
        JSONObject product = products.getJSONObject(internalPid);

        String productUrl = scrapUrl(product);

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

  private String scrapUrl(JSONObject product) {
    String url = null;

    String productUrl = JSONUtils.getStringValue(product, "url");
    if (productUrl != null) {
      url = CrawlerUtils.completeUrl(productUrl.split("#")[0], "https", "www.mobly.com.br");
    }

    return url;
  }
}
