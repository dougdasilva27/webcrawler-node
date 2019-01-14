package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class ColombiaExitoCrawler extends CrawlerRankingKeywords {
  private List<Cookie> cookies = new ArrayList<>();

  public ColombiaExitoCrawler(Session session) {
    super(session);
  }

  @Override
  protected void processBeforeFetch() {
    super.processBeforeFetch();

    Map<String, String> cookiesMap =
        DataFetcher.fetchCookies(session, "https://www.exito.com/", cookies, 1);

    for (Entry<String, String> entry : cookiesMap.entrySet()) {
      BasicClientCookie cookie = new BasicClientCookie(entry.getKey(), entry.getValue());
      cookie.setDomain(".exito.com");
      cookie.setPath("/");
      this.cookies.add(cookie);
    }
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // number of products per page
    this.pageSize = 80;

    this.log("Página " + this.currentPage);

    String keyword = this.keywordWithoutAccents.replace(" ", "%20");

    // builds the url with the keyword and page number
    String url = "https://www.exito.com/browse?Ntt=" + keyword + "&No="
        + (this.currentPage - 1) * 80 + "&Nrpp=80";

    this.log("Link onde são feitos os crawlers: " + url);

    // fetch the html
    this.currentDoc = fetchDocumentWithWebDriver(url);

    Elements products = this.currentDoc.select(".product-list div.product");

    if (products.size() >= 1) {
      if (totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalPid = e.attr("data-prdid");
        String internalId = null;
        String productUrl = scrapProductUrl(e);

        saveDataProduct(internalId, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: "
            + internalPid + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit) {
          break;
        }

      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
        + this.arrayProducts.size() + " produtos crawleados");
  }

  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.selectFirst(".plp-pagination-result p strong");

    if (totalElement != null) {
      Pattern p = Pattern.compile("([0-9]+)");

      // Reverses the string
      Matcher m = p.matcher(new StringBuilder(totalElement.text().trim()).reverse());

      if (m.find()) {
        try {
          // Get the first match on the reversed string and revert it to original
          this.totalProducts = Integer.parseInt(new StringBuilder(m.group(1)).reverse().toString());

        } catch (Exception e) {
          this.logError(CommonMethods.getStackTraceString(e));
        }
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }

  @Override
  protected boolean hasNextPage() {
    Elements pages = this.currentDoc.select(".desktop ul li:not(:first-child)");

    if (pages.size() > 2) {
      if (pages.get(pages.size() - 1).hasClass("disabled")
          && pages.get(pages.size() - 2).hasClass("active")) {
        return false;
      }
    }

    return true;
  }

  private String scrapProductUrl(Element e) {
    Element element = e.selectFirst(".row a");
    String productUrl = null;

    if (element != null) {
      productUrl = CrawlerUtils.sanitizeUrl(element, "href", "https:", "www.exito.com");
    }

    return productUrl;
  }
}
