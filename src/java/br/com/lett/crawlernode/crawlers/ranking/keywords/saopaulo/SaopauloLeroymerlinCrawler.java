package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class SaopauloLeroymerlinCrawler extends CrawlerRankingKeywords {

  public SaopauloLeroymerlinCrawler(Session session) {
    super(session);
  }

  private String nextUrl;

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 40;

    this.log("Página " + this.currentPage);

    if (this.currentPage == 1) {
      String url = "https://www.leroymerlin.com.br/search?term=" + this.keywordEncoded;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url, null);

      this.nextUrl = getNextUrl(this.nextUrl);
      if (!url.equals(this.nextUrl)) {
        this.currentDoc = fetchDocument(this.nextUrl);
      }

    } else {
      String url;
      if (this.nextUrl.contains("?")) {
        url = this.nextUrl + "&page=" + this.currentPage;
      } else {
        url = this.nextUrl + "?page=" + this.currentPage;
      }

      // chama função de pegar o html
      this.currentDoc = fetchDocument(url, null);
      this.log("Link onde são feitos os crawlers: " + url);
    }

    Elements products = this.currentDoc.select(".product-col .product-thumb .caption > a:first-child");

    // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }
      for (Element e : products) {
        String productUrl = e.attr("href");
        String internalId = crawlInternalId(productUrl);

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
  }

  @Override
  protected boolean hasNextPage() {
    return this.currentDoc.select("a.pagination-item.disabled > i.glyph-arrow-right").isEmpty();
  }

  private String crawlInternalId(String url) {
    String internalId = null;

    if (url.contains("?")) {
      url = url.split("\\?")[0];
    }

    if (url.contains("_")) {
      internalId = CommonMethods.getLast(url.split("_"));
    }

    return internalId;
  }

  private String getNextUrl(String url) {
    String newUrl = url;

    if (!this.currentDoc.select(".content-header .expandable-description").isEmpty()) {
      Elements categories = this.currentDoc.select("div.categories-col a");

      for (Element e : categories) {
        String title = e.attr("title").toLowerCase();
        String categoryUrl = e.attr("href");

        if (title.contains("ver todos")) {
          newUrl = categoryUrl;
          break;
        }
      }
    } else {
      String redirectUrl = CrawlerUtils.crawlFinalUrl(url, session);

      if (redirectUrl != null) {
        newUrl = redirectUrl;
      }
    }

    return newUrl;
  }
}
