package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilRihappyCrawler extends CrawlerRankingKeywords {

  public BrasilRihappyCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 36;
    this.log("Página " + this.currentPage);

    String url = "http://www.rihappy.com.br/" + this.keywordWithoutAccents.replaceAll(" ", "%20") + "?PageNumber=" + this.currentPage + "&PS=50";
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select("div.vitrine ul > li[layout] > a");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalPid = e.attr("rel");
        String productUrl = CrawlerUtils.completeUrl(e.attr("href"), "https", "www.rihappy.com.br");

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

  @Override
  protected void setTotalProducts() {
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, "span.resultado-busca-numero > span.value", null, true, 0);
    this.log("Total da busca: " + this.totalProducts);
  }
}
