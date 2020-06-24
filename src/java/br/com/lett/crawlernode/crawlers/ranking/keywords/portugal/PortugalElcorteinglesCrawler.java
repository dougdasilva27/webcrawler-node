package br.com.lett.crawlernode.crawlers.ranking.keywords.portugal;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class PortugalElcorteinglesCrawler extends CrawlerRankingKeywords {

  public PortugalElcorteinglesCrawler(Session session) {
    super(session);
    this.pageSize = 24;
  }

  @Override
  protected void extractProductsFromCurrentPage() {

    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String url = "https://www.elcorteingles.pt/supermercado/pesquisar/" + currentPage + "/?term=" + keywordEncoded + "&search=text";
    this.currentDoc = fetchDocument(url);


    this.log("Link onde são feitos os crawlers: " + url);


    Elements products = this.currentDoc.select(".dataholder.js-product");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalId = e.attr("data-product-id");
        String productUrl = "https://www.elcorteingles.pt" + e.selectFirst("a").attr("href");

        saveDataProduct(internalId, null, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
      }
    }
  }

  @Override
  protected void setTotalProducts() {
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".grid-coincidences .semi", false, 0);
    this.log("Total da busca: " + this.totalProducts);
  }
}
