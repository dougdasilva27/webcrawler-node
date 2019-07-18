package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilMixtelCrawler extends CrawlerRankingKeywords {

  public BrasilMixtelCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 12;

    this.log("Página " + this.currentPage);

    String url = "http://mixteldistribuidora.com.br/?s=" + this.keywordEncoded;

    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".product-type-simple");

    if (products.size() > 0) {

      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element product : products) {

        String internalId = crawlInternalPid(product);
        String productUrl = crawlProductUrl(product);

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


    this.log("Link onde são feitos os crawlers: " + url);
  }

  private String crawlProductUrl(Element product) {
    Element productAncor = product.selectFirst("a");
    String url = null;

    if (productAncor != null) {
      url = productAncor.attr("href");
    }

    return url;
  }

  private String crawlInternalPid(Element product) {
    String internalPid = null;

    if (product.hasAttr("id")) {
      internalPid = product.attr("id");
      internalPid = internalPid.replaceAll("[^0-9]", "");
    }

    return internalPid;
  }

}
