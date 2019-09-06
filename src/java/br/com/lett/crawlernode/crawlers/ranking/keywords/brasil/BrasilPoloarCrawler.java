package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilPoloarCrawler extends CrawlerRankingKeywords {

  public BrasilPoloarCrawler(Session session) {
    super(session);
    super.fetchMode = FetchMode.FETCHER;
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 12;
    this.log("Página " + this.currentPage);

    String url = "https://www.poloar.com.br/" + this.keywordWithoutAccents.replace(" ", "%20")
        + "?PageNumber=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select("div.prateleira ul > li[layout]");
    Elements productsId = this.currentDoc.select("div.prateleira ul > li.helperComplement");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      int index = 0;

      for (Element e : products) {
        String internalPid = crawlInternalPid(productsId, index);
        String internalId = null;
        String productUrl = CrawlerUtils.scrapUrl(e, ".data > a", "href", "https", "www.poloar.com.br");

        saveDataProduct(internalId, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit) {
          break;
        }

        index++;
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }

  @Override
  protected void setTotalProducts() {
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, "span.resultado-busca-numero > span.value", true, 0);
    this.log("Total da busca: " + this.totalProducts);
  }

  private String crawlInternalPid(Elements products, int index) {
    if (products.size() >= index) {
      String[] tokens = products.get(index).attr("id").split("_");
      return tokens[tokens.length - 1];
    }

    return null;
  }
}
