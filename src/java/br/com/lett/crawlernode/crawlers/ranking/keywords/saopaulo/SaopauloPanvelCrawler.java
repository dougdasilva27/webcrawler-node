package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class SaopauloPanvelCrawler extends CrawlerRankingKeywords {

  public SaopauloPanvelCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 15;

    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String url = "https://www.panvel.com/panvel/buscarProduto.do?paginaAtual=" + this.currentPage + "&termoPesquisa="
        + this.keywordWithoutAccents.replace(" ", "+");
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = crawlProductsInfo(url);
    Elements products = this.currentDoc.select(".box-produto > a");

    if (!products.isEmpty()) {
      for (Element e : products) {
        String urlProduct = crawlProductUrl(e);
        String internalId = crawlInternalId(urlProduct);

        saveDataProduct(internalId, null, urlProduct);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + urlProduct);
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
  protected boolean hasNextPage() {
    Element item = this.currentDoc.select(".pagination__item a").last();
    return item != null && !item.hasClass("pagination__arrow--disabled");
  }

  private String crawlInternalId(String url) {
    return CommonMethods.getLast(url.split("-"));
  }

  private String crawlProductUrl(Element e) {
    String productUrl = e.attr("href");

    if (!productUrl.startsWith("https://www.panvel.com")) {
      productUrl = "https://www.panvel.com" + productUrl;
    }

    return productUrl;
  }

  private Document crawlProductsInfo(String url) {
    return Jsoup.parse(fetchPostFetcher(url, null, null, null));
  }
}
