package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilJocarCrawler extends CrawlerRankingKeywords {

  public BrasilJocarCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 28;

    this.log("Página " + this.currentPage);

    String keyword = this.keywordWithoutAccents.replace(" ", "%20");
    String url = "https://www.jocar.com.br/Index.aspx?CSGI=1004%7C1121%7C1366%7C1367%7C1506%7C669%7C987%7C998&NSN=" + keyword + "&BPM=" + keyword
        + "&PG=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    // chama função de pegar o html
    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".productDiv .productName a");

    // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
    if (!products.isEmpty()) {
      for (Element e : products) {
        String productUrl = e.attr("href");
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
  protected boolean hasNextPage() {
    Element e = this.currentDoc.select(".paginacao_frm a").last();

    return e != null && !e.hasClass("paginacao_princ_sel");
  }

  private String crawlInternalId(String url) {
    String internalId = null;

    if (url.contains("?")) {
      String[] tokens = CommonMethods.getLast(url.toLowerCase().split("\\?")).split("&");
      for (String token : tokens) {
        if (token.startsWith("cp=")) {
          internalId = token.replace("cp=", "");
          break;
        }
      }
    }

    return internalId;
  }
}
