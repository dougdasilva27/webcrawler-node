package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class SaopauloPanvelCrawler extends CrawlerRankingKeywords {

  public SaopauloPanvelCrawler(Session session) {
    super(session);
  }

  private String crawlInternalId(String url) {
    String internalId = null;

    String[] tokens = url.split("=");
    internalId = tokens[tokens.length - 1];

    return internalId.split("&")[0];
  }

  private String crawlInternalPid(Element e) {
    String internalPid = null;

    return internalPid;
  }

  private String crawlProductUrl(Element e) {
    String productUrl = e.attr("href");

    if (!productUrl.startsWith("https://www.panvel.com")) {
      productUrl = "https://www.panvel.com" + productUrl;
    }

    return productUrl;
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 15;

    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String url = "http://www.panvel.com/panvel/buscarProduto.do?paginaAtual=" + this.currentPage + "&tipo=bar&termoPesquisa="
        + this.keywordWithoutAccents.replace(" ", "+");
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select("a.lnk_mais_detalhes.gsaLink");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String urlProduct = crawlProductUrl(e);
        String internalPid = crawlInternalPid(e);
        String internalId = crawlInternalId(urlProduct);

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
    Element lastPage = this.currentDoc.select(".pagination li > a").last();

    if (lastPage != null) {
      if (lastPage.hasClass("selected")) {
        return false;
      }

      return true;
    }

    return false;
  }

  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.select("div.pag p").first();

    if (totalElement != null) {
      try {
        int x = totalElement.text().indexOf("de");

        String token = totalElement.text().substring(x + 2).trim();

        this.totalProducts = Integer.parseInt(token);
      } catch (Exception e) {
        this.logError(CommonMethods.getStackTrace(e));
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }
}
