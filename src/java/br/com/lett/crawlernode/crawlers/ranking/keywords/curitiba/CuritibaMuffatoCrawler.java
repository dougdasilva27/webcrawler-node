package br.com.lett.crawlernode.crawlers.ranking.keywords.curitiba;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class CuritibaMuffatoCrawler extends CrawlerRankingKeywords {

  public CuritibaMuffatoCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);

    // número de produtos por página do market
    this.pageSize = 24;

    // monta a url com a keyword e a página
    String url =
        "https://buscadelivery.supermuffato.com.br/busca?q=" + this.keywordEncoded + "&common_filter[saleschannel]=14&page=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    // chama função de pegar a url
    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".nm-product-item > div");

    // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
    if (products.size() >= 1) {
      // se o total de busca não foi setado ainda, chama a função para setar
      if (this.totalProducts == 0)
        setTotalProducts();

      for (Element e : products) {
        // InternalPid
        String internalPid = crawlInternalPid(e);

        // InternalId
        String internalId = crawlInternalId(e);

        // Url do produto
        String productUrl = crawlProductUrl(e);

        saveDataProduct(internalId, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit)
          break;

      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado para a página atual!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }

  @Override
  protected boolean hasNextPage() {
    if (arrayProducts.size() < this.totalProducts) {
      return true;
    }

    return false;
  }

  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.select(".neemu-total-products-container").first();

    try {
      if (totalElement != null)
        this.totalProducts = Integer.parseInt(totalElement.text().replaceAll("[^0-9]", "").trim());
    } catch (Exception e) {
      this.logError(e.getMessage());
    }

    this.log("Total da busca: " + this.totalProducts);
  }

  private String crawlInternalId(Element e) {
    String internalId = e.attr("data-id");

    return internalId;
  }

  private String crawlInternalPid(Element e) {
    String internalPid = null;

    return internalPid;
  }

  private String crawlProductUrl(Element e) {
    String urlProduct = null;
    Element urlElement = e.select(".prd-list-item-link:not(.nm-product-img-link)").first();

    if (urlElement != null) {
      urlProduct = urlElement.attr("href");

      if (!urlProduct.contains("supermuffato")) {
        urlProduct = "http://delivery.supermuffato.com.br" + urlProduct;
      } else if (!urlProduct.contains("http")) {
        urlProduct = "http:" + urlProduct;
      }
    }

    return urlProduct;
  }

}
