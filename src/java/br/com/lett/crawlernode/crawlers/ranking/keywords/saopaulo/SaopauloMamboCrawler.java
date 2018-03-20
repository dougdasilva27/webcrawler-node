package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class SaopauloMamboCrawler extends CrawlerRankingKeywords {

  public SaopauloMamboCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);
    this.pageSize = 24;

    String keyword = this.keywordWithoutAccents.replaceAll(" ", "%20");
    String url = "http://busca.mambo.com.br/busca?q=" + keyword + "&page=" + this.currentPage;
    takeAScreenshot(url);

    String apiUrl = "https://busca.mambo.com.br/searchapi/v3/search?apikey=mambo&secretkey=O3i%2FCRv0rNgaIo08FGLo4g%3D%3D&terms=" + keyword + "&page="
        + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + apiUrl);

    JSONObject search = fetchJSONObject(apiUrl);
    JSONArray products = crawlProducts(search);

    if (products.length() > 0) {
      if (this.totalProducts == 0) {
        setTotalProducts(search);
      }

      for (int i = 0; i < products.length(); i++) {
        JSONObject product = products.getJSONObject(i);

        String internalPid = crawlInternalPid(product);
        String internalId = null;
        String productUrl = crawlProductUrl(product);

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
    return arrayProducts.size() < this.totalProducts;
  }

  protected void setTotalProducts(JSONObject search) {
    if (search.has("size")) {
      this.totalProducts = search.getInt("size");

      this.log("Total: " + this.totalProducts);
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
    String urlProduct = null;

    if (product.has("url")) {
      urlProduct = product.getString("url");

      if (!urlProduct.startsWith("http")) {
        urlProduct = "https://" + urlProduct;
      }
    }

    return urlProduct;
  }


  private JSONArray crawlProducts(JSONObject json) {
    JSONArray products = new JSONArray();

    if (json.has("products")) {
      products = json.getJSONArray("products");
    }

    return products;
  }

}
