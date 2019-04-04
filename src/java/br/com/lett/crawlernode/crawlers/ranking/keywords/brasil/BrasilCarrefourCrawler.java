package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.HashMap;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilCarrefourCrawler extends CrawlerRankingKeywords {

  public BrasilCarrefourCrawler(Session session) {
    super(session);
    super.fetchMode = FetchMode.APACHE;
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 20;

    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String url = "https://www.carrefour.com.br/busca/?termo=" + this.keywordEncoded + "&foodzipzone=na&page=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    // chama função de pegar o html
    this.currentDoc = Jsoup.parse(fetchPage(url));

    Elements products = this.currentDoc.select("li.product .prd-info a[title]");

    // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
    if (!products.isEmpty()) {
      // se o total de busca não foi setado ainda, chama a função para setar
      if (this.totalProducts == 0) {
        setTotalProductsCarrefour();
      }

      for (Element e : products) {

        String productUrl = crawlProductUrl(e);
        String internalPid = crawlInternalPid(e);
        String internalId = crawlInternalId(productUrl);

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
  protected boolean hasNextPage() {
    Element finalPage = this.currentDoc.select("#loadNextPage").first();

    // if href on id loadNextPage is nor Empty there are more pages.
    if (finalPage != null) {
      String href = finalPage.attr("href").trim();

      if (!href.isEmpty()) {
        return true;
      }
    }

    return false;
  }

  protected void setTotalProductsCarrefour() {
    Element totalElement = this.currentDoc.select(".result-count strong span").first();

    if (totalElement != null) {
      try {
        this.totalProducts = Integer.parseInt(totalElement.text().replaceAll("[^0-9]", "").trim());
      } catch (Exception e) {
        this.logError(CommonMethods.getStackTrace(e));
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String crawlInternalPid(Element e) {
    String internalId = null;

    Element id = e.select("input[name=productCodePost]").first();

    if (id != null) {
      internalId = id.val();
    }

    return internalId;
  }

  private String crawlInternalId(String url) {
    String internalPid = null;

    if (url.contains("?")) {
      url = url.split("?")[0];
    }

    if (url.contains("/p/")) {
      String[] tokens = url.split("/p/");

      if (tokens.length > 1 && tokens[1].contains("/")) {
        internalPid = tokens[1].split("/")[0];
      } else if (tokens.length > 1) {
        internalPid = tokens[1];
      }
    }

    return internalPid;
  }

  private String crawlProductUrl(Element e) {
    return CrawlerUtils.sanitizeUrl(e, "href", "https:", "www.carrefour.com.br");
  }


  private String fetchPage(String url) {
    Map<String, String> headers = new HashMap<>();
    headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
    headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
    headers.put("upgrade-insecure-requests", "1");

    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).build();
    String response = this.dataFetcher.get(session, request).getBody();

    if (response == null || response.isEmpty()) {
      response = new FetcherDataFetcher().get(session, request).getBody();
    }

    return response;
  }
}
