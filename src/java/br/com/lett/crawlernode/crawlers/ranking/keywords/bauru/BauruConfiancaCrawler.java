package br.com.lett.crawlernode.crawlers.ranking.keywords.bauru;

import java.util.ArrayList;
import java.util.List;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BauruConfiancaCrawler extends CrawlerRankingKeywords {

  public BauruConfiancaCrawler(Session session) {
    super(session);
  }

  private List<Cookie> cookies = new ArrayList<>();

  @Override
  public void processBeforeFetch() {
    this.log("Adding cookie...");

    BasicClientCookie cookie = new BasicClientCookie("bauru", "lojabauru");
    cookie.setDomain("www.confianca.com.br");
    cookie.setPath("/");
    this.cookies.add(cookie);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 21;

    this.log("Página " + this.currentPage);

    String url = "https://www.confianca.com.br/catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordEncoded;
    this.log("Link onde são feitos os crawlers: " + url);

    // chama função de pegar o html
    this.currentDoc = fetchDocument(url, cookies);

    Elements products = this.currentDoc.select(".products-grid li.item > a");

    // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
    if (!products.isEmpty()) {
      for (Element e : products) {
        String internalId = crawlInternalId(e);
        String productUrl = e.attr("href");

        saveDataProduct(internalId, null, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
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
  protected boolean hasNextPage() {
    return !this.currentDoc.select("a.next").isEmpty();
  }

  private String crawlInternalId(Element e) {
    String internalId = null;
    Element internalIdElement = e.select("img").first();

    if (internalIdElement != null) {
      String text = internalIdElement.attr("id");

      if (text.contains("-")) {
        internalId = CommonMethods.getLast(text.split("-"));
      }
    }

    return internalId;
  }
}
