package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilDrogarianovaesperancaCrawler extends CrawlerRankingKeywords {

  public BrasilDrogarianovaesperancaCrawler(Session session) {
    super(session);
    super.fetchMode = FetchMode.FETCHER;
  }

  private static final int PAGE_SIZE = 12;

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = PAGE_SIZE;
    this.log("Página " + this.currentPage);

    String url = "https://www.drogarianovaesperanca.com.br/busca/?Pagina=" + this.currentPage + "&q=" +
        CommonMethods.encondeStringURLToISO8859(this.location, logger, session) + "&ppg=36";
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".produto-lista");

    // In this scraper we need scrap products total first
    // because we need this information for verify if the site returns products for the keyword we send
    // or returns suggestions (in this case we don't have total Products),
    // we don't scrap suggestions products
    if (this.totalProducts == 0) {
      this.setTotalProducts();
    }

    if (!products.isEmpty() && this.totalProducts > 0) {
      for (Element e : products) {
        String productUrl = CrawlerUtils.scrapUrl(e, ".produto-descricao-lista a", "href", "https", "www.drogarianovaesperanca.com.br");
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
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, ".resultados-busca span", true, 0);
    this.log("Total: " + this.totalProducts);
  }

  /**
   * For scrap internal Id we need the url, is the only "safe" place
   * 
   * Ex: Internal Id will be found in the end of url
   * 
   * Url:
   * https://www.drogarianovaesperanca.com.br/naturais/fitoterapicos/comprar-prevelip-com-60-capsulas-16970/
   * 
   * InternalId: 16970
   * 
   * @param url
   * @return
   */
  private String crawlInternalId(String url) {
    String internalId = null;

    String lastStringUrl = CommonMethods.getLast(url.split("\\?")[0].split("-"));

    if (lastStringUrl.contains("/")) {
      internalId = lastStringUrl.split("/")[0];
    }

    return internalId;
  }
}
