package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilDicasaCrawler extends CrawlerRankingKeywords {

  public BrasilDicasaCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 24;

    this.log("Página " + this.currentPage);

    String url = "https://www.dicasashop.com.br/pesquisa/?p=" + this.keywordEncoded + "&pagina=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".products li");

    if (!products.isEmpty()) {

      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element element : products) {
        String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(element, "input[name=\"ProdutoId\"]", "value");
        String urlProduct = crawlProductUrl(element);

        saveDataProduct(null, internalPid, urlProduct);

        if (this.arrayProducts.size() == productsLimit) break;
      }
    } else {
      setTotalProducts();
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }

  private String crawlProductUrl(Element e) {
    Element urlElement = e.selectFirst(".product .photo");
    String url = null;

    if (urlElement != null) {
      url = urlElement.attr("href");
    }

    return url;
  }

  @Override
  protected void setTotalProducts() {
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, "div.results > span:nth-child(3)", false, 0);
    this.log("Total da busca: " + this.totalProducts);
  }

}
