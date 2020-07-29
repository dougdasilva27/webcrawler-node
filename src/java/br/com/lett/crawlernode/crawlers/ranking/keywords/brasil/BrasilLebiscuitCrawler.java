package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilLebiscuitCrawler extends CrawlerRankingKeywords {

  public BrasilLebiscuitCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);

    this.pageSize = 24;

    String url =
        "https://www.lebiscuit.com.br/busca?ft=" + keywordEncoded + "&PageNumber=" + currentPage;

    this.currentDoc = fetchDocument(url, cookies);

    Elements products = this.currentDoc.select("li[layout] .shelf-item");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) setTotalProducts();

      for (Element e : products) {
        String internalPid = e.attr("data-id");
        String productUrl = e.selectFirst("a").attr("href");

        saveDataProduct(null, internalPid, productUrl);

        this.log(
            "Position: "
                + this.position
                + " - InternalId: "
                + null
                + " - InternalPid: "
                + internalPid
                + " - Url: "
                + productUrl);
      }
    }
  }

  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.selectFirst(".resultado-busca-numero > .value");

    if (totalElement != null) {
      String text = totalElement.text().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }

  @Override
  protected void processBeforeFetch() {
    cookies.addAll(fetchCookies("https://www.lebiscuit.com.br/"));
  }
}
