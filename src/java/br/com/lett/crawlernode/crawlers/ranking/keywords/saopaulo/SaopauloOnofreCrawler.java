package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class SaopauloOnofreCrawler extends CrawlerRankingKeywords {

  public SaopauloOnofreCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 12;
    this.log("Página " + this.currentPage);

    String url;
    if (this.currentPage == 1) {
      url = "https://busca.onofre.com.br/search?w=" + this.keywordEncoded;
    } else {
      url = CrawlerUtils.scrapUrl(this.currentDoc, ".pages.inline li.sli_next_wrap > a", "href", "https", "www.onofre.com.br");
    }

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".products-grid .item[data-sku]");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".pager .amount", true, 0);
        this.log("Total: " + this.totalProducts);
      }

      for (Element e : products) {
        String internalPid = e.attr("data-sku");
        String urlProduct = CrawlerUtils.scrapUrl(e, ".product-name a", "title", "https", "www.onofre.com.br");

        saveDataProduct(null, internalPid, urlProduct);

        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
        if (this.arrayProducts.size() == productsLimit)
          break;
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
    if (!(hasNextPage()))
      setTotalProducts();

  }
}
