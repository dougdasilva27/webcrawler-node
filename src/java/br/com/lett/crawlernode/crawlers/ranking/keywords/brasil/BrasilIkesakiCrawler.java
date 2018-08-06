package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.ranking.RankingKeywordsSession;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilIkesakiCrawler extends CrawlerRankingKeywords {

  public BrasilIkesakiCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 48;

    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String url = "https://www.ikesaki.com.br/busca?ft=" + this.keywordEncoded + "&PageNumber=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".vitrine li[layout] > div");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalPid = crawlInternalPid(e);
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
  protected boolean checkIfHasNextPage() {
    return super.checkIfHasNextPage() && !(session instanceof RankingKeywordsSession);
  }

  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.select(".resultado-busca-numero .value").first();

    if (totalElement != null) {
      String total = totalElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!total.isEmpty()) {
        this.totalProducts = Integer.parseInt(total);
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String crawlInternalPid(Element e) {
    return e.attr("data-product-id");
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;

    Element url = e.select(".shelf-product-name__link").first();

    if (url != null) {
      productUrl = url.attr("href");

      if (!productUrl.startsWith("http")) {
        productUrl = "https:" + productUrl;
      }
    }

    return productUrl;
  }
}
