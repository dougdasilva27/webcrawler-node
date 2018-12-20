package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilServnutriCrawler extends CrawlerRankingKeywords {
  public BrasilServnutriCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 16;

    this.log("Página " + this.currentPage);

    String key = this.keywordWithoutAccents.replaceAll(" ", "%20");
    String url = "http://www.servnutri.com.br/page/" + this.currentPage + "/?s=" + key
        + "&post_type=product";

    this.log("Url: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".productinfo");

    if (products.size() >= 1) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        Element pid = e.select(".productinfo a[data-product_id]").first();
        String internalPid = pid.attr("data-product_id");
        String internalId = null;

        // monta a url
        Element eUrl = e.select(".productinfo a[rel]:not([data-product_id])").first();
        String productUrl = eUrl.attr("href");

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
  protected boolean hasNextPage() {
    if (this.arrayProducts.size() < this.totalProducts) {
      return true;
    }

    return false;
  }

  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.selectFirst(".woocommerce-result-count");

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
}
