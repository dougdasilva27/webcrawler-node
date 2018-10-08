package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class ArgentinaRibeiroCrawler extends CrawlerRankingKeywords {

  public ArgentinaRibeiroCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 16;

    this.log("Página " + this.currentPage);
    String url = "https://www.ribeiro.com.ar/browse/?_dyncharset=UTF-8&Nty=1&Ntt=" + this.keywordEncoded + "&No=" + this.arrayProducts.size();
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select("#atg_store_product > li > a");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalId = crawlInternalId(e);
        String productUrl = crawlProductUrl(e);

        saveDataProduct(internalId, null, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
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
    Element totalElement = this.currentDoc.select("#resultsCount").first();

    if (totalElement != null) {
      try {
        this.totalProducts = Integer.parseInt(totalElement.text().replaceAll("[^0-9]", "").trim());
      } catch (Exception e) {
        this.logError(CommonMethods.getStackTrace(e));
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String crawlInternalId(Element e) {
    String internalId = null;

    Element product = e.selectFirst("input[onclick^=agregar]");
    if (product != null) {
      internalId = product.attr("onclick").replaceAll("[^0-9]", "");
    }

    return internalId;
  }

  private String crawlProductUrl(Element e) {
    return CrawlerUtils.sanitizeUrl(e, "href", "https:", "www.ribeiro.com.ar").split(";")[0];
  }
}
