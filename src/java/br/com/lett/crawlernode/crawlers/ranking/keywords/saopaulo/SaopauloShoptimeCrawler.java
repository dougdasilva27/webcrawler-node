package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class SaopauloShoptimeCrawler extends CrawlerRankingKeywords {

  public SaopauloShoptimeCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 24;

    this.log("Página " + this.currentPage);

    String keyword = location.replaceAll(" ", "+");

    // monta a url com a keyword e a página
    String url = "https://www.shoptime.com.br/busca/?conteudo=" + this.keywordEncoded + "&limite=24&offset=" + this.arrayProducts.size();
    this.log("Link onde são feitos os crawlers: " + url);

    String urlAPi = "https://mystique-v1-shoptime.b2w.io/mystique/search?content=" + keyword + "&offset=" + +this.arrayProducts.size()
        + "&sortBy=moreRelevant&source=nanook";

    JSONObject api = fetchJSONObject(urlAPi);
    JSONArray products = new JSONArray();

    if (api.has("products") && api.get("products") instanceof JSONArray) {
      products = api.getJSONArray("products");
    }

    // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
    if (products.length() >= 1) {
      // se o total de busca não foi setado ainda, chama a função para setar
      if (this.totalProducts == 0) {
        setTotalBusca(api);
      }

      for (int i = 0; i < products.length(); i++) {
        JSONObject product = products.getJSONObject(i);

        // InternalPid
        String internalPid = crawlInternalPid(product);

        // Url do produto
        String productUrl = crawlProductUrl(internalPid);

        // InternalId
        String internalId = crawlInternalId();

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
    if (this.arrayProducts.size() < totalProducts) {
      return true;
    }

    return false;
  }


  protected void setTotalBusca(JSONObject api) {
    if (api.has("_result")) {
      JSONObject result = api.getJSONObject("_result");

      if (result.has("total")) {
        this.totalProducts = result.getInt("total");

        this.log("Total da busca: " + this.totalProducts);
      }
    }
  }

  private String crawlInternalId() {
    return null;
  }

  private String crawlInternalPid(JSONObject product) {
    String internalPid = null;

    if (product.has("id")) {
      internalPid = Integer.toString(product.getInt("id"));
    }

    return internalPid;
  }

  private String crawlProductUrl(String internalPid) {
    return "https://www.shoptime.com.br/produto/" + internalPid;
  }
}
