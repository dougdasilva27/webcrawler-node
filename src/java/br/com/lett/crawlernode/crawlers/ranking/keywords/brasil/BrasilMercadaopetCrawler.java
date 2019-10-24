package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilMercadaopetCrawler extends CrawlerRankingKeywords {

  public BrasilMercadaopetCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 10;
    this.log("Página " + this.currentPage);

    String url = "https://mercadaopet.com.br/page/" + this.currentPage + "/?s=" + this.keywordEncoded + "&post_type=product";

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".shop-products-inner .product-info");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".add_to_wishlist[data-product-id]", "data-product-id");
        String productUrl = CrawlerUtils.scrapUrl(e, ".product-name a[href]", "href", "https", "mercadaopet.com.br");

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

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
        + this.arrayProducts.size() + " produtos crawleados");
  }

  @Override
  protected void setTotalProducts() {
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".woocommerce-result-count", "de", null, true, true, 0);
    this.log("Total da busca: " + this.totalProducts);

  }
}
