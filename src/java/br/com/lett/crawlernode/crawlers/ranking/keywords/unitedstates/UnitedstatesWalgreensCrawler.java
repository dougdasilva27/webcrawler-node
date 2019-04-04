package br.com.lett.crawlernode.crawlers.ranking.keywords.unitedstates;

import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class UnitedstatesWalgreensCrawler extends CrawlerRankingKeywords {

  public UnitedstatesWalgreensCrawler(Session session) {
    super(session);
  }

  private static final String HOME_PAGE = "https://www.walgreens.com/";

  @Override
  public void processBeforeFetch() {
    this.cookies = CrawlerUtils.fetchCookiesFromAPage(HOME_PAGE, null, ".walgreens.com", "/", cookies, session, new HashMap<>(), dataFetcher);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 24;
    this.log("Página " + this.currentPage);

    String url = "https://www.walgreens.com/search/results.jsp?Ntt=" + this.keywordEncoded + "&No=" + this.arrayProducts.size();
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url, cookies);

    JSONObject initialState = CrawlerUtils.selectJsonFromHtml(currentDoc, "script", "window.__APP_INITIAL_STATE__=", "};", true, true);
    JSONArray products = crawlProductsJson(initialState);

    if (products.length() > 0) {
      if (this.totalProducts == 0) {
        setTotalProducts(initialState);
      }

      for (Object o : products) {
        JSONObject item = (JSONObject) o;
        if (item.has("productInfo")) {
          JSONObject productInfo = item.getJSONObject("productInfo");

          String internalId = productInfo.has("skuId") ? productInfo.get("skuId").toString() : null;
          String internalPid = productInfo.has("prodId") ? productInfo.get("prodId").toString() : null;
          String productUrl =
              productInfo.has("productURL") ? CrawlerUtils.completeUrl(productInfo.get("productURL").toString(), "https", "www.walgreens.com") : null;

          saveDataProduct(internalId, internalPid, productUrl);

          this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
          if (this.arrayProducts.size() == productsLimit) {
            break;
          }
        }
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }

  protected void setTotalProducts(JSONObject initialState) {

    if (initialState.has("searchResult")) {
      JSONObject searchResult = initialState.getJSONObject("searchResult");

      if (searchResult.has("summary")) {
        JSONObject summary = searchResult.getJSONObject("summary");
        this.totalProducts = CrawlerUtils.getIntegerValueFromJSON(summary, "productInfoCount", 0);
      }
    }

    this.log("Total da busca: " + this.totalProducts);
  }

  private JSONArray crawlProductsJson(JSONObject initialState) {
    JSONArray items = new JSONArray();

    if (initialState.has("searchResult")) {
      JSONObject preso = initialState.getJSONObject("searchResult");

      if (preso.has("productList")) {
        items = preso.getJSONArray("productList");
      }
    }

    return items;
  }

}
