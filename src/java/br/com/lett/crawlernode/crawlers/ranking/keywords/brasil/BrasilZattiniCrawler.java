package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilZattiniCrawler extends CrawlerRankingKeywords {

  public BrasilZattiniCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 42;

    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String url = "https://www.netshoes.com.br/busca?nsCat=Natural&q=" + this.keywordEncoded + "&page=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    // chama função de pegar o html
    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select("#item-list .item");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProductsCarrefour();
      }

      for (Element e : products) {
        String internalPid = crawlInternalPid(e);
        String productUrl = crawlProductUrl(e);

        saveDataProduct(null, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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

  protected void setTotalProductsCarrefour() {
    Element totalElement = this.currentDoc.select(".items-info .block").first();

    if (totalElement != null) {
      String text = totalElement.ownText();

      if (text.contains("de")) {
        String total = text.split("de")[1].replaceAll("[^0-9]", "").trim();

        if (!total.isEmpty()) {
          this.totalProducts = Integer.parseInt(total);
        }
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String crawlInternalPid(Element e) {
    return e.attr("parent-sku");
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;

    Element url = e.select("> a").first();

    if (url != null) {
      productUrl = url.attr("href");

      if (!productUrl.startsWith("http")) {
        productUrl = "https:" + productUrl;
      }
    }

    return productUrl;
  }
}
