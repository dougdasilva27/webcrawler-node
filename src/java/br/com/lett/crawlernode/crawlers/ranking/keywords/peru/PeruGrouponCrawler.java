package br.com.lett.crawlernode.crawlers.ranking.keywords.peru;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class PeruGrouponCrawler extends CrawlerRankingKeywords {
  public PeruGrouponCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 36;
    this.log("Página " + this.currentPage);

    String url = "https://www.groupon.com.pe/productos/search?q=" + this.keywordEncoded + "&offset=" + this.arrayProducts.size();

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select("#deals-div div.row > div[class] figure");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }
      for (Element e : products) {

        String productPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".unified_discount_id", "value");
        String productUrl = crawlProductUrl(e);

        saveDataProduct(null, productPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + productPid + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit)
          break;

      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }

  @Override
  protected void setTotalProducts() {
    this.totalProducts = CrawlerUtils.scrapTotalProductsForRanking(currentDoc, ".total-deals", false);
    this.log("Total da busca: " + this.totalProducts);
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;

    Element url = e.selectFirst("a.deal-link");
    if (url != null) {
      productUrl = CrawlerUtils.sanitizeUrl(url, "href", "https:", "www.groupon.com.pe");
    }

    return productUrl;
  }
}
