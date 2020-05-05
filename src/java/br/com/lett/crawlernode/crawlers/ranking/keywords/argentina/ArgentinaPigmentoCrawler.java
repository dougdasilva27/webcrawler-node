package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.MathUtils;

public class ArgentinaPigmentoCrawler extends CrawlerRankingKeywords {

  public ArgentinaPigmentoCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    pageSize = 30;

    currentDoc =
        fetchDocument(
            "https://perfumeriaspigmento.com.ar/catalogsearch/result/?p="
                + currentPage
                + "&q="
                + keywordEncoded
                + "&product_list_limit=30");

    if (this.totalProducts == 0) {
      setTotalProducts();
    }

    currentDoc
        .select(".item.product.product-item")
        .forEach(
            elem -> {
              String link = elem.selectFirst(".product-img a").attr("href");

              String internalId =
                  elem.selectFirst(".price-box.price-final_price").attr("data-product-id");

              saveDataProduct(internalId, null, link);
              this.log(
                  "Position: "
                      + this.position
                      + " - InternalId: "
                      + internalId
                      + " - Url: "
                      + link);
            });
  }

  @Override
  protected void setTotalProducts() {
    this.totalProducts =
        MathUtils.parseInt(
            currentDoc.selectFirst("#toolbar-amount .toolbar-number:nth-child(3)").text());
    this.log("Total da busca: " + this.totalProducts);
  }
}
