package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilAgroverdesrCrawler extends CrawlerRankingKeywords {

  private String keywordKey;

  public BrasilAgroverdesrCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 24;
    this.log("Página " + this.currentPage);

    this.currentDoc = fetchPage();
    Elements products = this.currentDoc.select(".ad-showcase li div[data-id]");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {

        String productPid = e.attr("data-id");
        String productUrl = CrawlerUtils.scrapUrl(e, ".product-image a", "href", "https",
            "www.agroverdesr.com.br");

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
      String url = "https://www.agroverdesr.com.br/" + this.keywordEncoded;
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
      String url = "https://www.agroverdesr.com.br/buscapagina?fq=" + this.keywordKey
          + "&PS=24&sl=ddf33672-d1a8-4f74-b1d9-57fb92b7ce81&cc=24&sm=0&PageNumber=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      doc = fetchDocument(url);
    }

    return doc;
  }

  @Override
  protected void setTotalProducts() {
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".resultado-busca-numero .value", null, null, false, false, 0);
    this.log("Total: " + this.totalProducts);

  }

}
