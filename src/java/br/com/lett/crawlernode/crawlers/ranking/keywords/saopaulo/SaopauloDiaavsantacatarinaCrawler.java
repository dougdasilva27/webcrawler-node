package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

public class SaopauloDiaavsantacatarinaCrawler extends CrawlerRankingKeywords {

  public SaopauloDiaavsantacatarinaCrawler(Session session) {
    super(session);
  }

  private static final String CEP = "04635001";

  @Override
  public void processBeforeFetch() {
    Logging.printLogDebug(logger, session, "Adding cookie...");
    String url = "https://www.dia.com.br/api/checkout/pub/postal-code/BRA/" + CEP;
    this.cookies = CrawlerUtils.fetchCookiesFromAPage(url, null, "www.dia.com.br", "/", session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 12;
    this.log("Página " + this.currentPage);

    String url = "https://www.dia.com.br/busca?ft=" + this.keywordEncoded + "&PageNumber=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url, cookies);

    Elements products = this.currentDoc.select("li[layout] .shelf-product[data-product-id]");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }
      for (Element e : products) {
        String internalPid = e.attr("data-product-id");
        String productUrl = CrawlerUtils.sanitizeUrl(e, "href", "https:", "dia.com.br");

        saveDataProduct(null, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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
    Element totalElement = this.currentDoc.selectFirst(".resultado-busca-numero .value");

    if (totalElement != null) {
      String text = totalElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
      }
    }

    this.log("Total da busca: " + this.totalProducts);
  }
}
