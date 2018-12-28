package br.com.lett.crawlernode.crawlers.ranking.keywords.peru;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class PeruPlazaveaCrawler extends CrawlerRankingKeywords {
  public PeruPlazaveaCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 20;
    this.log("Página " + this.currentPage);

    String url = "https://www.plazavea.com.pe/Busca/?ft=" + this.keywordEncoded + "&O=OrderByPriceASC&PageNumber=" + this.currentPage;

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".prateleira ul li[layout] .g-producto[data-prod]");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }
      for (Element e : products) {

        String productPid = e.attr("data-prod");
        String productUrl = crawlProductUrl(e);

        saveDataProduct(null, productPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + productPid + " - Url: " + productUrl);
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
    Element totalElement = this.currentDoc.select(".resultado-busca-numero .value").first();

    if (totalElement != null) {
      String text = totalElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;

    Element url = e.selectFirst("a.g-nombre-prod");
    if (url != null) {
      productUrl = url.attr("href");
    }

    return productUrl;
  }
}
