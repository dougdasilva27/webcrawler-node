package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilMultinepCrawler extends CrawlerRankingKeywords {

  public BrasilMultinepCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 16;
    this.log("Página " + this.currentPage);

    String url = "https://www.multinep.com.br/loja/busca.php?loja=495222&palavra_busca=" + this.keywordEncoded + "&pg=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    JSONObject search = extractInfo();

    if (search.has("listProducts") && search.getJSONArray("listProducts").length() > 0) {
      JSONArray products = search.getJSONArray("listProducts");

      if (this.totalProducts == 0) {
        setTotalProducts(search);
      }

      for (int i = 0; i < products.length(); i++) {
        JSONObject product = products.getJSONObject(i);

        String productUrl = crawlProductUrl(product);
        String internalId = crawlInternalId(product);

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

  protected void setTotalProducts(JSONObject search) {
    if (search.has("siteSearchResults")) {
      String total = search.get("siteSearchResults").toString().replaceAll("[^0-9]", "");

      if (!total.isEmpty()) {
        this.totalProducts = Integer.parseInt(total);
        this.log("Total da busca: " + this.totalProducts);
      }
    }
  }

  private String crawlInternalId(JSONObject product) {
    String internalId = null;

    if (product.has("idProduct")) {
      internalId = product.get("idProduct").toString();
    }

    return internalId;
  }

  private String crawlProductUrl(JSONObject product) {
    String urlProduct = null;

    if (product.has("urlProduct")) {
      urlProduct = product.get("urlProduct").toString();
    }

    return urlProduct;
  }

  private JSONObject extractInfo() {
    JSONObject json = new JSONObject();

    JSONArray datalayer = CrawlerUtils.selectJsonArrayFromHtml(this.currentDoc, "script", "dataLayer=", null, true, true);
    if (datalayer.length() > 0) {
      json = datalayer.getJSONObject(0);
    }

    return json;
  }
}
