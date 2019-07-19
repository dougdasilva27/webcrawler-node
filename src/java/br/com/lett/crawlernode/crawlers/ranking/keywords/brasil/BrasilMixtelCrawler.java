package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.Arrays;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

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

      for (Element product : products) {

        String internalId = crawlInternalId(product);
        String productUrl = CrawlerUtils.scrapUrl(product, "a", Arrays.asList("href"), "https", "mixteldistribuidora.com.br");

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

  private String crawlInternalId(Element product) {
    String internalId = null;

    if (product.hasAttr("id")) {
      internalId = CommonMethods.getLast(product.attr("id").split("-"));
    }

    return internalId;
  }

}
