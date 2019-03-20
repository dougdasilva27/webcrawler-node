package br.com.lett.crawlernode.crawlers.ranking.keywords.campogrande;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcherNO;
import br.com.lett.crawlernode.core.fetcher.LettProxy;
import br.com.lett.crawlernode.core.fetcher.methods.GETFetcher;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class CampograndeComperCrawler extends CrawlerRankingKeywords {

  public CampograndeComperCrawler(Session session) {
    super(session);
  }

  private static final String HOME_PAGE = "https://www.comperdelivery.com.br/";
  private List<Cookie> cookies = new ArrayList<>();

  private String userAgent;

  @Override
  protected void processBeforeFetch() {
    this.userAgent = DataFetcherNO.randUserAgent();

    Map<String, String> cookiesMap = DataFetcherNO.fetchCookies(session, HOME_PAGE, cookies, this.userAgent, 1);

    for (Entry<String, String> entry : cookiesMap.entrySet()) {
      BasicClientCookie cookie = new BasicClientCookie(entry.getKey(), entry.getValue());
      cookie.setDomain("www.comperdelivery.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);
    }

    Map<String, String> cookiesMap2 =
        DataFetcherNO.fetchCookies(session, "https://www.comperdelivery.com.br/store/SetStore?storeId=6602", cookies, this.userAgent, 1);
    for (Entry<String, String> entry : cookiesMap2.entrySet()) {
      BasicClientCookie cookie = new BasicClientCookie(entry.getKey(), entry.getValue());
      cookie.setDomain("www.comperdelivery.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);
    }
  }

  @Override
  public void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 20;

    this.log("Página " + this.currentPage);

    String specialKeywrod = this.keywordWithoutAccents.replace(" ", "-");
    String url = "https://www.comperdelivery.com.br/busca/3/0/0/MaisVendidos/Decrescente/20/" + this.currentPage + "/0/0/" + specialKeywrod
        + ".aspx?q=" + specialKeywrod;

    LettProxy proxy = session.getRequestProxy("https://www.comperdelivery.com.br/store/SetStore?storeId=6602");
    this.currentDoc = Jsoup.parse(GETFetcher.fetchPageGET(session, url, cookies, this.userAgent, proxy, 1));
    this.log("Link onde são feitos os crawlers: " + url);

    Elements products = this.currentDoc.select("ul#listProduct > li .url[title]");

    // se essa página tiver resultado faça:
    if (!products.isEmpty()) {
      // se o total de busca não foi setado ainda, chama a função para setar
      if (this.totalProducts == 0)
        setTotalProducts();

      for (Element e : products) {
        // Url do produto
        String productUrl = crawlProductUrl(e);

        // InternalId
        String internalId = crawlInternalId(productUrl);

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
    Element totalElement = this.currentDoc.select(".filter-details p strong").last();

    try {
      this.totalProducts = Integer.parseInt(totalElement.text());
    } catch (Exception e) {
      this.logError(e.getMessage());
    }

    this.log("Total da busca: " + this.totalProducts);
  }

  private String crawlInternalId(String url) {
    String internalId = null;
    String[] tokens = url.split("-");

    internalId = CommonMethods.getLast(tokens).split("/")[0];

    return internalId;
  }


  private String crawlProductUrl(Element e) {
    String productUrl = e.attr("href");

    if (!productUrl.startsWith(HOME_PAGE)) {
      productUrl = (HOME_PAGE + productUrl).replace("br//", "br/");
    }

    return productUrl;
  }
}
