package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilPichauCrawler extends CrawlerRankingKeywords {

  public BrasilPichauCrawler(Session session) {
    super(session);
  }

  private String categoryUrl;

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 16;
    this.log("Página " + this.currentPage);

    String url = "https://www.pichau.com.br/catalogsearch/result/index/?product_list_limit=48&p=" + this.currentPage + "&q=" + this.keywordEncoded;

    if (this.currentPage > 1 && this.categoryUrl != null) {
      url = this.categoryUrl + "?p=" + this.currentPage;
    }

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".results .product-item");

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
    Element totalElement = this.currentDoc.select("#toolbar-amount .toolbar-number").first();

    if (totalElement != null) {
      String text = totalElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
        this.log("Total da busca: " + this.totalProducts);
      }
    }
  }

  private String crawlInternalId(Element e) {
    String internalId = null;

    Element price = e.selectFirst(".product.details [data-product-id]");
    if (price != null) {
      internalId = price.attr("data-product-id");
    }

    return internalId;
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;

    Element link = e.selectFirst(".product-item-info > a");
    if (link != null) {
      productUrl = CrawlerUtils.sanitizeUrl(link, "href", "https:", "www.pichau.com.br");
    }

    return productUrl;
  }
}
