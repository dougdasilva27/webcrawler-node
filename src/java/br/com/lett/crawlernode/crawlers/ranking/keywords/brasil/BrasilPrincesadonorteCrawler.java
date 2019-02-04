package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilPrincesadonorteCrawler extends CrawlerRankingKeywords {

  public BrasilPrincesadonorteCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    String url = "https://www.princesadonorteonline.com.br//catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordEncoded;

    this.pageSize = 9;

    this.log("Página " + this.currentPage);

    Document doc = fetchDocument(url);

    Elements products = doc.select(".category-products .products-grid.row .item");

    if (products.size() > 0) {
      if (totalProducts == 0) {
        setTotalProducts(doc);
      }

      for (Element product : products) {

        String internalPid = null;
        String internalId = scrapInternalId(product);
        String productUrl = scrapProductUrl(product);

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

  private String scrapInternalId(Element product) {
    Element ancorElement = product.selectFirst(".view-detail");
    String internalId = null;

    if (ancorElement != null) {
      internalId = ancorElement.attr("id").replaceAll("[^0-9]", "");
    }

    return internalId;
  }


  private String scrapProductUrl(Element product) {
    String productUrl = null;
    Element ancorElement = product.selectFirst(".product-name a");

    if (ancorElement != null) {
      productUrl = ancorElement.attr("href");
    }

    return productUrl;
  }

  private void setTotalProducts(Document doc) {
    Element amount = doc.selectFirst(".amount");
    String total = null;
    if (amount != null) {
      total = amount.text().trim();
      total = total.substring(total.indexOf("de"), total.length());
      total = total.replaceAll("[^0-9]", "");

      this.totalProducts = Integer.parseInt(total);
    }
  }

}
