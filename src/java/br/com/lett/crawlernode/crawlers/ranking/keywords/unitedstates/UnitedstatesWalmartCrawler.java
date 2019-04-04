package br.com.lett.crawlernode.crawlers.ranking.keywords.unitedstates;

import java.util.ArrayList;
import java.util.List;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class UnitedstatesWalmartCrawler extends CrawlerRankingKeywords {

  public UnitedstatesWalmartCrawler(Session session) {
    super(session);
  }

  private List<Cookie> cookies = new ArrayList<>();

  @Override
  public void processBeforeFetch() {
    BasicClientCookie cookie = new BasicClientCookie("akgeo", "US");
    cookie.setDomain("www.walmart.com");
    cookie.setPath("/");
    this.cookies.add(cookie);

    BasicClientCookie cookie2 = new BasicClientCookie("usgmtbgeo", "US");
    cookie2.setDomain(".www.walmart.com");
    cookie2.setPath("/");
    this.cookies.add(cookie2);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 20;
    this.log("Página " + this.currentPage);

    String url = "https://www.walmart.com/search/?cat_id=0&page=" + this.currentPage + "&query=" + this.keywordEncoded;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url, cookies);

    JSONObject initialState = CrawlerUtils.selectJsonFromHtml(currentDoc, "script#searchContent", null, ";", true, true);
    JSONArray products = crawlProductsJson(initialState);

    if (products.length() > 0) {
      if (this.totalProducts == 0) {
        setTotalProducts(initialState);
      }

      for (Object o : products) {
        JSONObject item = (JSONObject) o;
        String internalPid = crawlInternalPid(item);
        String productUrl = crawlProductUrl(item);

        saveDataProduct(null, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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

  protected void setTotalProducts(JSONObject initialState) {

    if (initialState.has("searchContent")) {
      JSONObject searchContent = initialState.getJSONObject("searchContent");
      if (searchContent.has("preso")) {
        JSONObject preso = searchContent.getJSONObject("preso");
        if (preso.has("requestContext")) {
          JSONObject requestContext = preso.getJSONObject("requestContext");

          if (requestContext.has("itemCount")) {
            JSONObject itemCount = requestContext.getJSONObject("itemCount");

            if (itemCount.has("total") && itemCount.get("total") instanceof Integer) {
              this.totalProducts = itemCount.getInt("total");
            }
          }
        }
      }
    }

    this.log("Total da busca: " + this.totalProducts);
  }

  private String crawlProductUrl(JSONObject item) {
    String productUrl = null;

    if (item.has("productPageUrl")) {
      productUrl = CrawlerUtils.completeUrl(item.getString("productPageUrl"), "https", "www.walmart.com");
    }

    return productUrl;
  }

  private String crawlInternalPid(JSONObject item) {
    String internalPid = null;

    if (item.has("usItemId")) {
      internalPid = item.getString("usItemId");
    }

    return internalPid;
  }

  private JSONArray crawlProductsJson(JSONObject initialState) {
    JSONArray items = new JSONArray();

    if (initialState.has("searchContent")) {
      JSONObject searchContent = initialState.getJSONObject("searchContent");
      if (searchContent.has("preso")) {
        JSONObject preso = searchContent.getJSONObject("preso");

        if (preso.has("items")) {
          items = preso.getJSONArray("items");
        }
      }
    }

    return items;
  }

}
