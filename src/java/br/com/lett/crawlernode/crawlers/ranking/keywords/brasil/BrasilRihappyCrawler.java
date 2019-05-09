package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilRihappyCrawler extends CrawlerRankingKeywords {

  public BrasilRihappyCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 24;
    this.log("Página " + this.currentPage);

    String url = "http://www.havan.com.br/" + this.keywordWithoutAccents.replaceAll(" ", "%20") + "?PageNumber=" + this.currentPage + "&PS=50";
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select("div.prateleira ul > li[layout]");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        Element pid = e.select("> input.qd_cpProdId").first();
        String internalPid = pid.attr("value");
        String internalId = null;
        String productUrl = e.selectFirst("h3.shelf-qd-v1-product-name > a").attr("href");

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

  @Override
  protected boolean hasNextPage() {
    if (this.arrayProducts.size() < this.totalProducts) {
      // tem próxima página
      return true;
    }

    return false;

  }

  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.select("span.resultado-busca-numero > span.value").first();

    if (totalElement != null) {
      try {
        this.totalProducts = Integer.parseInt(totalElement.text().trim());
      } catch (Exception e) {
        this.logError(CommonMethods.getStackTraceString(e));
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }
}
