package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilMultiarCrawler extends CrawlerRankingKeywords {

  public BrasilMultiarCrawler(Session session) {
    super(session);
  }

  private boolean isCategory;

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 24;

    this.log("Página " + this.currentPage);

    String key = this.keywordWithoutAccents.replaceAll(" ", "%20");

    // monta a url com a keyword e a página
    String url = "http://www.multiar.com.br/" + key;

    if (this.currentPage > 1) {
      if (!this.isCategory) {
        url = "http://www.multiar.com.br/buscapagina?ft=" + key + "&PS=24" + "&sl=379a0b55-f6f6-4127-8d29-0be97c93ba9a&cc=0&sm=0&PageNumber="
            + this.currentPage;
      }
    }

    this.log("Link onde são feitos os crawlers: " + url);

    // chama função de pegar a url
    this.currentDoc = fetchDocument(url);

    Element elementIsCategory = this.currentDoc.select("div.bread-crumb > ul > li.last").first();

    if (this.currentPage == 1) {
      if (elementIsCategory != null)
        this.isCategory = true;
      else
        this.isCategory = false;
    }

    Elements products = this.currentDoc.select("div.prateleira.vitrine > ul > li");

    // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
    if (!products.isEmpty()) {
      for (int i = 0; i < products.size(); i++) {
        if (products.get(i).hasAttr("layout")) {
          String[] tokens = products.get(i + 1).attr("id").split("_");
          String internalPid = CommonMethods.getLast(tokens);
          String internalId = null;

          // monta a url
          Element eUrl = products.get(i).select("> a[title]").first();
          String productUrl = eUrl.attr("href");

          saveDataProduct(internalId, internalPid, productUrl);

          this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
          if (this.arrayProducts.size() == productsLimit)
            break;
        }
      }
    } else {
      setTotalProducts();
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
    if (!(hasNextPage()))
      setTotalProducts();
  }

  @Override
  protected boolean hasNextPage() {
    Elements ids = this.currentDoc.select("div.prateleira.vitrine > ul > li[layout]");

    if (this.currentPage > 1) {
      if (ids.size() < 24) {
        return false;
      } else {
        return true;
      }
    } else {
      if (this.isCategory) {
        return false;
      } else {
        if (ids.size() < 24) {
          return false;
        } else {
          return true;
        }
      }
    }
  }

}
