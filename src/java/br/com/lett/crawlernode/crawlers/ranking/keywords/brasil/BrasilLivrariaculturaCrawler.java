package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilLivrariaculturaCrawler extends CrawlerRankingKeywords {

  public BrasilLivrariaculturaCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 48;

    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String url = "https://www.livrariacultura.com.br/busca?N=0&Ntt=" + this.keywordEncoded + "&No=" + this.arrayProducts.size();
    this.log("Link onde são feitos os crawlers: " + url);

    // chama função de pegar o html
    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".product-new-box .product-title-new > a");

    // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProductsCarrefour();
      }

      for (Element e : products) {

        String internalId = e.attr("data-search-track-ref");
        String productUrl = e.attr("href");

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

  protected void setTotalProductsCarrefour() {
    Element totalElement = this.currentDoc.select(".container h1 > b").first();

    if (totalElement != null) {
      String text = totalElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }
}
