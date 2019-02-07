package br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils;

import java.util.Arrays;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class FalabellaCrawler extends CrawlerRankingKeywords {

  private String HOME_PAGE = "https://www.falabella.com/falabella-cl/";

  public FalabellaCrawler(Session session) {
    super(session);
  }

  protected void setHomePage(String homePage) {
    this.HOME_PAGE = homePage;
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 32;

    this.log("Página " + this.currentPage);

    String url = HOME_PAGE + "search/?Ntt=" + this.keywordEncoded + "&page=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".fb-pod-group__item--product > [data-sku-id]");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }
      for (Element e : products) {
        String internalId = e.attr("data-sku-id");
        String productUrl = crawlProductUrl(e);

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
  protected void setTotalProducts() {
    JSONObject json = CrawlerUtils.selectJsonFromHtml(this.currentDoc, "script", "var fbra_browseProductListConfig =", ";", false, false);
    if (json.has("state")) {
      JSONObject state = json.getJSONObject("state");

      if (state.has("searchItemList")) {
        JSONObject searchItemList = state.getJSONObject("searchItemList");

        if (searchItemList.has("resultsTotal") && searchItemList.get("resultsTotal") instanceof Integer) {
          this.totalProducts = searchItemList.getInt("resultsTotal");
        }
      }
    }

    this.log("Total da busca: " + this.totalProducts);
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;

    Element url = e.selectFirst(".fb-pod__header-link");
    if (url != null) {
      productUrl = CrawlerUtils.sanitizeUrl(url, Arrays.asList("href"), "https:", "www.falabella.com");
    }
    return productUrl;
  }

}
