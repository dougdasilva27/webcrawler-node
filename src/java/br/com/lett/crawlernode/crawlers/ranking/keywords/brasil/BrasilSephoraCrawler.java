package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilSephoraCrawler extends CrawlerRankingKeywords {

  public BrasilSephoraCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);

    this.pageSize = 40;

    String url = "https://www.sephora.com.br/search/?q=" + this.keywordEncoded.toLowerCase() + "&page=" + this.currentPage;

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url, cookies);

    Elements products = this.currentDoc.select("ul.search-result-items>li");
    boolean emptySearch = this.currentDoc.selectFirst(".results-hits") == null;

    if (!products.isEmpty() && !emptySearch) {
      if (this.totalProducts == 0)
         this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, "div.results-hits", false, 0);

      for (Element e : products) {
        String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div.product-tile", "data-variant-id");
        String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div.product-image>a", "href");

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

}
