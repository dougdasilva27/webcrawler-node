package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

public class RiodejaneiroDrogariavenancioCrawler extends CrawlerRankingKeywords {

  public RiodejaneiroDrogariavenancioCrawler(Session session) {
    super(session);
  }

  @Override
  protected void processBeforeFetch() {
    super.processBeforeFetch();

    Logging.printLogDebug(logger, session, "Adding cookie...");

    BasicClientCookie cookie = new BasicClientCookie("VTEXSC", "sc=1");
    cookie.setDomain(".drogariavenancio.com.br");
    cookie.setPath("/");
    this.cookies.add(cookie);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 50;
    this.log("Página " + this.currentPage);

    String url = "https://www.drogariavenancio.com.br/buscapagina?ft=" + this.keywordEncoded
        + "&PS=" + this.pageSize + "&sl=d8b783e8-6563-7d5f-61f2-2ba298708951&cc=" + this.pageSize
        + "&sm=0&PageNumber=" + this.currentPage;

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".shelf-product");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }
      for (Element e : products) {

        String internalId = null;
        String productPid = crawlProductPid(e);
        String productUrl = crawlProductUrl(e);

        saveDataProduct(internalId, productPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: "
            + productPid + " - Url: " + productUrl);
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
  protected boolean hasNextPage() {
    if (this.currentDoc.select(".shelf-product").size() < this.pageSize) {
      return false;
    }

    return true;
  }

  private String crawlProductPid(Element e) {
    return e.attr("data-product-id");
  }

  private String crawlProductUrl(Element e) {
    Element subE = e.selectFirst("figure.shelf-product__container .shelf-product__image a");
    String url = null;

    if (subE != null) {
      url = CrawlerUtils.sanitizeUrl(subE, "href", "https:", "www.drogariavenancio.com.br");
    }

    return url;
  }
}
