package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.HashMap;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilNanacareCrawler extends CrawlerRankingKeywords {

  public BrasilNanacareCrawler(Session session) {
    super(session);
  }

  private String nextUrl = null;

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 24;

    this.log("Página " + this.currentPage);

    if (this.currentPage == 1) {
      String url = "https://www.nanacare.com.br/cart/search/";

      String payload = "term=" + this.keywordEncoded;

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/x-www-form-urlencoded");
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).setPayload(payload).build();
      this.currentDoc = Jsoup.parse(this.dataFetcher.post(session, request).getBody());

      this.log("Link onde são feitos os crawlers: " + session.getRedirectedToURL(url));
      takeAScreenshot(session.getRedirectedToURL(url), 1, null);

    } else {
      this.currentDoc = fetchDocument(this.nextUrl);
      this.log("Link onde são feitos os crawlers: " + this.nextUrl);
    }

    this.nextUrl = crawlNextUrl();

    Elements products = this.currentDoc.select(".product-list-item");

    // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
    if (products.size() >= 1) {
      for (Element e : products) {
        // InternalPid
        String internalPid = null;

        // InternalId
        String internalId = crawlInternalId(e);

        // Url do produto
        String productUrl = crawlProductUrl(e);

        saveDataProduct(internalId, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit) {
          break;
        }

      }
    } else {
      setTotalProducts();
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    if (!hasNextPage()) {
      setTotalProducts();
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }

  @Override
  protected boolean hasNextPage() {
    if (this.nextUrl != null) {
      return true;
    }

    return false;
  }

  private String crawlInternalId(Element e) {
    String internalId = null;
    Element idElement = e.select("span[id^=product-price-]").first();

    if (idElement != null) {
      String[] tokens = idElement.attr("id").split("-");
      String id = tokens[tokens.length - 1].replaceAll("[^0-9]", "").trim();

      if (!id.isEmpty()) {
        internalId = id;
      }
    }

    return internalId;
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;
    Element url = e.select(".prod-info > a").first();

    if (url != null) {
      productUrl = url.attr("href");

      if (!productUrl.startsWith("https://www.nanacare.com.br/")) {
        productUrl = "https://www.nanacare.com.br/" + productUrl;
      }
    }

    return productUrl;
  }

  private String crawlNextUrl() {
    String url = null;
    Elements pagination = this.currentDoc.select(".pagination li a");

    for (Element e : pagination) {
      String text = e.ownText().trim();

      if ("›".equals(text)) {
        url = e.attr("href").trim() != "#" ? e.attr("href").trim() : null;
        break;
      }
    }

    return url;
  }
}
