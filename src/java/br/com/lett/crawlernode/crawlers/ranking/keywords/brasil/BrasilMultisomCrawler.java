package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilMultisomCrawler extends CrawlerRankingKeywords {

  public BrasilMultisomCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 24;

    this.log("Página " + this.currentPage);

    String keyword = this.keywordWithoutAccents.replaceAll(" ", "%20");

    // monta a url com a keyword e a página
    String url = "https://www.multisom.com.br/busca/" + keyword + "/page=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    // chama função de pegar a url
    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".listItems li > a:not(.unavailable)");
    Element noResult = this.currentDoc.select(".searchAgain").first();

    // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
    if (!products.isEmpty() && noResult == null) {
      // se o total de busca não foi setado ainda, chama a função para setar
      if (this.totalProducts == 0)
        setTotalProducts();
      for (Element e : products) {

        // InternalPid
        String internalPid = crawlInternalPid(e);

        // InternalId
        String internalId = crawlInternalId(e);

        // Url do produto
        String urlProduct = crawlProductUrl(e);

        saveDataProduct(internalId, internalPid, urlProduct);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
        if (this.arrayProducts.size() == productsLimit)
          break;

      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }

  @Override
  protected boolean hasNextPage() {
    Element lastPage = this.currentDoc.select(".navigation li").last();

    if (lastPage != null && !lastPage.hasClass("current")) {
      return true;
    }

    return false;
  }

  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.select(".result-count strong span").first();

    if (totalElement != null) {
      try {
        this.totalProducts = Integer.parseInt(totalElement.text().replaceAll("[^0-9]", "").trim());
      } catch (Exception e) {
        this.logError(e.getMessage());
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String crawlInternalId(Element e) {
    String internalId = null;

    String url = e.attr("href");
    if (url.contains("-")) {
      String[] tokens = url.split("-");
      internalId = tokens[tokens.length - 1].replaceAll("[^0-9]", "");
    }

    return internalId;
  }

  private String crawlInternalPid(Element e) {
    String internalPid = null;

    return internalPid;
  }

  private String crawlProductUrl(Element e) {
    String urlProduct = null;

    urlProduct = e.attr("href");

    if (!urlProduct.contains("multisom")) {
      urlProduct = "https://www.multisom.com.br" + e.attr("href");
    }


    return urlProduct;
  }
}
