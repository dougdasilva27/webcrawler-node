package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilVilanovaCrawler extends CrawlerRankingKeywords {

  private static final int PAGE_SIZE = 24;

  public BrasilVilanovaCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = PAGE_SIZE;

    this.log("Página " + this.currentPage);
    String url = "https://www.vilanova.com.br/search/" + this.keywordEncoded + "?page=" + this.currentPage;

    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".shelf-content-itens li[data-product-id]");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalPid = e.attr("data-product-id");
        String productUrl = CrawlerUtils.scrapUrl(e, ".shelf-url", "href", "https", "www.vilanova.com.br");

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
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".shelf-total-results-qty", true, 0);
    this.log("Total: " + this.totalProducts);
  }
}
