package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;

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

    String url = "https://www.terabyteshop.com.br/busca?app=true&url=%2Fbusca&filter%5Bstr%5D=" + this.keywordWithoutAccents.replace(" ", "+")
        + "&filter%5Bppg%5D=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    Request request = RequestBuilder.create().setCookies(cookies).setUrl(url).build();
    Response response = dataFetcher.get(session, request);

    // A resposta vem com um caractere ascii que não aparece em arquivo, por isso é usado essa expressão
    // para remover.
    String resultString = response.getBody().replaceAll("[^\\x00-\\x7F]", "");
    JSONObject bodyJson = new JSONObject(resultString);
    doc = Jsoup.parse(JSONUtils.getStringValue(bodyJson, "body"));

    return doc;
  }
}
