package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class DiaCrawlerRanking extends CrawlerRankingKeywords {

  public DiaCrawlerRanking(Session session) {
    super(session);
  }

  @Override
  public void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);
    this.pageSize = 20;

    String url =
        "https://dia.neemu.com/searchapi/v3/search?apiKey=dia&secretKey=6AiYVW9lux1pwyj4JaIE2Q==" + "&resultsQueries=5&resultsProducts=20&terms="
            + this.keywordWithoutAccents.replace(" ", "%20") + "&page=" + this.currentPage + "&productFormat=complete";
    this.log("Link onde são feitos os crawlers: " + url);

    JSONObject searchApi = fetchJSONObject(url);

    JSONArray products = searchApi.has("products") ? searchApi.getJSONArray("products") : new JSONArray();
    if (products.length() > 0) {
      if (this.totalProducts == 0) {
        setTotalProducts(searchApi);
      }

      for (int i = 0; i < products.length(); i++) {
        JSONObject product = products.getJSONObject(i);

        String internalPid = crawlInternalPid(product);
        String productUrl = crawlProductUrl(product);

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

  protected void setTotalProducts(JSONObject searchApi) {
    if (searchApi.has("size")) {
      String text = searchApi.get("size").toString().replaceAll("[^0-9]", "");

      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
        this.log("Total da busca: " + this.totalProducts);
      }
    }
  }

  private String crawlInternalPid(JSONObject product) {
    String internalPid = null;

    if (product.has("id")) {
      internalPid = product.get("id").toString();
    }

    return internalPid;
  }

  private String crawlProductUrl(JSONObject product) {
    String productUrl = null;

    if (product.has("url")) {
      productUrl = CrawlerUtils.completeUrl(product.get("url").toString(), "https://", "www.dia.com.br");
    }

    return productUrl;
  }

}
