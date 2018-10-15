package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilPichauCrawler extends CrawlerRankingKeywords {

  public BrasilPichauCrawler(Session session) {
    super(session);
  }

  private String categoryUrl;

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 16;
    this.log("Página " + this.currentPage);

    String url = "https://www.pichau.com.br/catalogsearch/result/index/?limit=48&p=" + this.currentPage + "&q=" + this.keywordEncoded;

    if (this.currentPage > 1 && this.categoryUrl != null) {
      url = this.categoryUrl + "?p=" + this.currentPage;
    }

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".linha-produtos .item");

    if (this.currentPage == 1) {
      String redirectUrl = CrawlerUtils.crawlFinalUrl(url, session);

      if (!url.equals(redirectUrl)) {
        this.categoryUrl = redirectUrl;
      }
    }

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalId = crawlInternalId(e);
        String productUrl = crawlProductUrl(e);

        saveDataProduct(internalId, null, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
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
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.select(".produtos header p").first();

    if (totalElement != null) {
      String text = totalElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
        this.log("Total da busca: " + this.totalProducts);
      }
    }
  }

  private String crawlInternalId(Element e) {
    String internalId = null;

    Element price = e.selectFirst(".price");
    if (price != null) {
      String text = price.attr("id");

      if (text.contains("-")) {
        internalId = CommonMethods.getLast(text.split("-"));
      }
    } else {
      Element meAvise = e.selectFirst("[data-produto]");

      if (meAvise != null) {
        internalId = meAvise.attr("data-produto");
      }
    }

    return internalId;
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;

    Element link = e.selectFirst(".title a");
    if (link != null) {
      productUrl = CrawlerUtils.sanitizeUrl(link, "href", "https:", "www.pichau.com.br");
    }

    return productUrl;
  }
}
