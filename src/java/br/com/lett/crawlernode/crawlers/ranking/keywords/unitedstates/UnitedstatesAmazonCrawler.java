package br.com.lett.crawlernode.crawlers.ranking.keywords.unitedstates;

import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.test.Test;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class UnitedstatesAmazonCrawler extends CrawlerRankingKeywords {

  public UnitedstatesAmazonCrawler(Session session) {
    super(session);
  }

  private static final String HOME_PAGE = "https://www.amazon.com/";
  private String nextPageUrl;

  @Override
  protected void processBeforeFetch() {
    Request request = RequestBuilder.create().setUrl(HOME_PAGE).setCookies(cookies).mustSendContentEncoding(false).build();
    this.cookies = CrawlerUtils.fetchCookiesFromAPage(request, ".amazon.com", "/", null, session, dataFetcher);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 20;
    this.log("Página " + this.currentPage);

    String url;

    if (this.currentPage == 1) {
      url = "https://www.amazon.com/s/ref=nb_sb_noss?url=search-alias%3Daps&page=" + this.currentPage + "&keywords=" + this.keywordEncoded
          + "&ie=UTF8&qid=1528919530";
    } else {
      url = this.nextPageUrl;
    }
    this.log("Link onde são feitos os crawlers: " + url);

    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.ACCEPT_ENCODING, "false");

    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).mustSendContentEncoding(false).setHeaders(headers).build();
    this.currentDoc = Jsoup.parse(this.dataFetcher.get(session, request).getBody());

    CommonMethods.saveDataToAFile(currentDoc, Test.pathWrite + "AMAZON.html");

    this.nextPageUrl = crawlNextPage();

    CommonMethods.saveDataToAFile(currentDoc, Test.pathWrite + "AMAZON.html");

    Elements products = this.currentDoc.select(".s-result-list .s-result-item");
    Element result = this.currentDoc.select("#noResultsTitle").first();

    if (!products.isEmpty() && result == null) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {

        String internalPid = crawlInternalPid(e);
        String internalId = internalPid;
        String productUrl = crawlProductUrl(internalPid);

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

  private String crawlNextPage() {
    String url = null;

    Element e = this.currentDoc.select(".pagnRA > a").first();

    if (e != null) {
      url = e.attr("href");

      if (url.startsWith("#")) {
        url = url.replaceFirst("#", "");
      }

      if (!url.contains("amazon.com")) {
        url = HOME_PAGE + "s" + url;
      }
    }

    return url;
  }

  @Override
  protected boolean hasNextPage() {
    return this.currentDoc.select("#pagnNextString").first() != null;
  }

  @Override
  protected void setTotalProducts() {
    JSONObject totalJson = CrawlerUtils.selectJsonFromHtml(currentDoc, "script[data-a-state=\"{\"key\":\"s-metadata\"}\"]", null, null, true);

    if (totalJson.has("totalResultCount")) {
      Object obj = totalJson.get("totalResultCount");

      if (obj instanceof Integer) {
        this.totalProducts = (Integer) obj;
        this.log("Total da busca: " + this.totalProducts);
      }
    }
  }

  private String crawlInternalPid(Element e) {
    return e.attr("data-asin");
  }

  private String crawlProductUrl(String id) {
    return HOME_PAGE + "dp/" + id;
  }

}
