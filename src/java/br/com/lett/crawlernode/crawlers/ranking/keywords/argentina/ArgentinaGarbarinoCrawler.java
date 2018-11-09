package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import java.util.ArrayList;
import java.util.List;
import org.apache.http.cookie.Cookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class ArgentinaGarbarinoCrawler extends CrawlerRankingKeywords {

  public ArgentinaGarbarinoCrawler(Session session) {
    super(session);
  }

  private List<Cookie> cookies = new ArrayList<>();

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 48;

    this.log("Página " + this.currentPage);

    String url = "https://www.garbarino.com/q/" + this.keywordWithoutAccents.replace(" ", "%20") + "/srch?page=" + this.currentPage + "&q="
        + this.keywordWithoutAccents.replace(" ", "%20");
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url, cookies);
    Elements products = this.currentDoc.select(".itemBox");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String productUrl = crawlProductUrl(e);
        String internalId = crawlInternalId(e);

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
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.selectFirst(".breadcrumb-item--active > span");

    if (totalElement != null) {
      String html = totalElement.text().replaceAll("[^0-9]", "");

      if (!html.isEmpty()) {
        this.totalProducts = Integer.parseInt(html);
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;

    Element urlElement = e.selectFirst(".itemBox--info a");

    if (urlElement != null) {
      productUrl = CrawlerUtils.sanitizeUrl(urlElement, "href", "https:", "www.garbarino.com");
    }

    return productUrl;
  }

  private String crawlInternalId(Element e) {
    String internalId = null;

    Element idElement = e.selectFirst(".button--action__fav");

    if (idElement != null) {
      internalId = idElement.attr("data-id");
    }

    return internalId;
  }

}
