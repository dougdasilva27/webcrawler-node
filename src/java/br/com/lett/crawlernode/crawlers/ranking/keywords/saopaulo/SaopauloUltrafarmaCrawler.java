package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class SaopauloUltrafarmaCrawler extends CrawlerRankingKeywords {

  public SaopauloUltrafarmaCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 12;

    this.log("Página " + this.currentPage);
    String url = "https://busca.ultrafarma.com.br/search?w=" + this.keywordEncoded + "&srt=" + this.arrayProducts.size();
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".conj_prod_categorias");

    if (!products.isEmpty()) {
      for (Element e : products) {
        String productUrl = crawlProductUrl(e);
        String internalId = crawlInternalId(e);

        saveDataProduct(internalId, null, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
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
    Element page = this.currentDoc.select(".paginacao li a").last();
    return page == null || !page.hasClass("ativo");
  }

  private String crawlInternalId(Element e) {
    String internalId = null;

    Element id = e.selectFirst(".preco_lista_prod[data-id]");
    if (id != null) {
      String text = id.attr("data-id");

      if (text.contains("_")) {
        internalId = CommonMethods.getLast(text.split("_"));
      }
    }

    return internalId;
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;

    Element url = e.selectFirst("a.nome_produtos_vitrine");
    if (url != null) {
      productUrl = url.attr("title");
    }

    return productUrl;
  }
}
