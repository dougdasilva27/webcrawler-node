package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Document;
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

  private String keywordKey;

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
    this.pageSize = 12;
    this.log("Página " + this.currentPage);

    this.currentDoc = fetchPage();
    Elements products = this.currentDoc.select(".shelf-product");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }
      for (Element e : products) {

        String productPid = e.attr("data-product-id");
        String productUrl =
            CrawlerUtils.scrapUrl(e, "figure.shelf-product__container .shelf-product__image a", "href", "https", "www.drogariavenancio.com.br");

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

  private Document fetchPage() {
    Document doc = new Document("");

    if (this.currentPage == 1) {
      String url = "https://www.drogariavenancio.com.br/" + this.keywordEncoded;
      this.log("Link onde são feitos os crawlers: " + url);
      doc = fetchDocument(url);

      Elements scripts = doc.select("script[type=text/javascript]");
      String token = "/busca?fq=";
      for (Element e : scripts) {
        String html = e.html();

        if (html.contains(token)) {
          this.keywordKey = CrawlerUtils.extractSpecificStringFromScript(html, "fq=", "&", false);
          break;
        }
      }
    } else if (this.keywordKey != null) {
      String url = "https://www.drogariavenancio.com.br/buscapagina?fq=" + this.keywordKey
          + "&PS=12&sl=d8b783e8-6563-7d5f-61f2-2ba298708951&cc=12&sm=0&PageNumber=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      doc = fetchDocument(url);
    }

    return doc;
  }

  @Override
  protected void setTotalProducts() {
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, ".resultado-busca-numero .value", true, 0);
    this.log("Total da busca: " + this.totalProducts);
  }
}
