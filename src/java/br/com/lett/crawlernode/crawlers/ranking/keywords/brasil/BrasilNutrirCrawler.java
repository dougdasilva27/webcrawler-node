package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilNutrirCrawler extends CrawlerRankingKeywords {
  public BrasilNutrirCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 20;

    this.log("Página " + this.currentPage);
    String key = this.keywordWithoutAccents.replaceAll(" ", "%20");
    String url = "https://www.nutrir-sc.com.br/_loja_/_busca_/" + key + "?pg=" + this.currentPage;

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".produto.produto_small");

    if (products.size() >= 1) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalId = scrapInternalId(e);
        String internalPid = null; // never have
        String productUrl = scrapUrl(e);

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
    Element totalElement = this.currentDoc.selectFirst(".loja_ativo .container > h2");

    if (totalElement != null) {
      Matcher m = Pattern.compile("([0-9]+)").matcher(totalElement.text());

      if (m.find()) {
        try {
          this.totalProducts = Integer.parseInt(m.group());

        } catch (Exception e) {
          this.logError(CommonMethods.getStackTraceString(e));
        }
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String scrapInternalId(Element e) {
    String internalId = null;
    String aux = e.id();

    if (!aux.isEmpty()) {
      internalId = aux.substring(1);
    }

    return internalId;
  }

  private String scrapUrl(Element e) {
    String url = null;
    Element sub_e = e.selectFirst("a.img");

    if (sub_e != null) {
      url = CrawlerUtils.sanitizeUrl(sub_e, "href", "https://", "www.nutrir-sc.com.br");
    }

    return url;
  }
}
