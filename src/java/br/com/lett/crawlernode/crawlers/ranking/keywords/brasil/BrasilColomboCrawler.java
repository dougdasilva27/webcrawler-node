package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilColomboCrawler extends CrawlerRankingKeywords {

  public BrasilColomboCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 20;

    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String url = "http://busca.colombo.com.br/search?televendas=&p=Q&srid=S12-USESD02&lbc=colombo&ts=ajax&w=" + this.keywordEncoded
        + "&uid=687778702&method=and&isort=score&view=grid&srt=" + this.arrayProducts.size();
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select("div.produto-info-content");

    // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
    if (!products.isEmpty()) {
      for (Element e : products) {
        // InternalPid
        String internalPid = crawlInternalPid(e);

        // Url do produto
        String urlProduct = crawlProductUrl(e);

        saveDataProduct(null, internalPid, urlProduct);

        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
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
    return this.currentDoc.select("div.produto-info-content").size() >= this.pageSize;
  }


  private String crawlInternalPid(Element e) {
    String internalPid = null;
    Element pid = e.select(".produto-info-content > script").first();

    if (pid != null) {
      internalPid = pid.html().replaceAll("[^0-9]", "").trim();
    }

    return internalPid;
  }

  private String crawlProductUrl(Element e) {
    String urlProduct = null;
    Element urlElement = e.select(".produto-descricao a").first();

    if (urlElement != null) {
      urlProduct = urlElement.attr("title");

      if (!urlProduct.contains("colombo")) {
        urlProduct = ("http://www.colombo.com.br/" + urlElement.attr("title")).replace("br//", "br/");
      }
    }

    return urlProduct;
  }
}
