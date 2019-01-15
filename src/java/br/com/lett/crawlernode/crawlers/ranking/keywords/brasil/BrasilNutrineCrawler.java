package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilNutrineCrawler extends CrawlerRankingKeywords {
  public BrasilNutrineCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 36;

    this.log("Página " + this.currentPage);

    String key = this.keywordWithoutAccents.replaceAll(" ", "%20");
    String url = "https://www.nutrine.com.br/Busca?q=" + key + "&Enviar=OK&tamanho=36&pagina="
        + this.currentPage;

    this.log("Url: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".linha .produtoItem");

    if (products.size() >= 1) {

      for (Element e : products) {
        String internalPid = null;
        String internalId = getProductInternalId(e, "data-id");
        String productUrl = getProductUrl(e, "a.produtoImagem");

        saveDataProduct(internalId, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: "
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
    Element e = this.currentDoc.select(".paginacao p a").last();
    boolean answer = false;

    if (e != null) {
      answer = !e.hasClass("atual");
    }

    return answer;
  }

  private String getProductInternalId(Element e, String selector) {
    return e.attr(selector);
  }

  private String getProductUrl(Element e, String selector) {
    Element el = e.selectFirst(selector);
    String url = null;

    if (el != null) {
      url = CrawlerUtils.sanitizeUrl(el, "href", "https", "www.nutrine.com.br");
    }

    return url;
  }
}
