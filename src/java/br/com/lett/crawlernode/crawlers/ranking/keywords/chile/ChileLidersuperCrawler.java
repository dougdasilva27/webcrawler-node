package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class ChileLidersuperCrawler extends CrawlerRankingKeywords {

  public ChileLidersuperCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 15;
    this.log("Página " + this.currentPage);

    String url = "https://www.lider.cl/supermercado/search?No=" + this.arrayProducts.size() + "&Ntt=" + this.keywordEncoded
        + "&isNavRequest=Yes&Nrpp=40&page=" + this.currentPage;

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select("#content-prod-boxes div[prod-number]");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalId = e.attr("prod-number");
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
  protected void setTotalProducts() {
    Element total = this.currentDoc.selectFirst(".result-departments a:last-child span");
    if (total != null) {
      String text = total.ownText().replaceAll("[^0-9]", "");

      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
        this.log("Total da busca: " + this.totalProducts);
      }
    }
  }

  private String crawlProductUrl(Element e) {
    String url = null;

    Element eUrl = e.selectFirst(".product-details a");
    if (eUrl != null) {
      url = CrawlerUtils.sanitizeUrl(eUrl, "href", "https:", "www.lider.cl");
    }

    return url;
  }
}
