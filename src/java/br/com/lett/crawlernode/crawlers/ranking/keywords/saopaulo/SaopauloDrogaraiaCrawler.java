package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class SaopauloDrogaraiaCrawler extends CrawlerRankingKeywords {

  public SaopauloDrogaraiaCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 12;

    this.log("Página " + this.currentPage);
    String url = "https://busca.drogaraia.com.br/search?w=" + this.keywordEncoded + "&cnt=150&srt=" + this.arrayProducts.size();
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".item div.container:not(.min-limit)");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalId = crawlInternalId(e);
        String productUrl = crawlProductUrl(e);

        saveDataProduct(internalId, null, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit) {
          break;
        }
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
    if (!(hasNextPage()))
      setTotalProducts();
  }


  private String crawlInternalId(Element e) {
    String internalId = null;
    Element id = e.selectFirst(".trustvox-shelf-container[data-trustvox-product-code]");

    if (id != null) {
      internalId = id.attr("data-trustvox-product-code");
    }

    return internalId;
  }

  private String crawlProductUrl(Element e) {
    String urlProduct = null;
    Element urlElement = e.selectFirst(".product-name.sli_title a");

    if (urlElement != null) {
      urlProduct = urlElement.attr("title");
    }

    return urlProduct;
  }

  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.selectFirst("p.amount");

    if (totalElement != null) {
      String token = totalElement.text().replaceAll("[^0-9]", "").trim();

      if (!token.isEmpty()) {
        this.totalProducts = Integer.parseInt(token);
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }
}
