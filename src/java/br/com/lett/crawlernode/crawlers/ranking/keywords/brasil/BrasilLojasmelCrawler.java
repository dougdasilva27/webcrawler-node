package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilLojasmelCrawler extends CrawlerRankingKeywords {

  public BrasilLojasmelCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 32;

    this.log("Página " + this.currentPage);

    // se a key contiver o +, substitui por %20, pois nesse market a pesquisa na url é assim
    String url = "https://www.lojasmel.com/" + this.keywordWithoutAccents.replace(" ", "%20") + "/?p=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".list-products .item-product");

    if (!products.isEmpty()) {
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
  protected boolean hasNextPage() {
    return this.currentDoc.select(".next a.active").first() != null;
  }

  private String crawlInternalPid(Element e) {
    return e.attr("data-sku");
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;
    Element urlElement = e.select("> a").first();

    if (urlElement != null) {
      productUrl = urlElement.attr("href");

      if (!productUrl.contains("www.lojasmel")) {
        productUrl = ("https://www.lojasmel.com/" + productUrl).replace("com//", "com/");
      }
    }

    return productUrl;
  }
}
