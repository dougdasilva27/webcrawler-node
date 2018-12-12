package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class ChileKitchencenterCrawler extends CrawlerRankingKeywords {

  public ChileKitchencenterCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 12;
    this.log("Página " + this.currentPage);

    String url = "https://www.kitchencenter.cl/products?keywords=" + this.keywordWithoutAccents.replace(" ", "%20") + "&page=" + this.currentPage;

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select("#products div[id]");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalPid = CommonMethods.getLast(e.attr("id").split("_"));
        String productUrl = crawlProductUrl(e);

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
    int total = 0;
    Elements stars = this.currentDoc.select("#filter-stars .list-group-item > span");
    for (Element e : stars) {
      String text = e.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        total += Integer.parseInt(text);
      }
    }

    if (total > 0) {
      this.totalProducts = total;
      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String crawlProductUrl(Element e) {
    String url = null;

    Element eUrl = e.selectFirst("a[itemprop=url]");
    if (eUrl != null) {
      url = CrawlerUtils.sanitizeUrl(eUrl, "href", "https:", "www.kitchencenter.cl");
    }

    return url;
  }
}
