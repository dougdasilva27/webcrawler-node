package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilDrogariaprimusCrawler extends CrawlerRankingKeywords {

  public BrasilDrogariaprimusCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 21;

    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String url = "https://www.drogariaprimus.com.br/pesquisa/?p=" + this.keywordEncoded + "&pagina="
        + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    // chama função de pegar o html
    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".products li[id^=li-productid-]");

    // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
    if (!products.isEmpty()) {
      if (totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        // InternalPid
        String internalPid = null;

        // InternalId
        String internalId = crawlInternalId(e);

        // Url do produto
        String productUrl = crawlProductUrl(e);

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
    return this.currentDoc.select(".page-next").first() != null;
  }

  @Override
  protected void setTotalProducts() {
    Element total = this.currentDoc.select(".results span").last();

    if (total != null) {
      String totalText = total.ownText().replaceAll("[^0-9]", "").trim();

      if (!totalText.isEmpty()) {
        this.totalProducts = Integer.parseInt(totalText);
      }
    }

    this.log("Total products: " + this.totalProducts);
  }

  private String crawlInternalId(Element e) {
    String[] tokens = e.attr("id").split("-");
    return tokens[tokens.length - 1];
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;
    Element url = e.select(".product-name a").first();

    if (url != null) {
      productUrl = url.attr("href");

      if (!productUrl.startsWith("http")) {
        productUrl = "https://www.drogariaprimus.com.br/" + productUrl;
      }
    }

    return productUrl;
  }
}
