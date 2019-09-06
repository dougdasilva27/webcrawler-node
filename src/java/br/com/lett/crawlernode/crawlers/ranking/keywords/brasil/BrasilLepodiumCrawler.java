package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilLepodiumCrawler extends CrawlerRankingKeywords {

  public BrasilLepodiumCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 24;

    this.log("Página " + this.currentPage);

    String url = "https://www.lepodium.com.br/" + this.location;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".list-prod .moveis-estar .ct");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element element : products) {
        String internalId = element.attr("data-id");
        String productUrl = crawlProductUrl(element);
        saveDataProduct(internalId, null, productUrl);
        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);

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

  private String crawlProductUrl(Element element) {
    Element elementUrl = element.selectFirst(".product-info a");
    String productUrl = null;

    if (elementUrl != null) {
      productUrl = elementUrl.attr("href");
    }

    return productUrl;
  }

  @Override
  protected boolean hasNextPage() {
    return this.arrayProducts.size() < this.totalProducts;
  }

  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.select(".neemu-total-products-container").first();

    if (totalElement != null) {
      String text = totalElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
      }
    }

    this.log("Total da busca: " + this.totalProducts);
  }

}

