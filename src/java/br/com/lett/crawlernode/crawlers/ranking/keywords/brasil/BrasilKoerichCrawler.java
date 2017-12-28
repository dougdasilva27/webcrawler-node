package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilKoerichCrawler extends CrawlerRankingKeywords {

  public BrasilKoerichCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);

    // número de produtos por página do market
    this.pageSize = 15;

    String keyword = this.keywordWithoutAccents.replaceAll(" ", "%20");

    // monta a url com a keyword e a página
    String url =
        "http://www.koerich.com.br/" + keyword + "?PageNumber=" + this.currentPage + "&PS=50";
    this.log("Link onde são feitos os crawlers: " + url);

    // chama função de pegar a url
    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".vitrine .prateleira ul li[layout]");
    Elements ids = this.currentDoc.select(".vitrine .prateleira ul li[id]");

    // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
    if (!products.isEmpty() && products.size() == ids.size()) {
      // se o total de busca não foi setado ainda, chama a função para setar
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (int i = 0; i < products.size(); i++) {
        // InternalPid
        String internalPid = crawlInternalPid(ids.get(i));

        // Url do produto
        String productUrl = crawlProductUrl(products.get(i));

        saveDataProduct(null, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: "
            + internalPid + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit) {
          break;
        }

      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
        + this.arrayProducts.size() + " produtos crawleados");
  }

  @Override
  protected boolean hasNextPage() {
    return arrayProducts.size() < this.totalProducts;
  }

  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.select("span.resultado-busca-numero span.value").first();
    if (totalElement != null) {
      try {
        this.totalProducts = Integer.parseInt(totalElement.text());
      } catch (Exception e) {
        this.logError(CommonMethods.getStackTrace(e));
      }
    }

    this.log("Total da busca: " + this.totalProducts);
  }

  private String crawlInternalPid(Element e) {
    String[] tokens = e.attr("id").split("_");

    return tokens[tokens.length - 1];
  }

  private String crawlProductUrl(Element e) {
    String urlProduct = null;
    Element urlElement = e.select(".product-image_principal").first();

    if (urlElement != null) {
      urlProduct = urlElement.attr("href");
    }

    return urlProduct;
  }

}
