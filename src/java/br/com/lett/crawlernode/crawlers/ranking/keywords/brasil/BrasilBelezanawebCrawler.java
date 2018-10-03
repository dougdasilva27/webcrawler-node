package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.Arrays;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilBelezanawebCrawler extends CrawlerRankingKeywords {

  public BrasilBelezanawebCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 36;

    this.log("Página " + this.currentPage);

    String url = "https://www.belezanaweb.com.br/busca?q=" + this.keywordEncoded + "&pagina=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".showcase > .showcase-item");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }
      for (Element e : products) {
        String internalId = crawlInternalPid(e);
        String productUrl = crawlProductUrl(e);

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
    Element totalElement = this.currentDoc.selectFirst(".pagination-total strong");

    if (totalElement != null) {
      String total = totalElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!total.isEmpty()) {
        this.totalProducts = Integer.parseInt(total);
      }
    }

    this.log("Total da busca: " + this.totalProducts);
  }

  private String crawlInternalPid(Element e) {
    String internalId = null;

    JSONObject json = CrawlerUtils.stringToJson(e.attr("data-event"));
    if (json.has("sku")) {
      internalId = json.get("sku").toString();
    }

    return internalId;
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;

    Element url = e.selectFirst(".showcase-item-buy [data-href]");
    if (url != null) {
      productUrl = CrawlerUtils.sanitizeUrl(url, Arrays.asList("data-href"), "https:", "www.belezanaweb.com.br");
    }
    return productUrl;
  }
}
