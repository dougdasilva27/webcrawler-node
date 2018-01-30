package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilMedicamentosbrasilCrawler extends CrawlerRankingKeywords {

  public BrasilMedicamentosbrasilCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 21;

    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String url = "https://www.medicamentosbrasil.com.br/search?q=" + this.keywordEncoded + "&page=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    // chama função de pegar o html
    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select("#products > .row > div[class]");
    Element emptySearch = this.currentDoc.select("#products > .row > div > h3").first();

    // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
    if (!products.isEmpty() && emptySearch == null) {
      for (Element e : products) {
        // InternalPid
        String internalPid = null;

        // InternalId
        String internalId = crawlInternalId(e);

        // Url do produto
        String productUrl = crawlProductUrl(e);

        saveDataProduct(internalId, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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
    Element lastPage = this.currentDoc.select(".pager ul li > a").last();

    if (lastPage != null && !lastPage.hasAttr("style")) {
      return true;
    }

    return false;
  }

  private String crawlInternalId(Element e) {
    String internalId = null;
    Element idElement = e.select("a[id]").first();

    if (idElement != null) {
      String[] tokens = idElement.attr("id").split("-");
      String id = tokens[tokens.length - 1].replaceAll("[^0-9]", "").trim();

      if (!id.isEmpty()) {
        internalId = id;
      }
    }

    return internalId;
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;
    Element url = e.select("a[href]").first();

    if (url != null) {
      productUrl = url.attr("href");

      if (!productUrl.startsWith("https://www.medicamentosbrasil.com.br/")) {
        productUrl = ("https://www.medicamentosbrasil.com.br/" + productUrl).replace("br//", "br/");
      }
    }

    return productUrl;
  }
}
