package br.com.lett.crawlernode.crawlers.ranking.keywords.belohorizonte;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BelohorizonteSupernossoCrawler extends CrawlerRankingKeywords {

  public BelohorizonteSupernossoCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 12;

    this.log("Página " + this.currentPage);
    String url = "https://www.supernossoemcasa.com.br/" + this.keywordWithoutAccents.replaceAll(" ", "%20") + "?PageNumber=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".vitrine.resultItemsWrapper ul > li[layout]");
    Elements productsIds = this.currentDoc.select(".vitrine.resultItemsWrapper ul > li[id]");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      int index = 0;

      for (Element e : products) {
        String internalPid = CommonMethods.getLast(productsIds.get(index).id().split("_"));
        String productUrl = CrawlerUtils.scrapUrl(e, ".product-name a", "href", "https", "www.supernossoemcasa.com.br");

        saveDataProduct(null, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);

        if (this.arrayProducts.size() == productsLimit) {
          break;
        }
        index++;
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }

  @Override
  protected void setTotalProducts() {
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, "span.resultado-busca-numero span.value", true, 0);
    this.log("Total da busca: " + this.totalProducts);
  }
}
