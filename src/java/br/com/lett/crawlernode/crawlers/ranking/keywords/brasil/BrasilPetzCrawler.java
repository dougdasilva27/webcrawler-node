package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.FetchUtilities;
import br.com.lett.crawlernode.core.fetcher.models.LettProxy;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilPetzCrawler extends CrawlerRankingKeywords {

  public BrasilPetzCrawler(Session session) {
    super(session);
    super.fetchMode = FetchMode.APACHE;
  }

  private static final String HOME_PAGE = "https://www.petz.com.br/";
  private String userAgent;
  private LettProxy proxyUsed;
  private List<Cookie> cookies = new ArrayList<>();

  @Override
  public void processBeforeFetch() {
    this.userAgent = FetchUtilities.randUserAgent();

    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.USER_AGENT, this.userAgent);

    Request request = RequestBuilder.create().setUrl(HOME_PAGE).setCookies(cookies).setHeaders(headers).build();
    Response response = this.dataFetcher.get(session, request);

    this.proxyUsed = response.getProxyUsed();

    for (Cookie cookieResponse : response.getCookies()) {
      BasicClientCookie cookie = new BasicClientCookie(cookieResponse.getName(), cookieResponse.getValue());
      cookie.setDomain("www.petz.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);
    }
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 18;

    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String url = "https://www.petz.com.br/busca_Loja.html?q=" + this.keywordEncoded + "&page=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.USER_AGENT, this.userAgent);

    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).setProxy(proxyUsed).build();
    this.currentDoc = Jsoup.parse(this.dataFetcher.get(session, request).getBody());

    Elements products = this.currentDoc.select(".liProduct > div");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalPid = crawlInternalPid(e);
        String productUrl = crawlProductUrl(e);

        saveDataProduct(null, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.select("#paginas a").first();

    if (totalElement != null) {
      String[] parameters = totalElement.attr("href").split("&");

      for (String parameter : parameters) {
        if (parameter.startsWith("total=")) {
          String total = parameter.split("=")[1].replaceAll("[^0-9]", "").trim();

          if (!total.isEmpty()) {
            this.totalProducts = Integer.parseInt(total);
          }

          break;
        }
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String crawlInternalPid(Element e) {
    String internalPid = null;

    Element sku = e.select("meta[itemprop=sku]").first();
    if (sku != null) {
      internalPid = sku.attr("content");
    }

    return internalPid;
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;

    Element url = e.select("> a").last();

    if (url != null) {
      productUrl = url.attr("href");

      if (!productUrl.contains("petz.com")) {
        productUrl = (HOME_PAGE + productUrl).replace("br//", "br/");
      }
    }

    return productUrl;
  }
}
