package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilApoiomineiroCrawler extends CrawlerRankingKeywords {

  public BrasilApoiomineiroCrawler(Session session) {
    super(session);
  }

  private static final String HOME_PAGE = "https://www.apoioentrega.com/";

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 40;

    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String url = "https://www.apoioentrega.com/buscapagina?ft=" + this.keywordEncoded
        + "&PS=40&sl=d5209c58-3b8c-42bc-bdfb-aeb8cf9b3c5c&cc=4&sm=0&PageNumber=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select("div.product-item[id]");

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
    return this.currentDoc.select("div.product-item[id]").size() >= this.pageSize;
  }

  private String crawlInternalPid(Element e) {
    return e.attr("id");
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;

    Element url = e.select("h5 a").first();
    if (url != null) {
      productUrl = url.attr("href");
      if (!productUrl.contains("apoioentrega.com")) {
        productUrl = (HOME_PAGE + productUrl).replace("com//", "com/");
      }
    }
    return productUrl;
  }
}
