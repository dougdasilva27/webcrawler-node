package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilFarmaciaindianaCrawler extends CrawlerRankingKeywords {

  public BrasilFarmaciaindianaCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 16;

    this.log("Página " + this.currentPage);

    String url = "https://www.farmaciaindiana.com.br/" + this.keywordEncoded;

    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements id = this.currentDoc.select("div.prateleira div.prateleira > ul > li[layout]");

    if (id.size() >= 1) {
      if (this.totalProducts == 0)
        setTotalProducts();

      for (Element e : id) {

        String internalId = crawlInternalId(e);
        String urlProduct = crawlProductUrl(e);

        saveDataProduct(internalId, null, urlProduct);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: "
            + null + " - Url: " + urlProduct);
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
    Element totalElement = this.currentDoc.select("span.resultado-busca-numero span.value").first();

    if (totalElement != null) {
      try {
        this.totalProducts = Integer.parseInt(totalElement.text());
      } catch (Exception e) {
        this.logError(e.getMessage());
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String crawlInternalId(Element e) {
    String internalId = null;
    Element id = e.selectFirst(".qd_cpProdId");

    if (id != null) {
      internalId = id.attr("value");
    }

    return internalId;
  }


  private String crawlProductUrl(Element e) {
    String url = null;
    Element input = e.selectFirst(".qd_cpUri");

    if (input != null) {
      url = input.attr("value");
    }

    return url;
  }
}
