package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class RiodejaneiroZonasulCrawler extends CrawlerRankingKeywords {

  public RiodejaneiroZonasulCrawler(Session session) {
    super(session);
    super.fetchMode = FetchMode.FETCHER;
  }

  @Override
  protected void processBeforeFetch() {
    this.cookies = CrawlerUtils.fetchCookiesFromAPage("https://www.zonasul.com.br/", Arrays.asList("ASP.NET_SessionId"), "www.zonasul.com.br", "/",
        null, session, dataFetcher);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 40;
    this.log("Página " + this.currentPage);

    this.currentDoc = fetchSearchPage();
    Elements products = this.currentDoc.select(".item_vitrine[data-id]");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalId = e.attr("data-id");
        String productUrl = crawlProductUrl(e);

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
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.selectFirst(".result");

    if (totalElement != null) {
      String text = totalElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
        this.log("Total da busca: " + this.totalProducts);
      }
    }
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;

    Element link = e.selectFirst("> a");
    if (link != null) {
      productUrl = CrawlerUtils.sanitizeUrl(link, "href", "https:", "www.zonasul.com.br");
    }

    return productUrl;
  }

  private Document fetchSearchPage() {
    Document doc;

    String url = "https://www.zonasul.com.br/busca/" + this.keywordWithoutAccents.replace(" ", "%20");

    this.log("Link onde são feitos os crawlers: " + url);

    if (this.currentPage == 1) {
      doc = fetchDocument(url);
    } else {
      String postUrl = "https://www.zonasul.com.br/Global/VitrinePaginada";
      StringBuilder payload = new StringBuilder().append("codSubSecao=").append("&filtroNomeComposicao=").append("&numeroPagina=")
          .append(this.currentPage).append("&ordenacao=1").append("&pagina=6").append("&subFiltros=");

      Map<String, String> headers = new HashMap<>();
      headers.put("referer", url);
      headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

      Request request = RequestBuilder.create().setUrl(postUrl).setCookies(cookies).setHeaders(headers).setPayload(payload.toString()).build();
      doc = Jsoup.parse(this.dataFetcher.post(session, request).getBody());
    }

    return doc;
  }
}
