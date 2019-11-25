package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilDogsdayCrawler extends CrawlerRankingKeywords {

  public BrasilDogsdayCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 12;
    this.log("Página " + this.currentPage);

    String url = "https://www.dogsday.com.br/index.php?route=product/search&search=" + this.keywordEncoded + "&page=" + this.currentPage;

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".product-layout.product-list");

    if (!products.isEmpty()) {
      for (Element e : products) {
        String internalId = scrapInternalId(e);
        String productUrl = "https://www.dogsday.com.br/index.php?route=product/product&product_id=" + internalId;

        saveDataProduct(internalId, internalId, productUrl);

        this.log(
            "Position: " + this.position +
                " - InternalId: " + internalId +
                " - InternalPid: " + internalId +
                " - Url: " + productUrl
        );

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


  private String scrapInternalId(Element e) {
    String internalPid = null;

    Element elementUrl = e.selectFirst("h4 a");
    if (elementUrl != null) {
      String url = elementUrl.attr("href");
      if (url.contains("&")) {
        String[] parameters = url.split("&");

        for (String parameter : parameters) {
          if (parameter.startsWith("product_id=")) {
            internalPid = CommonMethods.getLast(parameter.split("="));
            break;
          }
        }
      }
    }

    return internalPid;
  }

  @Override
  protected boolean hasNextPage() {
    Element nextPage = this.currentDoc.selectFirst(".pagination li:last-child");
    return nextPage != null && !nextPage.hasClass("active");
  }
}
