package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilDrogarianisseiCrawler extends CrawlerRankingKeywords {

  public BrasilDrogarianisseiCrawler(Session session) {
    super(session);
  }


  @Override
  protected void extractProductsFromCurrentPage() {

    this.pageSize = 9;

    String keyword = this.keywordWithoutAccents.replaceAll(" ", "-");

    this.log("Página " + this.currentPage);

    String url = "https://www.farmaciasnissei.com.br/" + keyword;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".item.p-2 .produto");

    if (!products.isEmpty()) {

      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {

        // InternalId
        String internalId = e.attr("data-produto_id");

        // Url do produto
        String urlProduct = CrawlerUtils.scrapUrl(e, "a", "href", "https:", "www.farmaciasnissei.com.br");

        saveDataProduct(internalId, null, urlProduct);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - Url: " + urlProduct);
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
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".font-xs #quantidade-busca", true, 0);
    this.log("Total: " + this.totalProducts);
  }

}
