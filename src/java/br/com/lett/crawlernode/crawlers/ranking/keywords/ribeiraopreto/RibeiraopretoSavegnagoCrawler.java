package br.com.lett.crawlernode.crawlers.ranking.keywords.ribeiraopreto;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class RibeiraopretoSavegnagoCrawler extends CrawlerRankingKeywords {

  public RibeiraopretoSavegnagoCrawler(Session session) {
    super(session);
  }

  /**
   * Código das cidades: Ribeirão Preto - 2 Sertãozinho - 6 Jardianópolis - 11 Jaboticabal - 7
   * Franca - 3 Barretos - 10 Bebedouro - 9 Monte Alto - 12 Araraquara - 4 São carlos - 5 Matão - 8
   */
  // private static final int cityCode = ControllerKeywords.codeCity;
  private static final int cityCode = 2;

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 20;

    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String url = "http://busca.savegnago.com.br/busca?q=" + this.keywordEncoded + "&sc=" + cityCode
        + "&page=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    // chama função de pegar a url
    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select("li.nm-product-item");

    // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
    if (products.size() >= 1) {
      // se o total de busca não foi setado ainda, chama a função para setar
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        // InternalPid
        String internalPid = crawlInternalPid(e);

        // InternalId
        String internalId = crawlInternalId(e);

        // Url do produto
        String productUrl = crawlProductUrl(e);

        saveDataProduct(internalId, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: "
            + internalPid + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit) {
          break;
        }

      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
        + this.arrayProducts.size() + " produtos crawleados");
  }

  @Override
  protected boolean hasNextPage() {
    return arrayProducts.size() < this.totalProducts;
  }

  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.select("span.nm-total-products").first();

    try {
      if (totalElement != null) {
        this.totalProducts = Integer.parseInt(totalElement.text());
      }
    } catch (Exception e) {
      this.logError(CommonMethods.getStackTrace(e));
    }

    this.log("Total da busca: " + this.totalProducts);
  }

  private String crawlInternalId(Element e) {
    return null;
  }

  private String crawlInternalPid(Element e) {
    return e.attr("data-sku");
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;
    Element urlElement = e.select("h2 a").first();

    if (urlElement != null) {
      productUrl = urlElement.attr("href");

      if (!productUrl.startsWith("http")) {
        productUrl = "https:" + productUrl;
      }

      if (!productUrl.contains("sc=") && productUrl.contains("?")) {
        productUrl += "&sc=" + cityCode;
      } else if (!productUrl.contains("sc=")) {
        productUrl += "?sc=" + cityCode;
      } else {
        int x = productUrl.lastIndexOf("sc=") + 3;
        String cityCodeDefault = productUrl.substring(x).split("")[0];

        productUrl = productUrl.replace("sc=" + cityCodeDefault, "sc=" + cityCode);
      }
    }

    return productUrl;
  }

}
