package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilLojadomecanicoCrawler extends CrawlerRankingKeywords {
  public BrasilLojadomecanicoCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 96;

    this.log("Página " + this.currentPage);

    String key = this.keywordWithoutAccents.replaceAll(" ", "%20");
    String url = "https://busca.lojadomecanico.com.br/busca?q=" + key + "&results_per_page=96&page=" + this.currentPage;

    this.log("Url: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".neemu-products-container li.nm-product-item");

    if (products.size() >= 1) {

      for (Element e : products) {
        String internalId = e.id().replace("nm-product-", "").trim();
        String internalPid = null;
        String productUrl = CrawlerUtils.scrapUrl(e, "a[itemprop=\"url\"]", "href", "https:", "www.lojadomecanico.com.br");

        saveDataProduct(internalId, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);

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
  protected boolean hasNextPage() {
    return (this.currentPage * this.pageSize) >= this.totalProducts;
  }

  @Override
  protected void setTotalProducts() {
    Element e = this.currentDoc.selectFirst(".nm-search-settings .neemu-total-products-container");

    if (e != null) {
      String aux = e.text();
      aux = aux.replaceAll("[^0-9]+", "");

      if (!aux.isEmpty()) {
        this.totalProducts = Integer.parseInt(aux);
      }
    }

    this.log("Total da busca: " + this.totalProducts);
  }
}
