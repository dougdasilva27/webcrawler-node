package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class SaopauloAraujoCrawler extends CrawlerRankingKeywords {

  public SaopauloAraujoCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 32;

    String key = this.keywordWithoutAccents.replaceAll(" ", "%20");

    this.log("Página " + this.currentPage);
    String url = "http://busca.araujo.com.br/busca?q=" + key + "&page=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".neemu-products-container .nm-product-item");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalPid = crawlInternalPid(e);
        String productUrl = crawlProductUrl(e);

        saveDataProduct(null, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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
    Element totalElement = this.currentDoc.select(".neemu-total-products-container").first();

    if (totalElement != null) {
      String total = totalElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!total.isEmpty()) {
        this.totalProducts = Integer.parseInt(total);
      }
    }

    this.log("Total da busca: " + this.totalProducts);
  }

  private String crawlInternalPid(Element e) {
    String internalPid = null;

    String text = e.attr("id");
    if (text.contains("-")) {
      internalPid = CommonMethods.getLast(text.split("-"));
    }

    return internalPid;
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;
    Element urlElement = e.select(".nm-product-name > a").first();

    if (urlElement != null) {
      productUrl = urlElement.attr("href");

      if (!productUrl.startsWith("http")) {
        productUrl = "https:" + productUrl;
      }
    }

    return productUrl;
  }
}
