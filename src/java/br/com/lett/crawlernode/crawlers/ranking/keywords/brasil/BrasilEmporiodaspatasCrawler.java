package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilEmporiodaspatasCrawler extends CrawlerRankingKeywords {

  public BrasilEmporiodaspatasCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 18;
    this.log("Página " + this.currentPage);

    String url = "https://www.emporiodaspatas.com.br/" + this.keywordEncoded
        + "?PageNumber=" + this.currentPage;


    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".shelf.products > .shelf.products ul li[layout]");

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
    Element inputElement = e.selectFirst(".productInfo .rateProduct");
    String internalPid = null;

    if (inputElement != null) {
      String[] splitId = inputElement.id().trim().split("-");
      
      internalPid = splitId.length > 1 ? splitId[1] : null;
    }

    return internalPid;
  }

  private String crawlProductUrl(Element e) {
    Element ancorElement = e.selectFirst(".productInfo .buy-button a");
    String url = null;

    if (ancorElement != null) {
      url = ancorElement.attr("href").trim();
    }

    return url;
  }

  @Override
  protected void setTotalProducts() {
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".main .searchResultsTime .resultado-busca-numero .value",
        true, 0);

    this.log("Total de produtos: " + this.totalProducts);
  }
}
