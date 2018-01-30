package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class SaopauloPontofrioCrawler extends CrawlerRankingKeywords {

  public SaopauloPontofrioCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String keyword = this.keywordWithoutAccents.replaceAll(" ", "%20");

    String url = "https://search3.pontofrio.com.br/busca?q=" + keyword + "&page=" + this.currentPage;

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".nm-product-item");

    // número de produtos por página do market
    this.pageSize = 24;

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }
      for (Element e : products) {
        String internalPid = crawlInternalPid(e);
        String urlProduct = crawlProductUrl(internalPid);

        saveDataProduct(null, internalPid, urlProduct);

        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
        if (this.arrayProducts.size() == productsLimit)
          break;

      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

  }

  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.select(".neemu-total-products-container span").first();

    if (totalElement != null) {
      String total = totalElement.attr("data-totalresults").replaceAll("[^0-9]", "").trim();

      if (!total.isEmpty()) {
        this.totalProducts = Integer.parseInt(total);
      }
    }
    this.log("Total da busca: " + this.totalProducts);
  }

  private String crawlInternalPid(Element e) {
    return e.attr("data-productid");
  }

  private String crawlProductUrl(String internalPid) {
    return "https://produto.pontofrio.com.br/?IdProduto=" + internalPid;
  }
}
