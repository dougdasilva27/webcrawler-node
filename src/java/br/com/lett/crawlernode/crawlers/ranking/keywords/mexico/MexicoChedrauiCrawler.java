package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class MexicoChedrauiCrawler extends CrawlerRankingKeywords {

  public MexicoChedrauiCrawler(Session session) {
    super(session);
  }

  private boolean isCategory;
  private String urlCategory;

  @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página

    String url = "https://www.chedraui.com.mx/search/?text=" + this.keywordEncoded;

    if (this.currentPage > 1 && isCategory) {
      url = this.urlCategory + "?q=%3Arelevance&page=" + (this.currentPage - 1) + "&pageSize=24";
    } else if (this.currentPage > 1) {
      url = "https://www.chedraui.com.mx/search?q=" + this.keywordEncoded + "%3Arelevance&page=" + (this.currentPage - 1) + "&pageSize=24";
    }

    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    if (this.currentPage == 1) {
      String redirectUrl = this.session.getRedirectedToURL(url);

      if (redirectUrl != null && currentDoc.select(".breadcrumb li").size() > 1) {
        isCategory = true;
        this.urlCategory = redirectUrl.split("\\?")[0];
      } else {
        isCategory = false;
      }
    }

    Elements products = this.currentDoc.select(".product__list--item > a[title]");

    if (!products.isEmpty()) {

      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String productUrl = crawlProductUrl(e);
        String internalPid = crawlInternalPid(productUrl);
        String internalId = null;

        saveDataProduct(internalId, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit)
          break;
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.pageSize = 24;

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

  }

  @Override
  protected boolean hasNextPage() {
    return this.arrayProducts.size() < this.totalProducts;
  }

  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.select(".pagination-bar-results").first();

    if (totalElement != null) {
      String text = totalElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
      }
    }

    this.log("Total da busca: " + this.totalProducts);
  }

  private String crawlInternalPid(String url) {
    String internalPid = null;

    String[] tokens = url.split("\\?")[0].split("/p");
    internalPid = tokens[tokens.length - 1].replaceAll("[^0-9]", "");

    return internalPid;
  }

  private String crawlProductUrl(Element e) {
    String productUrl = e.attr("href");

    if (!productUrl.contains("http")) {
      productUrl = ("https://www.chedraui.com.mx/" + productUrl).replace(".mx//", ".mx/");
    }

    return productUrl;
  }
}
