package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class ArgentinaCarrefoursuperCrawler extends CrawlerRankingKeywords {

  public ArgentinaCarrefoursuperCrawler(Session session) {
    super(session);
  }

  private static final String CEP = "1646";
  private static final String HOST = "supermercado.carrefour.com.ar";
  private String categoryUrl;

  @Override
  protected void processBeforeFetch() {
    super.processBeforeFetch();

    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");

    String payload = "codigo_postal=" + CEP;

    Request request = RequestBuilder.create()
        .setUrl("https://supermercado.carrefour.com.ar/stock/")
        .setCookies(cookies)
        .setPayload(payload)
        .setFollowRedirects(false)
        .setBodyIsRequired(false)
        .build();

    List<Cookie> cookiesResponse = this.dataFetcher.post(session, request).getCookies();
    for (Cookie c : cookiesResponse) {
      BasicClientCookie cookie = new BasicClientCookie(c.getName(), c.getValue());
      cookie.setDomain(HOST);
      cookie.setPath("/");
      this.cookies.add(cookie);
    }
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 10;
    this.log("Página " + this.currentPage);

    String url = "https://" + HOST + "/catalogsearch/result/?limit=36&q=" + this.keywordEncoded + "&p=" + this.currentPage;

    if (this.currentPage > 1 && this.categoryUrl != null) {
      url = this.categoryUrl + "?p=" + this.currentPage;
    }

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".home-product-cards .product-card .producto-info .open-modal[data-id]");

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

        String internalId = e.attr("data-id");
        String productUrl = CrawlerUtils.completeUrl(e.attr("href"), "https", HOST);

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
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".resultados-count", false, 0);
    this.log("Total da busca: " + this.totalProducts);
  }
}
