package br.com.lett.crawlernode.crawlers.ranking.keywords.portoalegre;

import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;

public class PortoalegrePetshopmolecaoCrawler extends CrawlerRankingKeywords {

  public PortoalegrePetshopmolecaoCrawler(Session session) {
    super(session);
  }

  @Override
  public void extractProductsFromCurrentPage() {
    this.pageSize = 20;

    this.log("Página " + this.currentPage);
    String url = "https://www.petshopmolecao.com.br/loja/busca.php?loja=560844&palavra_busca=" + this.keywordEncoded + "&pg=" + this.currentPage;
    this.currentDoc = fetchDocument(url);

    JSONObject search = CrawlerUtils.selectJsonFromHtml(this.currentDoc, "script", "dataLayer = [", "]", false, true);

    if (search.has("listProducts") && search.getJSONArray("listProducts").length() > 0) {
      JSONArray products = search.getJSONArray("listProducts");

      if (this.totalProducts == 0) {
        setTotalProducts(search);
      }

      for (int i = 0; i < products.length(); i++) {
        JSONObject product = products.getJSONObject(i);

        String productUrl = product.has("urlProduct") && !product.isNull("urlProduct") ? product.get("urlProduct").toString() : null;
        String internalPid = product.has("idProduct") && !product.isNull("idProduct") ? product.get("idProduct").toString() : null;

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
    this.totalProducts = JSONUtils.getIntegerValueFromJSON(search, "siteSearchResults", 0);
    this.log("Total da busca: " + this.totalProducts);
  }
}
