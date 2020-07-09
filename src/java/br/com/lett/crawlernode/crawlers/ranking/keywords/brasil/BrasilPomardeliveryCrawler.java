package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilPomardeliveryCrawler extends CrawlerRankingKeywords {

  public BrasilPomardeliveryCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 21;

    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String url = "https://www.pomardelivery.com.br/Busca.aspx?busca=" + this.keywordWithoutAccents + "&pagina=" + this.currentPage;

    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".lista_produtos.sitewide li");

    if (!products.isEmpty()) {
      for (Element e : products) {
        if (this.totalProducts == 0) {
          setTotalProducts();
        }

        String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".btn_comprar", "data-id");
        String productUrl = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a", "href"), "http://", "www.pomardelivery.com.br");

        saveDataProduct(internalId, null, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit) {
          break;
        }
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado para a página atual!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }

  @Override
  protected void setTotalProducts() {
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, "#formBusca .celula span", false, 0);
    this.log("Total da busca: " + this.totalProducts);
  }
}