package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class ArgentinaLaanonimaonlineCrawler extends CrawlerRankingKeywords {

  public ArgentinaLaanonimaonlineCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 20;

    this.log("Página " + this.currentPage);

    String url = "http://www.laanonimaonline.com/buscar?pag=" + this.currentPage + "&clave=" + this.keywordWithoutAccents.replace(" ", "%20");
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".producto.item a.mostrar_listado");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String productUrl = crawlProductUrl(e);
        String internalId = crawlInternalId(productUrl);

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

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }

  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.select(".spa_top .pag_cont2").first();

    if (totalElement != null) {
      String html = totalElement.outerHtml().replace("<!--", "").replace("-->", "");
      Element e = Jsoup.parse(html).select("strong").last();

      if (e != null) {
        String text = e.ownText().replaceAll("[^0-9]", "").trim();

        if (!text.isEmpty()) {
          this.totalProducts = Integer.parseInt(text);
        }
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String crawlInternalId(String url) {
    String internalId = null;

    if (url.contains("art_")) {
      internalId = CommonMethods.getLast(url.split("art_"));

      if (internalId.contains("?")) {
        internalId = internalId.split("\\?")[0];
      }

      if (internalId.contains("/")) {
        internalId = internalId.split("\\/")[0];
      }
    }

    return internalId;
  }

  private String crawlProductUrl(Element e) {
    String productUrl;

    productUrl = e.attr("href");

    if (!productUrl.contains("laanonimaonline")) {
      productUrl = ("https://www.laanonimaonline.com/" + e.attr("href")).replace(".com//", ".com/");
    }


    return productUrl;
  }
}
