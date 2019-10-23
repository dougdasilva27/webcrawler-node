package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.HashMap;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilAbcdaconstrucaoCrawler extends CrawlerRankingKeywords {

  public BrasilAbcdaconstrucaoCrawler(Session session) {
    super(session);
  }

  protected Document fetch() {
    StringBuilder payload = new StringBuilder();
    payload.append("c=busca_produtos")
        .append("&busca=").append(this.keywordEncoded)
        .append("&limite=").append(this.currentPage)
        .append("&categorias=")
        .append("&marcas=")
        .append("&subCategorias=");

    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
    String urlToFetch = "https://www.abcdaconstrucao.com.br/ajax/busca.ajax.php";

    Request request = RequestBuilder.create()
        .setUrl(urlToFetch)
        .setCookies(cookies)
        .setHeaders(headers)
        .setPayload(payload.toString())
        .build();

    return Jsoup.parse(this.dataFetcher.post(session, request).getBody());
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 32;
    this.log("Página " + this.currentPage);

    this.currentDoc = fetch();
    Elements products = this.currentDoc.select(".product-card");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a.desejo[id]", "id");
        String productUrl = CrawlerUtils.scrapUrl(e, " > a", "href", "https:", "www.abcdaconstrucao.com.br");

        saveDataProduct(internalId, null, productUrl);

        this.log(
            "Position: " + this.position +
                " - InternalId: " + internalId +
                " - InternalPid: " + null +
                " - Url: " + productUrl
        );

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
    StringBuilder payload = new StringBuilder();
    payload.append("c=qtd")
        .append("&busca=").append(this.keywordEncoded);

    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
    String urlToFetch = "https://www.abcdaconstrucao.com.br/ajax/busca.ajax.php";

    Request request = RequestBuilder.create()
        .setUrl(urlToFetch)
        .setCookies(cookies)
        .setHeaders(headers)
        .setPayload(payload.toString())
        .build();

    Document doc = Jsoup.parse(this.dataFetcher.post(session, request).getBody());
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(doc, "body", true, 0);
    this.log("Total: " + this.totalProducts);
  }

}
