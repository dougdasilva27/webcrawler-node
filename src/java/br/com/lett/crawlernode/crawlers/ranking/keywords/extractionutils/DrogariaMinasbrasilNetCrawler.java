package br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class DrogariaMinasbrasilNetCrawler extends CrawlerRankingKeywords {

  protected String host;

  public DrogariaMinasbrasilNetCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 16;
    this.log("Página " + this.currentPage);

    String url = "https://" + this.host + "/" + this.keywordEncoded + "?p=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".products .product");

    if (!products.isEmpty()) {
      if (totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalPid = null;
        String internalId = crawlInternalId(e);
        String productUrl = CrawlerUtils.scrapUrl(e, ".product-name a", "href", "https", this.host);

        saveDataProduct(internalId, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, ".toolbar-count span", true, 0);
    this.log("Total products: " + this.totalProducts);
  }

  private String crawlInternalId(Element e) {
    String internalId = null;
    Element idElement = e.selectFirst(".buy-form");

    if (idElement != null) {
      String url = idElement.attr("action");

      if (url.contains("product/")) {
        internalId = CommonMethods.getLast(url.split("product/")).split("/")[0].trim();
      }
    }

    return internalId;
  }
}
