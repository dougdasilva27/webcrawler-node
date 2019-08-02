package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class MexicoHebCrawler extends CrawlerRankingKeywords {

  public MexicoHebCrawler(Session session) {
    super(session);
  }

  private String categoryUrl;

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 12;
    this.log("Página " + this.currentPage);

    String url = "https://www.heb.com.mx/catalogsearch/result/index/?limit=36&p=" + this.currentPage + "&q=" + this.keywordEncoded;

    if (this.currentPage > 1 && this.categoryUrl != null) {
      url = this.categoryUrl + "?p=" + this.currentPage;
    }

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".products-grid .item");

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
        String internalPid = crawlInternalPid(e);
        String productUrl = crawlProductUrl(e);

        saveDataProduct(null, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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
    Element totalElement = this.currentDoc.selectFirst(".amount .catalog-qty");

    if (totalElement != null) {
      String text = totalElement.text().replaceAll("[^0-9]", "");

      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String crawlInternalPid(Element e) {
    String internalPid = null;

    Element price = e.selectFirst(".price-box span[id]");
    if (price != null) {
      String text = price.attr("id");

      if (text.contains("-")) {
        internalPid = CommonMethods.getLast(text.split("-"));
      }
    }

    return internalPid;
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;

    Element urlElement = e.selectFirst(".product-name a");
    if (urlElement != null) {
      productUrl = CrawlerUtils.sanitizeUrl(urlElement, "href", "https:", "www.heb.com.mx");
    }

    return productUrl;
  }
}
