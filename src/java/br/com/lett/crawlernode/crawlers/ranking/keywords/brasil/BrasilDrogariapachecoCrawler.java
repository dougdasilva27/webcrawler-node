package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilDrogariapachecoCrawler extends CrawlerRankingKeywords {

  public BrasilDrogariapachecoCrawler(Session session) {
    super(session);
  }


  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 15;

    this.log("Página " + this.currentPage);
    String url = "https://www.drogariaspacheco.com.br/" + this.keywordWithoutAccents.replaceAll(" ", "%20") + "?PS=50&PageNumber=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".vitrine.resultItemsWrapper ul > li[layout]");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalPid = crawlInternalPid(e);
        String productUrl = crawlProductUrl(e);

        saveDataProduct(null, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);

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
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.select("span.resultado-busca-numero span.value").first();

    if (totalElement != null) {
      String text = totalElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
      }
    }

    this.log("Total da busca: " + this.totalProducts);
  }

  private String crawlInternalPid(Element e) {
    String internalPid = null;

    Element ids = e.select(".idSku").first();

    if (ids != null) {
      internalPid = ids.text().trim();
    }

    return internalPid;
  }

  private String crawlProductUrl(Element e) {
    String urlProduct = null;
    Element urlElement = e.selectFirst("a.productPrateleira");

    if (urlElement != null) {
      urlProduct = urlElement.attr("href");
    }

    return urlProduct;
  }
}
