package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilSaraivaCrawler extends CrawlerRankingKeywords {

  public BrasilSaraivaCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 44;

    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String url =
        "https://busca.saraiva.com.br/?q=" + this.keywordEncoded + "&page=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    // chama função de pegar o html
    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".nm-product-item");

    // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
    if (products.size() >= 1) {
      // se o total de busca não foi setado ainda, chama a função para setar
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {

        // InternalPid
        String internalPid = crawlInternalPid(e);

        // InternalId
        String internalId = crawlInternalId(e);

        // Url do produto
        String productUrl = crawlProductUrl(e);

        saveDataProduct(internalId, internalPid, productUrl);

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
    // se elemeno page obtiver algum resultado
      // tem próxima página
    return this.arrayProducts.size() < this.totalProducts;
  }

  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.select(".cs-result-count--value").first();

    if (totalElement != null) {
      try {
        this.totalProducts = Integer.parseInt(totalElement.text().replaceAll("[^0-9]", "").trim());
      } catch (Exception e) {
        this.logError(CommonMethods.getStackTraceString(e));
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String crawlInternalId(Element e) {
    return e.attr("data-pid");
  }

  private String crawlInternalPid(Element e) {
    return null;
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;
    Element eUrl = e.select(".nm-product-name a").first();

    if (eUrl != null && !eUrl.attr("href").contains("imaginarium")) {
      productUrl = eUrl.attr("href").trim();

      if (!productUrl.startsWith("http")) {
        productUrl = "https:" + productUrl;
      }
    }

    return productUrl;
  }
}
