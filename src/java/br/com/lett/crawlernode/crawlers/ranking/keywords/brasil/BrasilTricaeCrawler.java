package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilTricaeCrawler extends CrawlerRankingKeywords {

  public BrasilTricaeCrawler(Session session) {
    super(session);
  }

  private static final String HOST = "www.tricae.com.br";

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 48;
    this.log("Página " + this.currentPage);

    String url = "https://" + HOST + "/all-products/?q=" + this.keywordEncoded + "&wtqs=1&page=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".product-box > [id]");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalPid = e.id();
        String productUrl = CrawlerUtils.scrapUrl(e, "a.product-box-link", "href", "https", HOST);

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
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".items-products .value", true, 0);
    this.log("Total: " + this.totalProducts);
  }
}
