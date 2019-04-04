package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilRicardoeletroCrawler extends CrawlerRankingKeywords {

  public BrasilRicardoeletroCrawler(Session session) {
    super(session);
    super.fetchMode = FetchMode.APACHE;
  }

  @Override
  public void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 24;

    this.log("Página " + this.currentPage);

    String keyword = this.keywordWithoutAccents.replaceAll(" ", "+");

    // monta a url com a keyword e a página
    String url = "https://www.ricardoeletro.com.br/Busca/Resultado/?p=" + this.currentPage + "&loja=&q=" + keyword;
    this.log("Link onde são feitos os crawlers: " + url);

    // chama função de pegar a url
    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select("div.box-vitrine.content-produto");

    // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
    if (!products.isEmpty()) {
      for (Element e : products) {

        // InternalPid
        String internalPid = crawlInternalPid(e);

        // InternalId
        String internalId = crawlInternalId(e);

        // Url do produto
        String productUrl = crawlProductUrl(e);

        saveDataProduct(internalId, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit)
          break;

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
    Element page = this.currentDoc.select("span.ultima.inativo").first();

    // se elemeno page obtiver algum resultado
    if (page != null) {
      // não tem próxima página
      return false;
    }

    return true;

  }

  private String crawlInternalId(Element e) {
    String internalId = null;
    Element inid = e.select(".foto-produto").first();

    if (inid != null) {
      internalId = inid.attr("data-codigo").trim();
    }

    return internalId;
  }

  private String crawlInternalPid(Element e) {
    String internalPid = null;

    return internalPid;
  }

  private String crawlProductUrl(Element e) {
    String urlProduct = null;
    Element urlElement = e.select(".nome-produto-vertical > a").first();

    if (urlElement != null) {
      urlProduct = urlElement.attr("href");
    }

    return urlProduct;
  }

}
