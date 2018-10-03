package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilAmazonCrawler extends CrawlerRankingKeywords {

  public BrasilAmazonCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 20;
    this.log("Página " + this.currentPage);

    String url =
        "https://www.amazon.com.br/s/ref=sr_pg_" + this.currentPage + "?page=" + this.currentPage + "&keywords=" + this.keywordEncoded + "&ie=UTF8";
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".s-result-list .s-result-item");
    Element result = this.currentDoc.select("#noResultsTitle").first();

    if (!products.isEmpty() && result == null) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {

        String internalPid = crawlInternalPid(e);
        String internalId = internalPid;
        String productUrl = crawlProductUrl(e);

        saveDataProduct(internalId, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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
    JSONObject totalJson = CrawlerUtils.selectJsonFromHtml(currentDoc, "script[data-a-state=\"{\"key\":\"s-metadata\"}\"]", null, null, true);

    if (totalJson.has("totalResultCount")) {
      Object obj = totalJson.get("totalResultCount");

      if (obj instanceof Integer) {
        this.totalProducts = (Integer) obj;
        this.log("Total da busca: " + this.totalProducts);
      }
    }
  }

  private String crawlInternalPid(Element e) {
    return e.attr("data-asin");
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;

    Element url = e.select(".a-link-normal").first();

    if (url != null) {
      productUrl = url.attr("href").split("\\?")[0];

      if (!productUrl.contains("amazon.com.br")) {
        productUrl = "https://www.amazon.com.br" + productUrl;
      }
    }

    return productUrl;
  }

}
