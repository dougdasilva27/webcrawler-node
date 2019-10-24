package br.com.lett.crawlernode.crawlers.ranking.keywords.portoalegre;

import java.util.Arrays;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class PortoalegreAguiaveterinariaCrawler extends CrawlerRankingKeywords {

  public PortoalegreAguiaveterinariaCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 24;
    this.log("Página " + this.currentPage);

    String url = "https://www.aguiaveterinaria.com.br/busca/page:" + this.currentPage + "?item=" + this.keywordEncoded;

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".product-list-item");

    if (!products.isEmpty()) {

      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String productUrl = CrawlerUtils.scrapUrl(e, ".title > a", Arrays.asList("href"), "https", "www.aguiaveterinaria.com.br");
        String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".md-button[data-grade-id]", "data-grade-id");

        saveDataProduct(internalId, null, productUrl);

        this.log(
            "Position: " + this.position +
                " - InternalId: " + internalId +
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
    return !this.currentDoc.select(".pagination-buttons .next").isEmpty();
  }
}
