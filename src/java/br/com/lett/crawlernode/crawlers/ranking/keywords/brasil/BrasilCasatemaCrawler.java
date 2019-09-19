package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilCasatemaCrawler extends CrawlerRankingKeywords {

  public BrasilCasatemaCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 10;
    this.log("Página " + this.currentPage);

    String url = "https://www.casatema.com.br/buscapagina?fq=" + this.keywordEncoded
        + "&PS=18&sl=f5a9b57e-3212-4dac-88ab-871111d79345&cc=1&sm=0&PageNumber="
        + this.currentPage;


    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".prateleira ul li[layout]");

    if (!products.isEmpty()) {

      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalPid = crawlInternalPid(e);
        String productUrl = crawlProductUrl(e);

        saveDataProduct(null, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: "
            + internalPid + " - Url: " + productUrl);
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


  private String crawlInternalPid(Element e) {
    Element inputElement = e.selectFirst("div[data-product-id]");
    String internalPid = null;

    if (inputElement != null) {
      internalPid = inputElement.attr("data-product-id").trim();
    }

    return internalPid;
  }

  private String crawlProductUrl(Element e) {
    Element ancorElement = e.selectFirst(".shelf-qd-v1-name a");
    String url = null;

    if (ancorElement != null) {
      url = ancorElement.attr("href").trim();
    }

    return url;
  }

  @Override
  protected void setTotalProducts() {
    Document document = fetchDocument("https://www.casatema.com.br/" + this.keywordEncoded);
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(document, ".searchResultsTime .resultado-busca-numero span[class=\"value\"]",
        false, 0);

    this.log("Total de produtos: " + this.totalProducts);
  }
}
