package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class SaopauloSondaCrawler extends CrawlerRankingKeywords {

  public SaopauloSondaCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {

    this.log("Página " + this.currentPage);

    String keyword = this.keywordWithoutAccents.replaceAll(" ", "%20");

    String url = "http://busca.sondadelivery.com.br/busca?q=" + keyword + "&page=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".neemu-products-container .nm-product-item");

    if (!products.isEmpty()) {

      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {

        String urlProduct = crawlProductUrl(e);

        String internalPid = crawlInternalPid(e);

        String internalId = crawlInternalId(urlProduct);

        saveDataProduct(internalId, internalPid, urlProduct);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);

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
    Element totalElement = this.currentDoc.select(".neemu-total-products-container strong").first();

    if (totalElement != null) {
      try {
        this.totalProducts = Integer.parseInt(totalElement.ownText());
      } catch (Exception e) {
        this.logError(CommonMethods.getStackTrace(e));
      }
    }

    this.log("Total da busca: " + this.totalProducts);
  }

  private String crawlInternalId(String url) {
    return Integer.toString(Integer.parseInt(url.split("/")[url.split("/").length - 1]));
  }

  private String crawlInternalPid(Element e) {
    return null;
  }

  private String crawlProductUrl(Element e) {
    String urlProduct = null;
    Element url = e.select(".nm-product-name > a").first();

    if (url != null) {
      urlProduct = url.attr("href");

      if (!urlProduct.contains("sondadelivery")) {
        urlProduct = CommonMethods.sanitizeUrl("https://www.sondadelivery.com.br/" + urlProduct);
      } else if (!urlProduct.startsWith("http")) {
        urlProduct = CommonMethods.sanitizeUrl("https:" + urlProduct);
      }
    }

    return urlProduct;
  }
}
