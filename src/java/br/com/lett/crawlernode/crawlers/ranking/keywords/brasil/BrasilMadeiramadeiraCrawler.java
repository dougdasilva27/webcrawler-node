package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilMadeiramadeiraCrawler extends CrawlerRankingKeywords {

  public BrasilMadeiramadeiraCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 48;

    this.log("Página " + this.currentPage);
    String url = "https://www.madeiramadeira.com.br/busca?q=" + this.keywordEncoded + "&sortby=relevance&resultsperpage=48&page=" + this.currentPage;

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".product-box__item");

    if (!products.isEmpty()) {

      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalPid = scrapInternalPid(e);
        String productUrl = scrapUrl(e);

        saveDataProduct(null, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: "
            + internalPid + " - Url: " + productUrl);

        if (this.arrayProducts.size() == productsLimit) {
          break;
        }
      }

    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
        + this.arrayProducts.size() + " produtos crawleados");
  }

  private String scrapUrl(Element e) {
    Element urlElement = e.selectFirst(".product__name a");
    String url = null;


    if (urlElement != null) {
      url = CrawlerUtils.completeUrl(urlElement.attr("href"), "https", "www.madeiramadeira.com.br");
    }

    return url;
  }

  private String scrapInternalPid(Element e) {
    return e.attr("data-listing-product-id");
  }

  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.selectFirst(
        "#wrapper > div.container.seller__products-box > div > div.col-xs-10 > div.row.col-xs-12.margin-bottom-10 > div:nth-child(3) > div.col-xs-2.no-padding--right.margin-top-10 > span");

    if (totalElement != null) {
      try {
        this.totalProducts = Integer.parseInt(totalElement.text().replaceAll("[^0-9]", "").trim());
      } catch (Exception e) {
        this.logError(e.getMessage());
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }
}
