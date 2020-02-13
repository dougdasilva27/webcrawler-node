package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilVitaesaudeCrawler extends CrawlerRankingKeywords {

  public BrasilVitaesaudeCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 9;

    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String url = "https://www.vitaesaude.com.br/search?search_query=" + this.keywordEncoded + "&page=" + this.currentPage + "&ajax=1";
    this.log("Link onde são feitos os crawlers: " + url);

    // chama função de pegar o html
    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".ProductList li");

    // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
    if (!products.isEmpty()) {
      for (Element e : products) {
        // InternalPid
        String internalPid = crawlInternalPid(e);

        // InternalId
        String internalId = null;

        // Url do produto
        String productUrl = crawlProductUrl(e);

        saveDataProduct(internalId, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit) {
          break;
        }

      }
    } else {
      setTotalProducts();
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    if (!hasNextPage()) {
      setTotalProducts();
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }

  @Override
  protected boolean hasNextPage() {
    Element lastPageElement = this.currentDoc.select(".PagingList > li").last();

    if (lastPageElement != null) {
      String text = lastPageElement.text().replaceAll("[^0-9]", "");

      return !text.isEmpty() && (this.currentPage < Integer.parseInt(text));
    }

    return false;
  }

  private String crawlInternalPid(Element e) {
    String internalPid = null;
    Element idElement = e.select(".ProductActionAdd a").first();

    if (idElement != null) {
      String[] tokens = idElement.attr("href").split("=");
      String id = tokens[tokens.length - 1].replaceAll("[^0-9]", "").trim();

      if (!id.isEmpty()) {
        internalPid = id;
      }
    }

    return internalPid;
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;
    Element url = e.select(".prod_nome > a").first();

    if (url != null) {
      productUrl = url.attr("href");

      if (!productUrl.contains("vitaesaude")) {
        productUrl = "https://www.vitaesaude.com.br/" + productUrl;
      }
    }

    return productUrl;
  }
}
