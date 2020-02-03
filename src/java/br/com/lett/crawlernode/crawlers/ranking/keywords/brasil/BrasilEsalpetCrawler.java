package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

public class BrasilEsalpetCrawler extends CrawlerRankingKeywords {
  
  private static final String HOME_PAGE = "www.esalpet.com.br";
  private static String searchUrl = "";

  public BrasilEsalpetCrawler(Session session) {
    super(session);
  }
  
  private void fetchSeachUrl() {
    
    String url = "https://www.esalpet.com.br/cart/search";
    
    Map<String, String> headers = new HashMap<>();
    headers.put("content-type", "application/x-www-form-urlencoded");
    
    String payload = "term=" + this.keywordEncoded;
    
    Request request = RequestBuilder.create()
        .setUrl(url)
        .setHeaders(headers)
        .setCookies(cookies)
        .setPayload(payload)
        .build();
    
    
    String redirectUrl = this.dataFetcher.post(session, request).getRedirectUrl();
    if(redirectUrl != null && redirectUrl.endsWith("/0")) {
      searchUrl = redirectUrl.substring(0, redirectUrl.length() - "/0".length());
    }
  }
  
  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 24;
    this.log("Página " + this.currentPage);
    
    fetchSeachUrl();

    String url = searchUrl + "/" + ((this.currentPage-1) * 24);
    
    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    
    JSONObject idsJson = CrawlerUtils.selectJsonFromHtml(this.currentDoc, "script", "fbq('track', 'Search', ", ");", false, true);
    JSONArray ids = new JSONArray();
    Elements products = this.currentDoc.select(".prod-list .prod-wrapper");
    
    if(idsJson.has("content_ids") && idsJson.get("content_ids") instanceof JSONArray) {
      ids = idsJson.getJSONArray("content_ids");
    }

    if (!products.isEmpty() && ids.length() == products.size()) {
      for(int i = 0; i < products.size(); i++) {
        Element e = products.get(i);

        String internalId = ids.getString(i);
        String productUrl = CrawlerUtils.scrapUrl(e, "> a[href]", "href", "https", HOME_PAGE);

        saveDataProduct(internalId, null, productUrl);

        this.log(
            "Position: " + this.position + 
            " - InternalId: " + internalId +
            " - InternalPid: " + null + 
            " - Url: " + productUrl);
        
        if (this.arrayProducts.size() == productsLimit)
          break;
      }
      
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
        + this.arrayProducts.size() + " produtos crawleados");
  }
  
  @Override
  protected boolean hasNextPage() {
    Element pagination = this.currentDoc.selectFirst(".pagination > li:last-child");
    return pagination != null && !pagination.hasClass("active");
  }
}
