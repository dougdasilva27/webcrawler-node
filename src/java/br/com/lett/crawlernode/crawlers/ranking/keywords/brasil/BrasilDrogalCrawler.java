package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilDrogalCrawler extends CrawlerRankingKeywords {

  public BrasilDrogalCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 56;

    this.log("Página " + this.currentPage);

    String url = "https://www.drogal.com.br/" + this.keywordEncoded + "/?p=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".list-products li > div");

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
    return !this.currentDoc.select("a.view-more[data-direction=next]").isEmpty();
  }

  private String crawlInternalId(Element e) {
    return e.attr("data-sku");
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;

    Element url = e.select("a.link").first();
    if (url != null) {
      productUrl = url.attr("href");

      if (!productUrl.contains("drogal")) {
        productUrl = ("https://www.drogal.com.br/" + productUrl).replace("br//", "br/");
      }
    }

    return productUrl;
  }
}
