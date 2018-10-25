package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class MexicoSuperamaCrawler extends CrawlerRankingKeywords {

  public MexicoSuperamaCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 18;

    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    // primeira página começa em 0 e assim vai.
    String url = "https://www.superama.com.mx/buscador/resultado?busqueda=" + this.keywordEncoded;
    this.log("Link onde são feitos os crawlers: " + url);

    JSONObject jsonSearch = fetchJSONObject(url);

    if (jsonSearch.has("Products")) {
      JSONArray products = jsonSearch.getJSONArray("Products");

      // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
      if (products.length() >= 1) {
        // se o total de busca não foi setado ainda, chama a função para setar
        if (this.totalProducts == 0) {
          this.totalProducts = products.length();
          this.log("Total da busca: " + this.totalProducts);
        }

        for (int i = 0; i < products.length(); i++) {
          JSONObject product = products.getJSONObject(i);
          // InternalPid
          String internalPid = crawlInternalPid();

          // InternalId
          String internalId = crawlInternalId(product);

          // Url do produto
          String productUrl = crawlProductUrl(product);

          saveDataProduct(internalId, internalPid, productUrl);

          this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
          if (this.arrayProducts.size() == productsLimit)
            break;
        }
      } else {
        this.result = false;
        this.log("Keyword sem resultado!");
      }
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }

  @Override
  protected boolean hasNextPage() {
    return false;
  }


  private String crawlInternalId(JSONObject product) {
    String internalId = null;

    if (product.has("Upc")) {
      internalId = product.getString("Upc");
    }

    return internalId;
  }

  private String crawlInternalPid() {
    String internalPid = null;

    return internalPid;
  }

  private String crawlProductUrl(JSONObject product) {
    String urlProduct = "https://www.superama.com.mx/catalogo";

    if (product.has("SeoDepartamentoUrlName")) {
      String url = product.getString("SeoDepartamentoUrlName").trim();

      if (!url.isEmpty()) {
        urlProduct += "/" + url;
      }
    }

    if (product.has("SeoFamiliaUrlName")) {
      String url = product.getString("SeoFamiliaUrlName").trim();

      if (!url.isEmpty()) {
        urlProduct += "/" + url;
      }
    }

    if (product.has("SeoLineaUrlName")) {
      String url = product.getString("SeoLineaUrlName").trim();

      if (!url.isEmpty()) {
        urlProduct += "/" + url;
      }
    }

    if (product.has("SeoProductUrlName")) {
      String url = product.getString("SeoProductUrlName").trim();

      if (!url.isEmpty()) {
        urlProduct += "/" + url;
      }
    }

    if (product.has("Upc")) {
      String url = product.getString("Upc").trim();

      if (!url.isEmpty()) {
        urlProduct += "/" + url;
      }
    }

    return urlProduct;
  }
}
