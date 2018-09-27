package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.Arrays;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilZattiniCrawler extends CrawlerRankingKeywords {

  public BrasilZattiniCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 42;
    this.log("Página " + this.currentPage);
    String url = "https://www.zattini.com.br/busca?nsCat=Natural&q=" + this.keywordEncoded + "&page=" + this.currentPage;

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select("#item-list .item[parent-sku]");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalPid = e.attr("parent-sku");
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
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.selectFirst(".items-info .block");

    if (totalElement != null) {
      String text = totalElement.ownText().toLowerCase();

      if (text.contains("de")) {
        String total = text.split("de")[1].replaceAll("[^0-9]", "").trim();

        if (!total.isEmpty()) {
          this.totalProducts = Integer.parseInt(total);
        }
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;

    Element url = e.selectFirst("> a");

    if (url != null) {
      productUrl = CrawlerUtils.sanitizeUrl(url, Arrays.asList("href"), "https:", "");;
    }

    return productUrl;
  }
}
