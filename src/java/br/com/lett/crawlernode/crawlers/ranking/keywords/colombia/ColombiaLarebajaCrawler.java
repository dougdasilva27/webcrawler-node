package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class ColombiaLarebajaCrawler extends CrawlerRankingKeywords {

  public ColombiaLarebajaCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 24;

    this.log("Página " + this.currentPage);

    String url = "https://www.larebajavirtual.com/catalogo/buscar?busqueda=" + this.keywordEncoded;

    if (this.currentPage > 1) {
      url = CrawlerUtils.scrapUrl(currentDoc, ".pager .next a", "href", "https:", "www.larebajavirtual.com");
    }

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".listaProductos li");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "[data-producto]", "data-producto");
        String productUrl = CrawlerUtils.scrapUrl(e, ".content_product a", "href", "https:", "larebajavirtual.com");
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
    Element total = this.currentDoc.selectFirst("#id-productos-list .summary");
    if (total != null) {
      String text = total.ownText().toLowerCase();

      if (text.contains("de")) {
        String totalText = CommonMethods.getLast(text.split("de")).replaceAll("[^0-9]", "");

        if (!totalText.isEmpty()) {
          this.totalProducts = Integer.parseInt(totalText);
          this.log("Total da busca: " + this.totalProducts);
        }
      }
    }
  }
}
