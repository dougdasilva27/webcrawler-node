package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import java.util.ArrayList;
import java.util.List;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class SaopauloDiaCrawler extends CrawlerRankingKeywords {

  public SaopauloDiaCrawler(Session session) {
    super(session);
  }

  private List<Cookie> cookies = new ArrayList<>();

  @Override
  public void processBeforeFetch() {
    this.log("Adding cookie...");

    BasicClientCookie cookie =
        new BasicClientCookie("dia_zv_zipcode", "04532040%3DRua%20Benedito%20Lapin%2C%20Itaim%20Bibi%20-%20S%C3%A3o%20Paulo%20%2F%20SP");
    cookie.setDomain(".dia.com.br");
    cookie.setPath("/");
    this.cookies.add(cookie);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 16;

    this.log("Página " + this.currentPage);

    String url = "https://delivery.dia.com.br/search/" + this.keywordEncoded + "?page=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    // chama função de pegar o html
    this.currentDoc = fetchDocument(url, cookies);

    Elements products = this.currentDoc.select(".shelf-content-itens li[data-product-id]");

    // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }
      for (Element e : products) {
        String internalPid = e.attr("data-product-id");
        String internalId = e.attr("data-sku-id");
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
    Element totalElement = this.currentDoc.select(".shelf-total-results-qty").first();

    if (totalElement != null) {
      String text = totalElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
      }
    }

    this.log("Total da busca: " + this.totalProducts);
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;
    Element urlElement = e.select(".box-product > a").first();

    if (urlElement != null) {
      productUrl = urlElement.attr("href");

      if (!productUrl.contains("delivery.dia")) {
        productUrl = ("https://delivery.dia.com.br/" + productUrl).replace(".br//", ".br/");
      }
    }

    return productUrl;
  }
}
