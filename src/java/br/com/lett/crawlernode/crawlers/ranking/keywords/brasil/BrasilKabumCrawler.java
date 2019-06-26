package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilKabumCrawler extends CrawlerRankingKeywords {

  public BrasilKabumCrawler(Session session) {
    super(session);
  }

  private static final String PRODUCTS_SELECTOR = "div.listagem-titulo_descr .H-titulo a[href]";

  @Override
  protected void processBeforeFetch() {
    super.processBeforeFetch();
    this.cookies = CrawlerUtils.fetchCookiesFromAPage("https://www.kabum.com.br/", null, ".kabum.com.br", "/", cookies, session, null, dataFetcher);
  }

  private String baseUrl;
  private boolean isCategory = false;

  @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);
    this.pageSize = 30;

    String url;
    if (!isCategory) {
      url = "https://www.kabum.com.br/cgi-local/site/listagem/listagem.cgi?string=" + this.keywordEncoded + "&pagina=" + this.currentPage;
    } else {
      url = this.baseUrl + "&pagina=" + this.currentPage;
    }

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);

    if (this.currentPage == 1) {
      this.baseUrl = CrawlerUtils.completeUrl(this.session.getRedirectedToURL(url), "https", "www.kabum.com.br");
      this.isCategory = this.baseUrl != null && !url.equalsIgnoreCase(baseUrl);
    }

    Elements products = this.currentDoc.select(PRODUCTS_SELECTOR);

    if (!products.isEmpty()) {
      for (Element e : products) {
        String urlProduct = CrawlerUtils.completeUrl(e.attr("href"), "https", "www.kabum.com.br");
        if (urlProduct.contains("?")) {
          urlProduct = urlProduct.split("\\?")[0];
        }

        String[] tokens = urlProduct.split("/");
        String internalId = tokens[tokens.length - 2];

        saveDataProduct(internalId, null, urlProduct);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + urlProduct);
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
    return this.currentDoc.select(PRODUCTS_SELECTOR).size() >= this.pageSize;
  }
}
