package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilViannapetCrawler extends CrawlerRankingKeywords {

  public BrasilViannapetCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 0;
    this.log("Página " + this.currentPage);

    String url = "https://www.viannapet.com/" + this.keywordEncoded + "_qO" + this.keywordEncoded + "XvOresultxSM";

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select("#search-list li .product_name a");

    if (!products.isEmpty()) {
      for (Element e : products) {
        String productUrl = CrawlerUtils.completeUrl(e.attr("href"), "https", "www.viannapet.com");

        saveDataProduct(null, null, productUrl);

        this.log(
            "Position: " + this.position +
                " - InternalId: " + null +
                " - InternalPid: " + null +
                " - Url: " + productUrl);

        if (this.arrayProducts.size() == productsLimit)
          break;
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
    return false;
  }
}
