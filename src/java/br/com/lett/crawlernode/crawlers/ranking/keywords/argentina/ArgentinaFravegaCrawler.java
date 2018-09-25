package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import java.util.ArrayList;
import java.util.List;
import org.apache.http.cookie.Cookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class ArgentinaFravegaCrawler extends CrawlerRankingKeywords {

  public ArgentinaFravegaCrawler(Session session) {
    super(session);
  }

  private List<Cookie> cookies = new ArrayList<>();

  @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);

    this.pageSize = 24;

    String keyword = this.keywordWithoutAccents.replaceAll(" ", "%20");
    String url = "https://www.fravega.com/" + keyword + "?PageNumber=" + this.currentPage;

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url, cookies);

    Elements products = this.currentDoc.select("li[layout] .wrapData");
    Elements productsPid = this.currentDoc.select("li[id]");
    int count = 0;

    if (!products.isEmpty()) {
      if (this.totalProducts == 0)
        setTotalProducts();

      for (Element e : products) {
        String internalPid = crawlInternalPid(productsPid.get(count));

        String productUrl = crawlProductUrl(e);

        saveDataProduct(null, internalPid, productUrl);
        count++;

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
    Element totalElement = this.currentDoc.selectFirst(".resultado-busca-numero > span.value");

    if (totalElement != null) {
      String text = totalElement.text().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String crawlInternalPid(Element e) {
    String internalPid = null;

    if (e != null) {
      String id = e.attr("id");

      if (id.contains("_")) {
        internalPid = CommonMethods.getLast(id.split("_"));
      }
    }
    return internalPid;
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;
    Element urlElement = e.selectFirst("a");

    if (urlElement != null) {
      productUrl = urlElement.attr("href");

      if (!productUrl.contains("fravega.com")) {
        productUrl = ("https://www.fravega.com/" + urlElement.attr("href")).replace(".com//", ".com/");
      }
    }

    return productUrl;
  }
}
