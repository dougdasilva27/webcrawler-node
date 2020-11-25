package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;

public class ArgentinaPigmentoCrawler extends CrawlerRankingKeywords {

  public ArgentinaPigmentoCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {

    currentDoc =
        fetchDocument(
            "https://perfumeriaspigmento.com.ar/catalogsearch/result/?p="
                + currentPage
                + "&q="
                + keywordEncoded);

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

     Element element = this.currentDoc.select("#toolbar-amount .toolbar-number:nth-child(3)").first();

     int totalProdutos = 0;

     if (element != null) {
        totalProdutos = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, "#toolbar-amount .toolbar-number:nth-child(3)", false, 0);
     } else{
        totalProdutos = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, "#toolbar-amount .toolbar-number", false, 0);
     }

     this.totalProducts = totalProdutos;
    this.log("Total da busca: " + this.totalProducts);
  }
}
