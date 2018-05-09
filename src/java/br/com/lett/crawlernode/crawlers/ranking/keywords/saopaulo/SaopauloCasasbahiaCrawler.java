package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import java.util.HashMap;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.GETFetcher;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class SaopauloCasasbahiaCrawler extends CrawlerRankingKeywords {

  public SaopauloCasasbahiaCrawler(Session session) {
    super(session);
  }

  private static final String HOME_PAGE = "https://www.casasbahia.com.br/";

  private boolean isCategory;
  private String urlCategory;

  @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página

    String url = "https://buscas.casasbahia.com.br/?strBusca=" + this.keywordEncoded + "&paginaAtual=" + this.currentPage;

    if (this.currentPage > 1 && isCategory) {
      url = this.urlCategory + "&paginaAtual=" + this.currentPage;
    }

    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = Jsoup.parse(fetchPage(url));
    Elements products = this.currentDoc.select("a.link.url");

    if (this.currentPage == 1) {
      String redirectUrl = this.session.getRedirectedToURL(url);
      if (redirectUrl != null && !redirectUrl.equals(url)) {
        isCategory = true;
        this.urlCategory = redirectUrl;
      } else {
        isCategory = false;
      }
    }

    // número de produtos por página do market
    if (!isCategory) {
      this.pageSize = 21;
    }

    Elements result = this.currentDoc.select(".naoEncontrado, #divBuscaVaziaSuperior");

    if (!products.isEmpty() && result.isEmpty()) {
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
    return !this.currentDoc.select("li.next a").isEmpty();
  }

  private String crawlInternalPid(Element e) {
    return e.attr("data-id");
  }

  private String crawlProductUrl(Element e) {
    String productUrl = e.attr("href");

    if (productUrl.contains("?")) {
      productUrl = productUrl.split("\\?")[0];
    }

    return productUrl;
  }

  private String fetchPage(String url) {
    Map<String, String> headers = new HashMap<>();
    headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
    headers.put("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
    headers.put("Cache-Control", "no-cache");
    headers.put("Connection", "keep-alive");
    headers.put("Host", "www.casasbahia.com.br");
    headers.put("Referer", HOME_PAGE);
    headers.put("Upgrade-Insecure-Requests", "1");
    headers.put("User-Agent", DataFetcher.randUserAgent());

    return GETFetcher.fetchPageGETWithHeaders(session, url, null, headers, 1);
  }

  @Override
  protected void setTotalProducts() {
    Element totalElement = null;
    if (!isCategory) {
      totalElement = this.currentDoc.select(".resultado .resultado strong").first();

      if (totalElement != null) {
        String text = totalElement.text().replaceAll("[^0-9]", "");
        if (!text.isEmpty()) {
          this.totalProducts = Integer.parseInt(totalElement.text());
        }
      }
      this.log("Total da busca: " + this.totalProducts);
    } else if (this.arrayProducts.size() < 100 && !hasNextPage()) {
      this.totalProducts = this.arrayProducts.size();
    }
  }
}
