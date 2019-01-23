package br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils;

import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public abstract class B2WCrawler extends CrawlerRankingKeywords {

  public B2WCrawler(Session session) {
    super(session);
  }

  protected abstract String getStoreName();

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 24;

    this.log("Página " + this.currentPage);
    String urlAPi = "https://mystique-v2-" + getStoreName() + ".b2w.io/search?content=" + this.keywordWithoutAccents.replace(" ", "%20") + "&offset="
        + this.arrayProducts.size() + "&sortBy=relevance&source=nanook";

    JSONObject api = CrawlerUtils.stringToJson(fetchGetFetcher(urlAPi, null, new HashMap<>(), cookies));
    JSONArray products = api.has("products") && api.get("products") instanceof JSONArray ? api.getJSONArray("products") : new JSONArray();

    if (products.length() >= 1) {
      if (this.totalProducts == 0) {
        setTotalProducts(api);
      }

      for (int i = 0; i < products.length(); i++) {
        JSONObject product = products.getJSONObject(i);

        String internalPid = crawlInternalPid(product);
        String productUrl = crawlProductUrl(internalPid);

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

  protected void setTotalProducts(JSONObject api) {
    if (api.has("_result")) {
      JSONObject result = api.getJSONObject("_result");

      if (result.has("total")) {
        this.totalProducts = result.getInt("total");

        this.log("Total da busca: " + this.totalProducts);
      }
    }
  }

  private String crawlInternalPid(JSONObject product) {
    String internalPid = null;

    if (product.has("id")) {
      internalPid = Integer.toString(product.getInt("id"));
    }

    return internalPid;
  }

  private String crawlProductUrl(String internalPid) {
    return "https://www." + getStoreName() + ".com.br/produto/" + internalPid;
  }
}
