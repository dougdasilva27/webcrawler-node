package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class ArgentinaCarrefourCrawler extends CrawlerRankingKeywords {

  public ArgentinaCarrefourCrawler(Session session) {
    super(session);
  }

  private String categoryUrl;

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 9;
    this.log("Página " + this.currentPage);

    String url = "https://www.carrefour.com.ar/catalogsearch/result/index/?limit=36&p=" + this.currentPage + "&q=" + this.keywordEncoded;

    if (this.currentPage > 1 && this.categoryUrl != null) {
      url = this.categoryUrl + "?p=" + this.currentPage;
    }

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".products-grid .item > a");

    if (this.currentPage == 1) {
      String redirectUrl = CrawlerUtils.getRedirectedUrl(url, session);

      if (!url.equals(redirectUrl)) {
        this.categoryUrl = redirectUrl;
      }
    }

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }
      for (Element e : products) {

        String internalId = crawlInternalId(e);
        String productUrl = crawlProductUrl(e);

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
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.select(".toolbar .amount strong").first();

    if (totalElement != null) {
      String text = totalElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String crawlInternalId(Element e) {
    String internalId = null;

    Element img = e.select("> img").first();
    if (img != null) {
      String text = img.attr("id");

      if (text.contains("-")) {
        internalId = CommonMethods.getLast(text.split("-"));
      }
    }

    return internalId;
  }

  private String crawlProductUrl(Element e) {
    return e.attr("href");
  }
}
