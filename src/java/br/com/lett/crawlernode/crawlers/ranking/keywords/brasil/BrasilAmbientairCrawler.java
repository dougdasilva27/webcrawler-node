package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilAmbientairCrawler extends CrawlerRankingKeywords {

  public BrasilAmbientairCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 8;

    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String url = "https://www.ambientair.com.br/" + this.keywordWithoutAccents.replaceAll(" ", "%20") + "?PS=50&PageNumber=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".prateleira ul > li[layout] .product-name a");
    Elements productsPid = this.currentDoc.select(".prateleira ul > li[id]");

    int count = 0;

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalPid = crawlInternalPid(productsPid.get(count));
        String productUrl = crawlProductUrl(e);

        saveDataProduct(null, internalPid, productUrl);
        count++;

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
    Element total = this.currentDoc.select(".resultado-busca-numero .value").first();

    if (total != null) {
      String text = total.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);

        this.log("Total products: " + this.totalProducts);
      }
    }
  }

  private String crawlInternalPid(Element e) {
    return CommonMethods.getLast(e.attr("id").split("_"));
  }

  private String crawlProductUrl(Element e) {
    String productUrl = e.attr("href");

    if (!productUrl.contains("ambientair.com")) {
      productUrl = ("https://www.ambientair.com.br/" + e.attr("href")).replace(".br//", ".br/");
    }

    return productUrl;
  }
}
