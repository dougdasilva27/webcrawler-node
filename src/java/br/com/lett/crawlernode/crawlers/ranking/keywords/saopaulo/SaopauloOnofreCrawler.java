package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class SaopauloOnofreCrawler extends CrawlerRankingKeywords {

  public SaopauloOnofreCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 30;
    this.log("Página " + this.currentPage);

    String url = "https://www.onofre.com.br/search?N=0&No=" + this.arrayProducts.size() + "&Nrpp=30&Ntt=" + this.keywordEncoded;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".product-item > a.product-item--link");

    if (!products.isEmpty()) {
      for (Element e : products) {
        String internalPid = crawlInternalPid(e);
        String internalId = crawlInternalId(e);
        String urlProduct = crawlProductUrl(e);

        saveDataProduct(internalId, internalPid, urlProduct);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
        if (this.arrayProducts.size() == productsLimit)
          break;
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
    if (!(hasNextPage()))
      setTotalProducts();

  }

  @Override
  protected boolean hasNextPage() {
    return this.currentDoc.select(".showcase-pagination__last").first() != null;
  }

  private String crawlInternalId(Element e) {
    String internalId = null;
    Element id = e.select(".product-name").first();

    if (id != null) {
      internalId = id.attr("data-sku-code");
    }

    return internalId;
  }

  private String crawlInternalPid(Element e) {
    String internalPid = null;
    Element pid = e.select(".product-img img").first();

    if (pid != null) {
      String temp = CommonMethods.getLast(pid.attr("src").toLowerCase().split("/")).split("\\.")[0].trim();

      if (!temp.isEmpty()) {
        internalPid = temp;
      }
    }

    return internalPid;
  }

  private String crawlProductUrl(Element e) {
    String urlProduct = e.attr("href");

    if (!urlProduct.contains("onofre.com")) {
      urlProduct = "https://www.onofre.com.br" + urlProduct;
    }

    return urlProduct;
  }
}
