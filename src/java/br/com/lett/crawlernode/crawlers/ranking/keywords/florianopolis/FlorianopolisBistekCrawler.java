package br.com.lett.crawlernode.crawlers.ranking.keywords.florianopolis;

import java.util.HashMap;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class FlorianopolisBistekCrawler extends CrawlerRankingKeywords {

  public FlorianopolisBistekCrawler(Session session) {
    super(session);
  }

  private static final String HOST = "www.bistekonline.com.br";

  @Override
  protected void processBeforeFetch() {
    this.cookies = CrawlerUtils.fetchCookiesFromAPage("https://www.bistekonline.com.br/store/SetStoreByZipCode?zipCode=88066-000", null, HOST, "/",
        cookies, session, new HashMap<>(), dataFetcher);
  }

  @Override
  public void extractProductsFromCurrentPage() {
    this.pageSize = 24;
    this.log("Página " + this.currentPage);

    String keyword = this.keywordWithoutAccents.replace(" ", "%20");
    String url =
        "https://www.bistekonline.com.br/busca/3/0/0/MaisVendidos/Decrescente/24/" + this.currentPage + "/0/0/" + keyword + ".aspx?q=" + keyword;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url, cookies);
    Elements products = this.currentDoc.select("#listProduct > li");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }
      for (Element e : products) {
        String internalPid = crawlInternalPid(e);
        String urlProduct = crawlProductUrl(e);

        saveDataProduct(null, internalPid, urlProduct);

        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
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
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, ".filter-details p strong:last-child", null, true, 0);
    this.log("Total da busca: " + this.totalProducts);
  }

  private String crawlInternalPid(Element e) {
    String internalPid = null;
    Element pid = e.selectFirst("input");

    if (pid != null) {
      internalPid = pid.attr("value");
    }

    return internalPid;
  }

  private String crawlProductUrl(Element e) {
    String urlProduct = null;
    Element eUrl = e.select(" a.link.url[title]").first();

    if (eUrl != null) {
      urlProduct = CrawlerUtils.completeUrl(eUrl.attr("href"), "https", HOST);
    }

    return urlProduct;
  }
}
