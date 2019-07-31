package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class SaopauloSondaCrawler extends CrawlerRankingKeywords {

  public SaopauloSondaCrawler(Session session) {
    super(session);
    super.fetchMode = FetchMode.APACHE;
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 96;
    this.log("Página " + this.currentPage);

    String url =
        "https://www.sondadelivery.com.br/delivery/busca/" + this.keywordWithoutAccents.replace(" ", "%20") + "/" + this.currentPage + "/96/0/";
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".product-list .product:first-child");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {

        String productUrl = CrawlerUtils.scrapUrl(e, ".product--info > a", "href", "https", "www.sondadelivery.com.br");
        String internalId = productUrl != null ? crawlInternalId(productUrl) : null;

        saveDataProduct(internalId, null, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);

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
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, ".search-filter--results strong", null, true, 0);
    this.log("Total da busca: " + this.totalProducts);
  }

  private String crawlInternalId(String url) {
    return CommonMethods.getLast(url.split("\\?")[0].split("/"));
  }
}
