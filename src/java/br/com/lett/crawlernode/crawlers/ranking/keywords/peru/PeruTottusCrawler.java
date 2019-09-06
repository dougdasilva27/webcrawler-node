package br.com.lett.crawlernode.crawlers.ranking.keywords.peru;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class PeruTottusCrawler extends CrawlerRankingKeywords {

  public PeruTottusCrawler(Session session) {
    super(session);
  }

  private String redirectUrl;

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 15;
    this.log("Página " + this.currentPage);

    String url = "http://www.tottus.com.pe/tottus/search?Ntt=" + this.keywordEncoded;

    if (this.currentPage > 1 && this.redirectUrl != null) {

      if (url.contains("browse")) {

        url = this.redirectUrl.replace("browse", "productListFragment") + "?No=" + this.arrayProducts.size() + "&Nrpp=&currentCatId="
            + CommonMethods.getLast(this.redirectUrl.split("\\?")[0].split("/"));
      } else {
        url = this.redirectUrl + "?No=" + this.arrayProducts.size();
      }
    } else if (this.currentPage > 1) {
      url += "&No=" + this.arrayProducts.size();
    }

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".item-product-caption");

    if (this.currentPage == 1) {
      this.redirectUrl = CrawlerUtils.getRedirectedUrl(url, session);
    }

    if (!products.isEmpty()) {
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
  protected boolean hasNextPage() {
    return this.currentDoc.select(".item-product-caption").size() >= this.pageSize;
  }

  private String crawlInternalId(Element e) {
    String internalId = null;

    Element input = e.selectFirst("input.btn-add-cart");
    if (input != null) {
      internalId = input.val();
    }

    return internalId;
  }

  private String crawlProductUrl(Element e) {
    String url = null;

    Element eUrl = e.selectFirst(".title a");
    if (eUrl != null) {
      url = CrawlerUtils.sanitizeUrl(eUrl, "href", "https:", "www.tottus.com.pe");
    }

    return url;
  }
}
