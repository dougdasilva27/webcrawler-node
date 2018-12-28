package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import java.util.Arrays;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class ChileTelemercadosCrawler extends CrawlerRankingKeywords {

  public ChileTelemercadosCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);

    this.pageSize = 16;
    String url = "https://www.telemercados.cl/busca/?ft=" + this.keywordWithoutAccents.replaceAll(" ", "%20") + "&PageNumber=" + this.currentPage;

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select("li[layout] .product-info .nombre[data-idproduct]");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalPid = e.attr("data-idproduct");
        String productUrl = crawlProductUrl(e);

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
    this.totalProducts = CrawlerUtils.scrapTotalProductsForRanking(currentDoc, ".resultado-busca-numero .value", true);
    this.log("Total da busca: " + this.totalProducts);
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;
    Element urlElement = e.selectFirst("> a");

    if (urlElement != null) {
      productUrl = CrawlerUtils.sanitizeUrl(urlElement, Arrays.asList("href"), "https:", "www.telemercados.cl");
    }

    return productUrl;
  }
}
