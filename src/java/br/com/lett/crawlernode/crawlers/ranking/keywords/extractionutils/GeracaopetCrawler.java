package br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class GeracaopetCrawler extends CrawlerRankingKeywords {
  protected String cep;

  public GeracaopetCrawler(Session session, String cep) {
    super(session);
    this.cep = cep;
  }

  @Override
  public void processBeforeFetch() {
    BasicClientCookie cookie = new BasicClientCookie("zipcode", cep);
    cookie.setDomain(".www.geracaopet.com.br");
    cookie.setPath("/");
    this.cookies.add(cookie);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);

    this.pageSize = 16;

    String url = "https://www.geracaopet.com.br/catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordEncoded;

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url, cookies);

    Elements products = this.currentDoc.select(".results .products-grid li");

    if (!products.isEmpty()) {

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
  protected boolean hasNextPage() {
    return !this.currentDoc.select(".next").isEmpty();
  }

  private String crawlProductUrl(Element e) {
    Element ancor = e.selectFirst(".product-item-name .product-item-link");
    String url = null;

    if (ancor != null) {
      url = ancor.attr("href");
    }

    return url;
  }

  private String crawlInternalPid(Element e) {
    Elements dataRole = e.select("div[data-role]");
    String internalPid = null;

    if (dataRole.hasAttr("data-product-id")) {
      internalPid = dataRole.attr("data-product-id");
    } else if (dataRole.hasAttr("data-role")) {
      internalPid = dataRole.attr("data-role");
      if (internalPid.startsWith("swatch-opt")) {
        internalPid = internalPid.replaceAll("[^0-9]", "");
      }
    }

    return internalPid;
  }

}
