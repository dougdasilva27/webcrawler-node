package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;

public class BrasilNutricaototalCrawler extends CrawlerRankingKeywords {
  public BrasilNutricaototalCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 12;

    this.log("Página " + this.currentPage);
    String key = this.keywordWithoutAccents.replaceAll(" ", "%20");
    String url = "https://www.nutricaototal.com.br/catalogsearch/result/index/?p="
        + this.currentPage + "&q=" + key;

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".product-block");

    if (products.size() >= 1) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalId = crawlInternalId(e, ".regular-price[id^=product-price]");
        String internalPid = null; // never have
        String productUrl = crawlProductUrl(e, ".product-block a[href].product-image");

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
    Element totalElement =
        this.currentDoc.selectFirst(".category-products .toolbar .pager .pager_left .amount");

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

  private String crawlInternalId(Element e, String selector) {
    Element aux = e.selectFirst(selector);
    String internalId = null;

    if (aux != null) {
      String attr = aux.id();

      if (!attr.isEmpty()) {
        internalId = MathUtils.parseInt(attr).toString();
      }
    }

    return internalId;
  }

  private String crawlProductUrl(Element e, String selector) {
    Element aux = e.selectFirst(selector);
    String url = null;

    if (aux != null) {
      url = CrawlerUtils.sanitizeUrl(aux, "href", "https", "www.nutricaototal.com.br");
    }

    return url;
  }
}
