package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilBuscapeCrawler extends CrawlerRankingKeywords {
  public BrasilBuscapeCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 24;
    this.log("Página " + this.currentPage);

    String url = "https://www.buscape.com.br/search/" + this.keywordWithoutAccents.replace(" ", "-") + "?pagina=" + this.currentPage;

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".offers-result .card--product a:first-child");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, ".offers-result__total-items", true, 0);
        this.log("Total da busca: " + this.totalProducts);
      }
      for (Element e : products) {
        String internalId = e.attr("data-log_id");
        String productUrl = CrawlerUtils.completeUrl(e.attr("href"), "https", "www.buscape.com.br");

        saveDataProduct(internalId, null, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit)
          break;

      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }
}
