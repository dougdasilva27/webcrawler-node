package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Document;
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

    String url = "https://www.lepodium.com.br/buscapagina?ft=" + this.location
        + "&PS=24&sl=d9cf4574-2102-48f5-9219-ad8da4285fbf&cc=3&sm=0&PageNumber="
        + this.currentPage;
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
    Document doc = fetchDocument("https://www.lepodium.com.br/" + this.location);
    Element totalElement = doc.select(".searchResultsTime .value").first();

    if (totalElement != null) {
      String text = totalElement.ownText().replaceAll("[^0-9]", "").trim();
      System.err.println(text);
      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
      }
    }

    this.log("Total da busca: " + this.totalProducts);
  }

}

