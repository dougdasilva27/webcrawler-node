package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.Arrays;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilCasaeconstrucaoCrawler extends CrawlerRankingKeywords {

  public BrasilCasaeconstrucaoCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 24;

    this.log("Página " + this.currentPage);

    // se a key contiver o +, substitui por %20, pois nesse market a pesquisa na url é assim
    String url =
        "https://www.cec.com.br/busca?q=" + this.keywordWithoutAccents.replace(" ", "%20") + "&page=" + this.currentPage + "&resultsperpage=64";
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select("div.products div[id] div.product");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalId = crawlInternalId(e);
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
    Element totalElement = this.currentDoc.selectFirst("#lblCountProductsNeemu");

    if (totalElement != null) {
      String text = totalElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
      }
    }

    this.log("Total da busca: " + this.totalProducts);
  }

  private String crawlInternalId(Element e) {
    return e.attr("data-product-id");
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;
    Element urlElement = e.selectFirst(".name-and-brand");

    if (urlElement != null) {
      productUrl = CrawlerUtils.sanitizeUrl(urlElement, Arrays.asList("href"), "https:", "www.cec.com.br");
    }

    return productUrl;
  }
}
