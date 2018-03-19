package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilLilianiCrawler extends CrawlerRankingKeywords {

  public BrasilLilianiCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 20;

    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String url = "https://busca.liliani.com.br/" + this.keywordWithoutAccents.replace(" ", "%20") + "?pagina=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    // chama função de pegar o html
    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select("#listProduct .hproduct");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProductsCarrefour();
      }

      for (Element e : products) {
        String internalPid = e.attr("data-item-id");
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
    Element totalElement = this.currentDoc.select(".resumo-resultado strong").first();

    if (totalElement != null) {
      String text = totalElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;

    Element url = e.select("a.link.url").first();

    if (url != null) {
      productUrl = url.attr("href");

      if (!productUrl.startsWith("http")) {
        productUrl = ("https://www.liliani.com.br/" + e.attr("href")).replace("br//", "br/");
      }
    }

    return productUrl;
  }
}
