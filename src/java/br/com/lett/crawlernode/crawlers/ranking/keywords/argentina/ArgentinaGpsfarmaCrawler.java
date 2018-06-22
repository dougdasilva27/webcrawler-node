package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class ArgentinaGpsfarmaCrawler extends CrawlerRankingKeywords {

  public ArgentinaGpsfarmaCrawler(Session session) {
    super(session);
  }

  private String categoryUrl;

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 16;
    this.log("Página " + this.currentPage);

    String url = "https://www.gpsfarma.com/catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordEncoded;

    if (this.currentPage > 1 && this.categoryUrl != null) {
      url = this.categoryUrl + "?p=" + this.currentPage;
    }

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".products-grid .item");

    if (this.currentPage == 1) {
      String redirectUrl = CrawlerUtils.crawlFinalUrl(url, session);

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
    Element totalElement = this.currentDoc.select(".count-container .amount").first();

    if (totalElement != null) {
      String text = totalElement.ownText();

      if (text.contains("de")) {
        String totalText = CommonMethods.getLast(text.split("de")).replaceAll("[^0-9]", "").trim();

        if (!totalText.isEmpty()) {
          this.totalProducts = Integer.parseInt(totalText);
        }
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String crawlInternalId(Element e) {
    String internalId = null;

    String text = e.attr("id");
    if (text.contains("-")) {
      internalId = CommonMethods.getLast(text.split("-")).trim();
    }

    return internalId;
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;
    Element eUrl = e.select(" > a").first();

    if (eUrl != null) {
      productUrl = eUrl.attr("href");
    }

    return productUrl;
  }
}
