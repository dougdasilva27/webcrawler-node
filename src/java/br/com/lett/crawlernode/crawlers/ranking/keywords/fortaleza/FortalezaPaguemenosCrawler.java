package br.com.lett.crawlernode.crawlers.ranking.keywords.fortaleza;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class FortalezaPaguemenosCrawler extends CrawlerRankingKeywords {

  public FortalezaPaguemenosCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 48;

    this.log("Página " + this.currentPage);

    // se a key contiver o +, substitui por %20, pois nesse market a pesquisa na url é assim
    String url = "https://www.paguemenos.com.br/" + this.keywordWithoutAccents.replaceAll(" ", "%20") + "?PS=50&PageNumber=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select("div.vitrine.resultItemsWrapper div.prateleira > ul > li[layout]");
    Elements ids = this.currentDoc.select("div.vitrine.resultItemsWrapper div.prateleira > ul > li[id].helperComplement");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (int i = 0; i < products.size(); i++) {
        String internalPid = crawlInternalPid(ids.get(i));
        String productUrl = crawlProductUrl(products.get(i));

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

  @Override
  protected boolean hasNextPage() {
    return this.arrayProducts.size() < this.totalProducts;
  }

  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.select("span.resultado-busca-numero span.value").first();

    if (totalElement != null) {
      String text = totalElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
      }
    }

    this.log("Total da busca: " + this.totalProducts);
  }

  private String crawlInternalPid(Element e) {
    return e.attr("id").split("_")[1];
  }

  private String crawlProductUrl(Element e) {
    String urlProduct = null;
    Element urlElement = e.select(".product-content > a").first();

    if (urlElement != null) {
      urlProduct = urlElement.attr("href");
    }

    return urlProduct;
  }
}
