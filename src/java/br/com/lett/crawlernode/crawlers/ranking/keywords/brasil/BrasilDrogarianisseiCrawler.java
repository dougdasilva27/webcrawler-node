package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilDrogarianisseiCrawler extends CrawlerRankingKeywords {

  public BrasilDrogarianisseiCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 16;
    this.log("Página " + this.currentPage);

    String url = "https://www.drogariasnissei.com.br/catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordEncoded
        + "&product_list_limit=36";
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".category-products .product-item-info");

    if (!products.isEmpty()) {
      for (Element e : products) {
        String internalId = crawlInternalId(e);
        String productUrl = crawlProductUrl(e);

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
  protected boolean hasNextPage() {
    return this.currentDoc.select(".action.next").first() != null;
  }

  private String crawlInternalId(Element e) {
    String internalId = null;

    Element id = e.select("[data-product-id]").first();
    if (id != null) {
      internalId = id.attr("data-product-id");
    }

    return internalId;
  }

  private String crawlProductUrl(Element e) {
    String productUrl = e.attr("href");

    Element url = e.select(".product-item-link").first();
    if (url != null) {
      productUrl = url.attr("href");
    }

    return productUrl;
  }
}
