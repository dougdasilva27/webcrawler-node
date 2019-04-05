package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.test.Test;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class MexicoSorianasuperCrawler extends CrawlerRankingKeywords {

  public MexicoSorianasuperCrawler(Session session) {
    super(session);
  }

  private static final String DOMAIN = "superentucasa.soriana.com";

  @Override
  protected void processBeforeFetch() {
    Request request =
        RequestBuilder.create().setCookies(cookies).setUrl("http://superentucasa.soriana.com/default.aspx").setFollowRedirects(false).build();

    this.cookies = CrawlerUtils.fetchCookiesFromAPage(request, DOMAIN, "/", null, session, new FetcherDataFetcher());
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);

    String url = "http://" + DOMAIN + "/default.aspx?p=13365&postback=1&Txt_Bsq_Descripcion=" + this.keywordEncoded + "&cantCeldas=0&minCeldas=0";

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url, cookies);

    CommonMethods.saveDataToAFile(currentDoc, Test.pathWrite + "/SORIANA2.html");

    Elements products = this.currentDoc.select(".product-item");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {

        String internalId = crawlInternalId(e);
        String productUrl = CrawlerUtils.scrapUrl(e, "a[href]:first-child", "href", "http:", DOMAIN);

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
    return false;
  }

  private String crawlInternalId(Element e) {
    String internalId = null;
    Element id = e.selectFirst("input[type=hidden][name=s]");

    if (id != null) {
      internalId = id.val();
    }

    return internalId;
  }
}
