package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilVilanovaCrawler extends CrawlerRankingKeywords {

  private static final int PAGE_SIZE = 24;

  public BrasilVilanovaCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = PAGE_SIZE;

    this.log("Página " + this.currentPage);
    // https://www.vilanova.com.br/Busca/Resultado/?p=2&loja=&q=Chocolate&precode=&precoate=&ordenacao=6&limit=24
    String url = "https://www.vilanova.com.br/Busca/Resultado/?p=" + this.currentPage + "&loja=&q=" + this.keywordEncoded + "&ordenacao=6&limit=24";

    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".shelf-content-items .box-produto");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        Element internalIdElement = e.selectFirst(".text-center a");
        String internalId = internalIdElement.attr("data-codigoproduto");
        String productUrl = CrawlerUtils.scrapUrl(e, ".img-name a", "href", "https", "www.vilanova.com.br");

        saveDataProduct(null, internalId, productUrl);

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
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".qtd-produtos", true, 0);
    this.log("Total: " + this.totalProducts);
  }
}
