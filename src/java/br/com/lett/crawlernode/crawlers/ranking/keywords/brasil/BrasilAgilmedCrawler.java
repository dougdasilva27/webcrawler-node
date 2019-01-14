package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilAgilmedCrawler extends CrawlerRankingKeywords {

  public BrasilAgilmedCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 40;

    this.log("Página " + this.currentPage);

    String url = "https://www.agilmedrs.com.br/buscar?q=" + this.keywordEncoded + "&pagina="
        + this.currentPage;


    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select("#listagemProdutos ul li ul li");

    if (!products.isEmpty()) {

      for (Element e : products) {

        String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e,
            "div[data-trustvox-product-code]", "data-trustvox-product-code");

        String internalPid = CrawlerUtils.scrapStringSimpleInfo(e, ".produto-sku", false);


        String productUrl = CrawlerUtils.scrapUrl(e, "a[class=\"produto-sobrepor\"]", "href",
            "https:", "www.agilmedrs.com.br");

        saveDataProduct(internalId, null, productUrl);


        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: "
            + internalPid + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit)
          break;
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
        + this.arrayProducts.size() + " produtos crawleados");
  }

  @Override
  protected boolean hasNextPage() {
    Element nextButton = this.currentDoc.selectFirst(".pagination li:last-child");
    boolean result = false;

    if (nextButton != null) {
      if (!nextButton.hasClass("disabled")) {
        result = true;
      }
    }

    return result;

  }

}
