package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class SaopauloPontofrioCrawler extends CrawlerRankingKeywords {

  public SaopauloPontofrioCrawler(Session session) {
    super(session);
    super.fetchMode = FetchMode.APACHE;
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);

    String url = "https://search3.pontofrio.com.br/busca?q=" + this.keywordEncoded + "&page=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = Jsoup.parse(fetchPage(url));
    Elements products = this.currentDoc.select(".neemu-products-container li.nm-product-item");

    this.pageSize = 21;

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }
      for (Element e : products) {
        // InternalPid
        String internalPid = crawlInternalPid(e);

        // Url do produto
        String productUrl = crawlProductUrl(e);

        saveDataProduct(null, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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
    return !this.currentDoc.select(".neemu-pagination-next a").isEmpty();
  }

  private String crawlInternalPid(Element e) {
    return e.attr("data-productid");
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;

    Element url = e.select(".nm-product-name a").first();

    if (url != null) {
      productUrl = url.attr("href");

      if (!productUrl.startsWith("http")) {
        productUrl = "https:" + productUrl;
      }

      if (productUrl.contains("?")) {
        productUrl = productUrl.split("\\?")[0];
      }
    }

    return productUrl;
  }

  private String fetchPage(String url) {
    Map<String, String> headers = new HashMap<>();

    Request request = RequestBuilder.create()
        .setUrl(url)
        .setCookies(cookies)
        .setHeaders(headers)
        .setProxyservice(
            Arrays.asList(
                ProxyCollection.INFATICA_RESIDENTIAL_BR,
                ProxyCollection.BUY,
                ProxyCollection.STORM_RESIDENTIAL_US
            )
        ).build();

    return this.dataFetcher.get(session, request).getBody();
  }

  @Override
  protected void setTotalProducts() {
    Element totalElement = null;
    totalElement = this.currentDoc.select("span[data-totalresults]").first();

    if (totalElement != null) {
      String text = totalElement.text().replaceAll("[^0-9]", "");
      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(totalElement.text());
      }
    }

    this.log("Total da busca: " + this.totalProducts);
  }
}
