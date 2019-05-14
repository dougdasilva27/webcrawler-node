package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilBenoitCrawler extends CrawlerRankingKeywords {

  public BrasilBenoitCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 40;

    this.log("Página " + this.currentPage);

    String url = "https://www.benoit.com.br/pesquisa.partial?pg=" + this.currentPage + "&o=mais-relevantes&t=" + this.keywordEncoded;

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select("#wd6 ul li");

    if (!products.isEmpty()) {

      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {

        String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div[data-pid]", "data-pid");
        String productUrl = CrawlerUtils.scrapUrl(e, "h3[class=\"name\"] a", "href", "https:", "www.benoit.com.br");

        saveDataProduct(null, internalPid, productUrl);


        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".product-count span", false, 0);
    this.log("Total products: " + this.totalProducts);
  }
}
