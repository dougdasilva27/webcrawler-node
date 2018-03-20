package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilHomerefillCrawler extends CrawlerRankingKeywords {

  public BrasilHomerefillCrawler(Session session) {
    super(session);
  }

  private String redirectUrl;

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 20;

    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String url = "https://www.homerefill.com.br/shopping/search?search=" + this.keywordEncoded;

    if (this.currentPage == 1) {
      this.currentDoc = fetchDocument(url);

      this.redirectUrl = session.getRedirectedToURL(url) != null ? session.getRedirectedToURL(url) : url;
    } else {
      this.currentDoc = fetchDocument(this.redirectUrl + "&page=" + this.currentPage);
    }

    this.log("Link onde são feitos os crawlers: " + this.redirectUrl + "&page=" + this.currentPage);

    Elements products = this.currentDoc.select(".page-department .organism-product .column div[data-product-sku]");
    Element emptySearch = this.currentDoc.select(".page-department__suggests").first();

    if (!products.isEmpty() && emptySearch == null) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {

        // InternalPid
        String internalPid = crawlInternalPid();

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
    return this.arrayProducts.size() < this.totalProducts;
  }

  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.select("h2.page-search__header__title").first();

    if (totalElement != null) {
      String text = totalElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String crawlInternalId(Element e) {
    return e.attr("data-product-sku");
  }

  private String crawlInternalPid() {
    return null;
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;
    Element eUrl = e.select("a[href]").first();

    if (eUrl != null) {
      productUrl = eUrl.attr("href");
    }

    return productUrl;
  }
}
