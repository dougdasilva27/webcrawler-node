package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class ChileParisCrawler extends CrawlerRankingKeywords {

  public ChileParisCrawler(Session session) {
    super(session);
  }

  private static final String HOST = br.com.lett.crawlernode.crawlers.corecontent.chile.ChileParisCrawler.HOST;

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 40;
    this.log("Página " + this.currentPage);

    String url = "https://" + HOST + "/search?q=" + this.keywordEncoded + "&sz=40&start=" + ((this.currentPage - 1) * this.pageSize);

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select("#search-result-items .product-tile");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalPid = e.attr("data-itemid");

        // this case we need to capture all variations
        Elements colors = e.select(".carousel-colors a[href]");
        if (!colors.isEmpty()) {
          this.position++;
          for (Element c : colors) {
            String productUrl = CrawlerUtils.sanitizeUrl(c, "href", "https", HOST);
            saveDataProduct(null, internalPid, productUrl, this.position);

            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit) {
              break;
            }
          }
        } else {
          String productUrl = crawlProductUrl(e);

          saveDataProduct(null, internalPid, productUrl);

          this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
          if (this.arrayProducts.size() == productsLimit) {
            break;
          }
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
    this.totalProducts = CrawlerUtils.scrapTotalProductsForRanking(this.currentDoc, ".total-products > span");
    this.log("Total da busca: " + this.totalProducts);
  }

  private String crawlProductUrl(Element e) {
    String url = null;

    Element eUrl = e.selectFirst(".box-desc-product a");
    if (eUrl != null) {
      url = CrawlerUtils.sanitizeUrl(eUrl, "href", "https:", HOST);
    }

    return url;
  }
}
