package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilTerabyteCrawler extends CrawlerRankingKeywords {

  public BrasilTerabyteCrawler(Session session) {
    super(session);
  }

  private static final String PRODUCTS_SELECTOR = ".pbox .text-center a";

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 64;
    this.log("Página " + this.currentPage);

    this.currentDoc = fetchSearchPage();
    Elements products = this.currentDoc.select(PRODUCTS_SELECTOR);

    if (!products.isEmpty()) {
      for (Element e : products) {
        String productUrl = CrawlerUtils.sanitizeUrl(e, "href", "https:", "www.terabyteshop.com.br");
        String internalId = crawlInternalId(productUrl);

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
  protected boolean hasNextPage() {
    return this.currentDoc.select(PRODUCTS_SELECTOR).size() >= this.pageSize;
  }

  private String crawlInternalId(String url) {
    String internalId = null;

    if (url.contains("produto/")) {
      String text = CommonMethods.getLast(url.split("produto/")).split("/")[0].trim();

      if (!text.isEmpty()) {
        internalId = text;
      }
    }

    return internalId;
  }

  private Document fetchSearchPage() {
    Document doc = new Document("");

    String url = "https://www.terabyteshop.com.br/busca?str=" + this.keywordWithoutAccents.replace(" ", "%20") + "&ppg=" + this.currentPage;

    this.log("Link onde são feitos os crawlers: " + url);

    if (this.currentPage == 1) {
      doc = fetchDocument(url);
    } else {
      JSONObject json = fetchJSONObject(url);

      if (json.has("src")) {
        doc = Jsoup.parse(json.get("src").toString());
      }
    }

    return doc;
  }
}
