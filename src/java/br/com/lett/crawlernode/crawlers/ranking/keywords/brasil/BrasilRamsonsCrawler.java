package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilRamsonsCrawler extends CrawlerRankingKeywords {

  public BrasilRamsonsCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 21;

    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String url = "https://busca.ramsons.com.br/" + this.keywordWithoutAccents.replaceAll(" ", "%20") + "?pagina=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".list-products > article.products");

    if (!products.isEmpty()) {
      for (Element e : products) {
        if (this.totalProducts == 0) {
          setTotalProducts();
        }

        String internalId = crawlInternalId(e);
        String productUrl = crawlProductUrl(e);

        saveDataProduct(internalId, null, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit) {
          break;
        }
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado para a página atual!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }

  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.select(".resumo-resultado strong").first();

    if (totalElement != null) {
      String total = totalElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!total.isEmpty()) {
        this.totalProducts = Integer.parseInt(total);
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String crawlInternalId(Element e) {
    return e.attr("data-item-id");
  }

  /**
   * @param e
   * @return
   */
  private String crawlProductUrl(Element e) {
    String urlProduct = null;
    Element url = e.select(".product-name a").first();

    if (url != null) {
      urlProduct = url.attr("href");
    }

    return urlProduct;
  }
}
