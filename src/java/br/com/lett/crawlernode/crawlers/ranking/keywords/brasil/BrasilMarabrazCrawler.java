package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.Arrays;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilMarabrazCrawler extends CrawlerRankingKeywords {

  public BrasilMarabrazCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 10;
    this.log("Página " + this.currentPage);

    String url = "https://busca.marabraz.com.br/busca?q=" + this.keywordEncoded + "&page=" + this.currentPage;

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".neemu-products-container .nm-product-item");

    if (!products.isEmpty()) {

      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, null, "data-sku");
        String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, null, "data-id");

        String productUrl = scrapProductUrl(e);

        saveDataProduct(internalId, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);

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
  protected void setTotalProducts() {

    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".neemu-total-products-container  strong", false, 0);

    this.log("Total da busca: " + this.totalProducts);

  }

  private String scrapProductUrl(Element e) {
    String productUrl = null;
    Element urlElement = e.selectFirst(".nm-product-name a");

    if (urlElement != null) {
      productUrl = CrawlerUtils.sanitizeUrl(urlElement, Arrays.asList("href"), "https:", "https://www.marabraz.com.br");
    }

    return productUrl;
  }

}
